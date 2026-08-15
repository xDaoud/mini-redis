package dev.xDaoud.miniredis.resp;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public sealed interface RespValue {

    record SimpleString(String value) implements RespValue { }

    record RespError(String message) implements RespValue { }

    record RespInteger(long value) implements RespValue { }

    /** payload == null represents the RESP2 null bulk string, {@code $-1\r\n}. */
    record BulkString(byte[] payload) implements RespValue {

        public static final BulkString NULL = new BulkString(null);

        public static BulkString of(String s) {
            return new BulkString(s.getBytes(StandardCharsets.UTF_8));
        }

        public String asString() {
            return payload == null ? null : new String(payload, StandardCharsets.UTF_8);
        }

        // Records derive equals/hashCode from components, and for arrays that is
        // reference equality. Without these overrides every content assertion fails.
        @Override
        public boolean equals(Object o) {
            return o instanceof BulkString other && Arrays.equals(payload, other.payload);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(payload);
        }

        @Override
        public String toString() {
            return payload == null ? "BulkString[null]" : "BulkString[" + asString() + "]";
        }
    }

    /** elements == null represents the RESP2 null array, {@code *-1\r\n}. */
    record RespArray(List<RespValue> elements) implements RespValue {

        public static final RespArray NULL = new RespArray(null);

        public static RespArray of(RespValue... values) {
            return new RespArray(List.of(values));
        }
    }
}