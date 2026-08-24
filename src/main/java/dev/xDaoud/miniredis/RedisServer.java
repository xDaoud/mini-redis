package dev.xDaoud.miniredis;


import dev.xDaoud.miniredis.resp.RespProtocolException;
import dev.xDaoud.miniredis.resp.RespReader;
import dev.xDaoud.miniredis.resp.RespValue;
import dev.xDaoud.miniredis.resp.RespWriter;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class RedisServer implements AutoCloseable {

    private final ServerSocket serverSocket;

    public RedisServer(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
    }

    public int port() {
        return serverSocket.getLocalPort();
    }

    public void serveForever() throws IOException {
        while (!serverSocket.isClosed()) {
            Socket client = serverSocket.accept();
            handleConnection(client);          // Phase 3: Thread.ofVirtual().start(...)
        }
    }

    private void handleConnection(Socket socket) {
        System.out.println("client connected: " + socket.getRemoteSocketAddress());
        try (socket) {
            RespReader reader = new RespReader(socket.getInputStream());
            RespWriter writer = new RespWriter(socket.getOutputStream());

            RespValue request;
            while ((request = reader.read()) != null) {   // null == clean disconnect
                writer.write(execute(request));
                writer.flush();                            // or redis-cli hangs forever
            }
        } catch (RespProtocolException e) {
            System.err.println("protocol error: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("connection error: " + e.getMessage());
        }
        System.out.println("client disconnected");
    }

    private RespValue execute(RespValue request) {
        List<String> args;
        try {
            args = toCommand(request);
        } catch (RespProtocolException e) {
            return new RespValue.RespError("ERR Protocol error: " + e.getMessage());
        }
        if (args.isEmpty()) {
            return new RespValue.RespError("ERR Protocol error: empty command");
        }

        String name = args.get(0).toUpperCase(Locale.ROOT);
        return switch (name) {
            case "PING" -> args.size() == 1
                    ? new RespValue.SimpleString("PONG")
                    : RespValue.BulkString.of(args.get(1));
            case "ECHO" -> args.size() == 2
                    ? RespValue.BulkString.of(args.get(1))
                    : new RespValue.RespError("ERR wrong number of arguments for 'echo' command");
            default -> new RespValue.RespError("ERR unknown command '" + args.get(0) + "'");
        };
    }

    /** Clients always send commands as an array of bulk strings. Enforce it. */
    private static List<String> toCommand(RespValue request) throws RespProtocolException {
        if (!(request instanceof RespValue.RespArray(List<RespValue> elements))
                || elements == null) {
            throw new RespProtocolException("expected an array");
        }
        List<String> args = new ArrayList<>(elements.size());
        for (RespValue element : elements) {
            if (!(element instanceof RespValue.BulkString bulk) || bulk.payload() == null) {
                throw new RespProtocolException("expected an array of bulk strings");
            }
            args.add(bulk.asString());
        }
        return args;
    }

    @Override
    public void close() throws IOException {
        serverSocket.close();
    }
}