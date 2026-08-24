package dev.xDaoud.miniredis;


import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RedisServerTest {

    @Test
    void respondsToPingOverARealSocket() throws Exception {
        try (RedisServer server = new RedisServer(0)) {   // ephemeral port, never 6379
            Thread.ofVirtual().start(() -> {
                try {
                    server.serveForever();
                } catch (IOException ignored) {
                }
            });

            try (Socket socket = new Socket("localhost", server.port())) {
                socket.getOutputStream()
                      .write("*1\r\n$4\r\nPING\r\n".getBytes(StandardCharsets.UTF_8));
                socket.getOutputStream().flush();

                byte[] reply = new byte[7];
                new DataInputStream(socket.getInputStream()).readFully(reply);
                assertEquals("+PONG\r\n", new String(reply, StandardCharsets.UTF_8));
            }
        }
    }
}