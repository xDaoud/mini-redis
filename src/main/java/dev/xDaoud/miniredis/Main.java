package dev.xDaoud.miniredis;

import java.io.IOException;

public final class Main {
    public static void main(String[] args) throws IOException {
        try (RedisServer server = new RedisServer(6379)) {
            System.out.println("mini-redis listening on port " + server.port());
            server.serveForever();
        }
    }
}