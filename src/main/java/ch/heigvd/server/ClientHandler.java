package ch.heigvd.server;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/** Handles a single client TCP connection, routing line-framed requests to {@link ServerSession}. */
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final ServerSession session;

    /** Creates a handler for {@code socket} backed by {@code roomRegistry}. */
    public ClientHandler(Socket socket, RoomRegistry roomRegistry) {
        this.socket = socket;
        this.session = new ServerSession(roomRegistry);
    }

    @Override
    public void run() {
        try (
                socket;
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))
        ) {
            String line;
            while ((line = in.readLine()) != null) {
                String answer = session.handleMessage(line);
                if (answer == null) {
                    continue;
                }
                out.write(answer + '\n');
                out.flush();
                if ("BYE".equals(answer)) {
                    break;
                }
            }
        } catch (IOException ignored) {
        } finally {
            session.close();
        }
    }
}
