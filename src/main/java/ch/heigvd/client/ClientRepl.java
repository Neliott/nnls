package ch.heigvd.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;

/** Interactive terminal REPL client for collaborative synchronization over NNLSP. */
public class ClientRepl {
    private static final String EOL = "\n";

    private final ClientTransport client;
    private final LocalStateFactory stateFactory;
    private final InputStream in;
    private final OutputStream out;

    private LocalState state;
    private String currentRoomCode;
    private boolean disconnected;

    /** Creates a REPL client with injectable transport, state factory, and I/O streams. */
    public ClientRepl(
            ClientTransport client, LocalStateFactory stateFactory, InputStream in, OutputStream out) {
        this.client = Objects.requireNonNull(client, "client must not be null");
        this.stateFactory = Objects.requireNonNull(stateFactory, "stateFactory must not be null");
        this.in = Objects.requireNonNull(in, "in must not be null");
        this.out = Objects.requireNonNull(out, "out must not be null");
    }

    /** Connects to the server and runs the interactive command loop until exit or disconnect. */
    public void start() {
        BufferedReader userIn = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        BufferedWriter userOut = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));

        try (ClientTransport conn = client) {
            if (!conn.isConnected()) {
                conn.connect();
            }
            println(userOut, "Connected to NNLS server. Type HELP for available commands.");
            boolean running = true;
            while (running && !disconnected) {
                userOut.write("> ");
                userOut.flush();

                String line = userIn.readLine();
                if (line == null) {
                    break;
                }
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    running = dispatch(trimmed, userIn, userOut);
                }
            }
        } catch (IOException e) {
            safePrintln(userOut, "Connection error: " + e.getMessage());
        } finally {
            clearRoomState();
        }
    }

    private boolean dispatch(String input, BufferedReader userIn, BufferedWriter userOut)
            throws IOException {
        String[] tokens = input.split("\\s+", 2);
        String command = tokens[0].toUpperCase(Locale.ROOT);
        String args = (tokens.length > 1) ? tokens[1].trim() : "";

        switch (command) {
            case "HELP":
                println(userOut, "Commands: HELP | CREATE <filepath> | JOIN <roomCode> [dir] | "
                        + "DIFF | STATUS | SHOW | PUSH | PULL | LEAVE | QUIT");
                return true;
            case "QUIT":
                return handleQuit(userOut);
            default:
                println(userOut, "Unknown command '" + tokens[0] + "'. Type HELP for commands.");
                return true;
        }
    }

    private boolean handleQuit(BufferedWriter userOut) throws IOException {
        if (!client.isConnected()) {
            println(userOut, "Goodbye!");
            return false;
        }
        ServerResponse res = send("QUIT", userOut);
        if (res == null) {
            return false;
        }
        if (res.getKind() == ServerResponse.Kind.BYE) {
            clearRoomState();
            println(userOut, "Goodbye!");
            return false;
        }
        printUnexpected(res, userOut);
        return true;
    }

    private ServerResponse send(String request, BufferedWriter userOut) throws IOException {
        String raw = client.sendRequest(request);
        if (raw == null) {
            disconnected = true;
            println(userOut, "Server closed the connection.");
            return null;
        }
        try {
            return ServerResponse.parse(raw);
        } catch (IllegalArgumentException e) {
            println(userOut, "Malformed server response: " + raw);
            return null;
        }
    }

    private void printUnexpected(ServerResponse res, BufferedWriter userOut) throws IOException {
        println(userOut, res.getKind() == ServerResponse.Kind.ERROR
                ? res.formatErrorMessage()
                : "Unexpected server response: " + res.getKind());
    }

    private void clearRoomState() {
        state = null;
        currentRoomCode = null;
    }

    private void println(BufferedWriter userOut, String message) throws IOException {
        userOut.write(message + EOL);
        userOut.flush();
    }

    private void safePrintln(BufferedWriter userOut, String message) {
        try {
            println(userOut, message);
        } catch (IOException ignored) {
            // Output stream unavailable
        }
    }
}
