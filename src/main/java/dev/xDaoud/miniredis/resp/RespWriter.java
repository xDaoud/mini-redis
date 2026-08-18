package dev.xDaoud.miniredis.resp;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class RespWriter {

    private static final byte[] CRLF = { '\r', '\n' };

    private final OutputStream out;

    public RespWriter(OutputStream out) {
        this.out = out instanceof BufferedOutputStream b ? b : new BufferedOutputStream(out);
    }

    public void write(RespValue value) throws IOException {
        switch (value) {
            case RespValue.SimpleString(String s) -> writeLine('+', s);
            case RespValue.RespError(String m)    -> writeLine('-', m);
            case RespValue.RespInteger(long n)    -> writeLine(':', Long.toString(n));

            case RespValue.BulkString(byte[] payload) -> {
                if (payload == null) {
                    writeLine('$', "-1");
                } else {
                    writeLine('$', Integer.toString(payload.length));
                    out.write(payload);
                    out.write(CRLF);
                }
            }

            case RespValue.RespArray(List<RespValue> elements) -> {
                if (elements == null) {
                    writeLine('*', "-1");
                } else {
                    writeLine('*', Integer.toString(elements.size()));
                    for (RespValue element : elements) {
                        write(element);
                    }
                }
            }
            // No default. RespValue is sealed, so this switch is provably exhaustive.
        }
    }

    public void flush() throws IOException {
        out.flush();
    }

    private void writeLine(char prefix, String payload) throws IOException {
        out.write(prefix);
        out.write(payload.getBytes(StandardCharsets.UTF_8));
        out.write(CRLF);
    }
}