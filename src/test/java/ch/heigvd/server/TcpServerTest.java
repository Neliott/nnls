package ch.heigvd.server;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/** Integration tests for {@link TcpServer} and {@link ClientHandler}. */
class TcpServerTest {

    @Test
    void framesLinesSkipsEmptyLinesAndClosesOnQuit() throws Exception {
        TcpServer server = new TcpServer(0);
        Thread serverThread = Thread.ofVirtual().start(server::start);

        try (TestClient client = new TestClient(server.getPort())) {
            String createResp = client.send("\nCREATE notes.txt");
            assertNotNull(createResp);
            assertTrue(createResp.matches("OK [A-Z0-9]{5}"), "Unexpected: " + createResp);

            assertEquals("BYE", client.send("QUIT"));
            assertNull(client.readLine());
        } finally {
            server.stop();
            serverThread.join(2000);
        }
    }

    @Test
    void acceptsConcurrentClientsWithSharedRegistry() throws Exception {
        TcpServer server = new TcpServer(0);
        Thread serverThread = Thread.ofVirtual().start(server::start);

        try (TestClient clientA = new TestClient(server.getPort());
             TestClient clientB = new TestClient(server.getPort())) {
            String roomCode = clientA.send("CREATE shared.txt").substring(3);
            assertEquals("OK shared.txt 0 []", clientB.send("JOIN " + roomCode));

            assertEquals("BYE", clientA.send("QUIT"));
            assertEquals("BYE", clientB.send("QUIT"));
        } finally {
            server.stop();
            serverThread.join(2000);
        }
    }

    private static final class TestClient implements AutoCloseable {
        private final Socket socket;
        private final BufferedReader in;
        private final BufferedWriter out;

        TestClient(int port) throws IOException {
            this.socket = new Socket("127.0.0.1", port);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        }

        String send(String line) throws IOException {
            out.write(line + '\n');
            out.flush();
            return in.readLine();
        }

        String readLine() throws IOException {
            return in.readLine();
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }
    }
}
