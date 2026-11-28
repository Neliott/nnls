package ch.heigvd.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Multithreaded TCP server accepting NNLSP client connections on virtual threads. */
public class TcpServer {
    private final RoomRegistry roomRegistry;
    private final ServerSocket serverSocket;
    private final ExecutorService executor;

    /** Creates a server bound to {@code port} backed by a default {@link RoomManager}. */
    public TcpServer(int port) throws IOException {
        this(port, new RoomManager());
    }

    /** Creates a server bound to {@code port} using the given {@link RoomRegistry}. */
    public TcpServer(int port, RoomRegistry roomRegistry) throws IOException {
        this.roomRegistry = Objects.requireNonNull(roomRegistry, "roomRegistry must not be null");
        this.serverSocket = new ServerSocket(port);
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    /** Returns the local port the server socket is bound to. */
    public int getPort() {
        return serverSocket.getLocalPort();
    }

    /** Starts accepting incoming TCP connections until {@link #stop()} is called. */
    public void start() {
        try (serverSocket) {
            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                executor.submit(new ClientHandler(clientSocket, this.roomRegistry));
            }
        } catch (Exception e) {
            if (!serverSocket.isClosed()) {
                throw new RuntimeException("Error starting server: " + e.getMessage());
            }
        }
    }

    /** Closes the server socket and shuts down the client executor. */
    public void stop() throws IOException {
        serverSocket.close();
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
