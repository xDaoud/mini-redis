package dev.xDaoud.miniredis.resp;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RespReaderTest {

    private static RespValue parse(String wire) throws IOException {
        return new RespReader(
                new ByteArrayInputStream(wire.getBytes(StandardCharsets.UTF_8))).read();
    }

    @Test void simpleString() throws Exception {
        assertEquals(new RespValue.SimpleString("OK"), parse("+OK\r\n"));
    }

    @Test void error() throws Exception {
        assertEquals(new RespValue.RespError("ERR nope"), parse("-ERR nope\r\n"));
    }

    @Test void integer() throws Exception {
        assertEquals(new RespValue.RespInteger(-42), parse(":-42\r\n"));
    }

    @Test void bulkString() throws Exception {
        assertEquals(RespValue.BulkString.of("hello"), parse("$5\r\nhello\r\n"));
    }

    @Test void emptyBulkString() throws Exception {
        assertEquals(RespValue.BulkString.of(""), parse("$0\r\n\r\n"));
    }

    @Test void nullBulkString() throws Exception {
        assertEquals(RespValue.BulkString.NULL, parse("$-1\r\n"));
    }

    @Test void bulkStringMayContainCrlf() throws Exception {
        // The point of length-prefixing: the payload is never scanned for a terminator.
        RespValue value = parse("$5\r\na\r\nb!\r\n");
        assertEquals("a\r\nb!", ((RespValue.BulkString) value).asString());
    }

    @Test void nestedArray() throws Exception {
        assertEquals(
                new RespValue.RespArray(List.of(
                        new RespValue.RespArray(List.of(new RespValue.RespInteger(1))),
                        new RespValue.SimpleString("OK"))),
                parse("*2\r\n*1\r\n:1\r\n+OK\r\n"));
    }

    @Test void pingAsSentByRedisCli() throws Exception {
        assertEquals(RespValue.RespArray.of(RespValue.BulkString.of("PING")),
                     parse("*1\r\n$4\r\nPING\r\n"));
    }

    @Test void cleanEofReturnsNull() throws Exception {
        assertNull(parse(""));
    }

    @Test void truncatedBulkStringThrows() {
        assertThrows(EOFException.class, () -> parse("$10\r\nshort"));
    }

    @Test void bareNewlineIsRejected() {
        assertThrows(RespProtocolException.class, () -> parse("+OK\n"));
    }

    @Test void unknownTypeByteIsRejected() {
        assertThrows(RespProtocolException.class, () -> parse("!nope\r\n"));
    }

    @Test void survivesOneByteAtATime() throws Exception {
        // Proves the parser is immune to TCP segmentation.
        byte[] wire = "*2\r\n$3\r\nfoo\r\n$3\r\nbar\r\n".getBytes(StandardCharsets.UTF_8);
        var reader = new RespReader(new DripInputStream(wire));
        assertEquals(RespValue.RespArray.of(RespValue.BulkString.of("foo"),
                                            RespValue.BulkString.of("bar")),
                     reader.read());
    }

    /** An InputStream that never returns more than one byte per bulk read. */
    private static final class DripInputStream extends FilterInputStream {
        DripInputStream(byte[] data) { super(new ByteArrayInputStream(data)); }

        @Override public int read(byte[] b, int off, int len) throws IOException {
            return super.read(b, off, Math.min(len, 1));
        }
    }
}