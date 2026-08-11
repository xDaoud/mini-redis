package dev.xDaoud.miniredis;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public final class EchoServer {

    public static void main(String[] args) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(6379)) {
            System.out.println("listening on port " + serverSocket.getLocalPort());

            while (true) {
                Socket client = serverSocket.accept();
                System.out.println("client connected: " + client.getRemoteSocketAddress());

                try (client) {
                    InputStream in = client.getInputStream();
                    OutputStream out = client.getOutputStream();

                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                        out.flush();
                    }
                }

                System.out.println("client disconnected");
            }
        }
    }
}