package dev.xDaoud.miniredis.resp;

import java.io.IOException;

public class RespProtocolException extends IOException {
    public RespProtocolException(String message) {
        super(message);
    }
}
