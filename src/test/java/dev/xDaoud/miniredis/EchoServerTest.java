package dev.xDaoud.miniredis;

import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EchoServerTest {

    @Test
    void echoesBytesBack() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {   // 0 = OS picks a free port
            int port = server.getLocalPort();

            Thread.ofVirtual().start(() -> {
                try (Socket client = server.accept();
                     InputStream in = client.getInputStream();
                     OutputStream out = client.getOutputStream()) {
                    in.transferTo(out);
                } catch (IOException ignored) {
                }
            });

            try (Socket socket = new Socket("localhost", port)) {
                socket.getOutputStream().write("hello\n".getBytes(StandardCharsets.UTF_8));
                socket.getOutputStream().flush();

                byte[] reply = new byte[6];
                new DataInputStream(socket.getInputStream()).readFully(reply);
                assertEquals("hello\n", new String(reply, StandardCharsets.UTF_8));
            }
        }
    }
}