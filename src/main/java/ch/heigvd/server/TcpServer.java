package ch.heigvd.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TcpServer {
    private final RoomRegistry roomRegistry;
    private final ServerSocket serverSocket;
    private final ExecutorService executor;

    public TcpServer(int port) throws IOException {
        this.roomRegistry = new RoomManager();
        this.serverSocket = new ServerSocket(port);
        executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    public void start(){
        try (serverSocket){
            while(!serverSocket.isClosed()){
                Socket clientSocket = serverSocket.accept();
                executor.submit(new ClientHandler(clientSocket, this.roomRegistry));
            }
        } catch (Exception e) {
            if (!serverSocket.isClosed()) {
                throw new RuntimeException("Error starting server: " + e.getMessage());
            }
        }
    }

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
