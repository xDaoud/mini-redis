package dev.xDaoud.miniredis.resp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RespWriterTest {

    private static String serialize(RespValue value) throws IOException {
        var bytes = new ByteArrayOutputStream();
        var writer = new RespWriter(bytes);
        writer.write(value);
        writer.flush();
        return bytes.toString(StandardCharsets.UTF_8);
    }

    @Test void writesSimpleString() throws Exception {
        assertEquals("+PONG\r\n", serialize(new RespValue.SimpleString("PONG")));
    }

    @Test void writesNullBulkString() throws Exception {
        assertEquals("$-1\r\n", serialize(RespValue.BulkString.NULL));
    }

    @Test void writesArray() throws Exception {
        assertEquals("*2\r\n$3\r\nfoo\r\n:7\r\n",
                serialize(RespValue.RespArray.of(RespValue.BulkString.of("foo"),
                                                 new RespValue.RespInteger(7))));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "+OK\r\n", "-ERR bad\r\n", ":99\r\n", "$5\r\nhello\r\n",
            "$0\r\n\r\n", "$-1\r\n", "*-1\r\n", "*0\r\n",
            "*2\r\n$1\r\na\r\n:1\r\n", "*1\r\n*1\r\n+nested\r\n"
    })
    void roundTrips(String wire) throws Exception {
        RespValue parsed = new RespReader(
                new ByteArrayInputStream(wire.getBytes(StandardCharsets.UTF_8))).read();
        assertEquals(wire, serialize(parsed));
    }
}