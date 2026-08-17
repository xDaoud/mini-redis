package dev.xDaoud.miniredis.resp;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class RespReader {

    private static final long MAX_BULK_LENGTH  = 512L * 1024 * 1024;  // matches Redis
    private static final long MAX_ARRAY_LENGTH = 1024 * 1024;

    private final InputStream in;

    public RespReader(InputStream in) {
        // Byte-oriented buffering: cheap single-byte reads, no charset decoding.
        this.in = in instanceof BufferedInputStream b ? b : new BufferedInputStream(in);
    }

    /**
     * Reads the next complete RESP value.
     *
     * @return the value, or null if the stream ended cleanly between values
     */
    public RespValue read() throws IOException {
        int type = in.read();
        if (type == -1) {
            return null;                       // clean EOF: the client hung up
        }
        return switch (type) {
            case '+' -> new RespValue.SimpleString(readLine());
            case '-' -> new RespValue.RespError(readLine());
            case ':' -> new RespValue.RespInteger(readLong());
            case '$' -> readBulkString();
            case '*' -> readArray();
            default  -> throw new RespProtocolException(
                    "unexpected type byte 0x%02x ('%s')".formatted(type, (char) type));
        };
    }

    /** Reads up to and including CRLF; returns everything before the CR. */
    private String readLine() throws IOException {
        var buffer = new ByteArrayOutputStream(64);
        while (true) {
            int b = in.read();
            if (b == -1) throw new EOFException("stream ended mid-line");
            if (b == '\r') {
                int next = in.read();
                if (next == -1)   throw new EOFException("stream ended after CR");
                if (next != '\n') throw new RespProtocolException("CR not followed by LF");
                return buffer.toString(StandardCharsets.UTF_8);
            }
            if (b == '\n') throw new RespProtocolException("bare LF without preceding CR");
            buffer.write(b);
        }
    }

    private long readLong() throws IOException {
        String line = readLine();
        try {
            return Long.parseLong(line);
        } catch (NumberFormatException e) {
            throw new RespProtocolException("expected an integer, got '" + line + "'");
        }
    }

    private RespValue readBulkString() throws IOException {
        long length = readLong();
        if (length == -1) return RespValue.BulkString.NULL;
        if (length < 0 || length > MAX_BULK_LENGTH) {
            throw new RespProtocolException("invalid bulk length: " + length);
        }
        // readNBytes loops until it has the full count; a bare read() may come up short.
        byte[] payload = in.readNBytes((int) length);
        if (payload.length != length) throw new EOFException("stream ended mid bulk string");
        expectCrlf();
        return new RespValue.BulkString(payload);
    }

    private RespValue readArray() throws IOException {
        long count = readLong();
        if (count == -1) return RespValue.RespArray.NULL;
        if (count < 0 || count > MAX_ARRAY_LENGTH) {
            throw new RespProtocolException("invalid array length: " + count);
        }
        List<RespValue> elements = new ArrayList<>((int) count);
        for (long i = 0; i < count; i++) {
            RespValue element = read();                       // <-- the recursion
            if (element == null) throw new EOFException("stream ended mid array");
            elements.add(element);
        }
        return new RespValue.RespArray(List.copyOf(elements));
    }

    private void expectCrlf() throws IOException {
        int cr = in.read();
        int lf = in.read();
        if (cr == -1 || lf == -1)     throw new EOFException("stream ended before terminator");
        if (cr != '\r' || lf != '\n') throw new RespProtocolException("expected CRLF terminator");
    }
}