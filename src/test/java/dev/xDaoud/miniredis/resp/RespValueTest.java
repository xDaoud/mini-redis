package dev.xDaoud.miniredis.resp;

import org.junit.jupiter.api.Test;

import static dev.xDaoud.miniredis.resp.RespValue.BulkString;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RespValueTest {

    @Test
    void bulkStringsWithSameContentAreEqual() {
        assertEquals(BulkString.of("hello"), BulkString.of("hello"));
    }
}
