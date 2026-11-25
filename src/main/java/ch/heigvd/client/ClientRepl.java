package ch.heigvd.client;

import ch.heigvd.common.DiffEngine;
import ch.heigvd.common.NetworkDelta;
import com.github.difflib.patch.DeltaType;
import com.github.difflib.patch.PatchFailedException;
import com.google.gson.JsonSyntaxException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
            case "CREATE":
                return handleCreate(args, userOut);
            case "JOIN":
                return handleJoin(args, userOut);
            case "DIFF":
            case "STATUS":
                return handleDiff(userOut);
            case "SHOW":
                return handleShow(userOut);
            case "PUSH":
                return handlePush(userIn, userOut);
            case "PULL":
                return handlePull(userOut);
            case "LEAVE":
                return handleLeave(userOut);
            case "QUIT":
                return handleQuit(userOut);
            default:
                println(userOut, "Unknown command '" + tokens[0] + "'. Type HELP for commands.");
                return true;
        }
    }

    private boolean handleCreate(String args, BufferedWriter userOut) throws IOException {
        if (!requireRoom(false, userOut) || args.isEmpty()) {
            if (state == null && args.isEmpty()) {
                println(userOut, "Usage: CREATE <filepath>");
            }
            return true;
        }
        Path localPath = Paths.get(args);
        Path fileName = localPath.getFileName();
        if (fileName == null || fileName.toString().contains(" ") || fileName.toString().contains("\t")) {
            println(userOut, "Invalid filename (must be non-empty without spaces): " + args);
            return true;
        }
        LocalState candidate = stateFactory.create(localPath);
        List<String> lines;
        try {
            lines = candidate.readLocalLines();
        } catch (IOException e) {
            println(userOut, "Cannot read local file '" + localPath + "': " + e.getMessage());
            return true;
        }

        ServerResponse createRes = send("CREATE " + fileName, userOut);
        if (!isOk(createRes, userOut)) {
            return true;
        }
        state = candidate;
        currentRoomCode = createRes.getPayload();

        String initJson = lines.isEmpty() ? "[]" : DiffEngine.serializeDeltas(
                Collections.singletonList(new NetworkDelta(DeltaType.INSERT, 0, 0, lines)));
        ServerResponse pushRes = send("PUSH 0 " + initJson, userOut);
        if (pushRes != null && pushRes.getKind() == ServerResponse.Kind.OK) {
            candidate.updateSyncedState(lines, pushRes.getPayload());
            println(userOut, "Room " + currentRoomCode + " created. Uploaded '" + fileName
                    + "' (checksum: " + pushRes.getPayload() + ").");
        } else if (pushRes != null) {
            println(userOut, "Room " + currentRoomCode + " created, but initial upload failed:");
            printUnexpected(pushRes, userOut);
        }
        return true;
    }

    private boolean handleJoin(String args, BufferedWriter userOut) throws IOException {
        if (!requireRoom(false, userOut) || args.isEmpty()) {
            if (state == null && args.isEmpty()) {
                println(userOut, "Usage: JOIN <roomCode> [directory]");
            }
            return true;
        }
        String[] joinArgs = args.split("\\s+", 2);
        Path targetDir = (joinArgs.length > 1) ? Paths.get(joinArgs[1]) : Paths.get(".");
        if (!targetDir.toFile().isDirectory()) {
            println(userOut, "Directory does not exist: " + targetDir);
            return true;
        }

        ServerResponse res = send("JOIN " + joinArgs[0], userOut);
        if (!isOk(res, userOut)) {
            return true;
        }
        String[] parts = res.getPayload().split(" ", 3);
        if (parts.length < 3 || parts[0].isEmpty() || parts[1].isEmpty()) {
            println(userOut, "Received invalid JOIN state from server.");
            return true;
        }
        List<String> remoteLines = decodeState(parts[2], userOut);
        if (remoteLines == null) {
            return true;
        }

        Path targetFile = targetDir.resolve(parts[0]).normalize();
        LocalState newState = stateFactory.create(targetFile);
        try {
            newState.writeLocalLines(remoteLines);
        } catch (IOException e) {
            println(userOut, "Failed to write '" + targetFile + "': " + e.getMessage());
            return true;
        }
        newState.updateSyncedState(remoteLines, parts[1]);
        state = newState;
        currentRoomCode = joinArgs[0];
        println(userOut, "Joined room " + currentRoomCode + ". Saved '" + parts[0]
                + "' to " + targetFile + " (checksum: " + parts[1] + ").");
        return true;
    }

    private boolean handleDiff(BufferedWriter userOut) throws IOException {
        if (!requireRoom(true, userOut)) {
            return true;
        }
        List<NetworkDelta> deltas = readDiff(userOut);
        if (deltas == null) {
            return true;
        }
        if (deltas.isEmpty()) {
            println(userOut, "No local changes (checksum: " + state.getCurrentChecksum() + ").");
            return true;
        }
        println(userOut, "Local changes (" + deltas.size() + " delta(s)):");
        for (NetworkDelta d : deltas) {
            println(userOut, "  " + d.type + " at line " + d.pos + " (size " + d.size + "): " + d.lines);
        }
        return true;
    }

    private boolean handleShow(BufferedWriter userOut) throws IOException {
        if (!requireRoom(true, userOut)) {
            return true;
        }
        List<String> lines;
        try {
            lines = state.readLocalLines();
        } catch (IOException e) {
            println(userOut, "Cannot read local file: " + e.getMessage());
            return true;
        }
        println(userOut, "--- " + state.getLocalPath() + " (checksum: " + state.getCurrentChecksum() + ") ---");
        if (lines.isEmpty()) {
            println(userOut, "(empty file)");
        } else {
            for (int i = 0; i < lines.size(); i++) {
                println(userOut, i + ": " + lines.get(i));
            }
        }
        return true;
    }

    private boolean handlePush(BufferedReader userIn, BufferedWriter userOut) throws IOException {
        if (!requireRoom(true, userOut)) {
            return true;
        }
        List<String> localLines;
        try {
            localLines = state.readLocalLines();
        } catch (IOException e) {
            println(userOut, "Cannot read local file: " + e.getMessage());
            return true;
        }
        List<NetworkDelta> deltas = readDiff(userOut);
        if (deltas == null) {
            return true;
        }
        if (deltas.isEmpty()) {
            println(userOut, "No local changes to push.");
            return true;
        }

        ServerResponse res = send(
                "PUSH " + state.getCurrentChecksum() + " " + DiffEngine.serializeDeltas(deltas), userOut);
        if (res == null) {
            return true;
        }
        if (res.getKind() == ServerResponse.Kind.OK) {
            state.updateSyncedState(localLines, res.getPayload());
            println(userOut, "Push successful. New checksum: " + res.getPayload());
            return true;
        }
        if (res.getKind() == ServerResponse.Kind.ERROR && res.getErrorCode() == 4) {
            return handleConflict(res.getErrorData(), userIn, userOut);
        }
        printUnexpected(res, userOut);
        return true;
    }

    private boolean handleConflict(
            String errorData, BufferedReader userIn, BufferedWriter userOut) throws IOException {
        String[] parts = errorData.split(" ", 2);
        if (parts.length < 2 || parts[0].isEmpty()) {
            println(userOut, "Conflict detected, but server omitted valid state data.");
            return true;
        }
        List<String> remoteLines = decodeState(parts[1], userOut);
        if (remoteLines == null) {
            return true;
        }
        println(userOut, "Conflict detected (server checksum: " + parts[0] + ").");
        println(userOut, "  [1] Accept remote version (overwrite local file)");
        println(userOut, "  [2] Keep local edits (rebase baseline for re-push)");

        while (true) {
            userOut.write("Choice (1/2): ");
            userOut.flush();
            String choice = userIn.readLine();
            if (choice == null) {
                return false;
            }
            switch (choice.trim()) {
                case "1":
                    applyRemote(parts[0], remoteLines, "Accepted remote version", userOut);
                    return true;
                case "2":
                    state.keepLocalAndRebase(remoteLines, parts[0]);
                    println(userOut, "Kept local edits; baseline rebased to " + parts[0] + ".");
                    return true;
                default:
                    println(userOut, "Invalid choice. Enter 1 or 2.");
                    break;
            }
        }
    }

    private boolean handlePull(BufferedWriter userOut) throws IOException {
        if (!requireRoom(true, userOut)) {
            return true;
        }
        ServerResponse res = send("PULL " + state.getCurrentChecksum(), userOut);
        if (res == null) {
            return true;
        }
        if (res.getKind() == ServerResponse.Kind.UP_TO_DATE) {
            println(userOut, "Already up to date (checksum: " + state.getCurrentChecksum() + ").");
            return true;
        }
        if (res.getKind() != ServerResponse.Kind.SYNC) {
            printUnexpected(res, userOut);
            return true;
        }
        String[] parts = res.getPayload().split(" ", 2);
        if (parts.length < 2 || parts[0].isEmpty()) {
            println(userOut, "Received invalid SYNC payload from server.");
            return true;
        }
        List<String> remoteLines = decodeState(parts[1], userOut);
        if (remoteLines != null) {
            applyRemote(parts[0], remoteLines, "Pulled updates from server", userOut);
        }
        return true;
    }

    private boolean handleLeave(BufferedWriter userOut) throws IOException {
        if (!requireRoom(true, userOut)) {
            return true;
        }
        ServerResponse res = send("LEAVE", userOut);
        if (isOk(res, userOut)) {
            String leftRoom = currentRoomCode;
            clearRoomState();
            println(userOut, "Left room " + leftRoom + ".");
        }
        return true;
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

    private List<NetworkDelta> readDiff(BufferedWriter userOut) throws IOException {
        try {
            return state.computeLocalDiff();
        } catch (IOException e) {
            println(userOut, "Cannot read local file: " + e.getMessage());
            return null;
        }
    }

    private void applyRemote(
            String checksum, List<String> remoteLines, String prefix, BufferedWriter userOut)
            throws IOException {
        try {
            state.acceptRemote(remoteLines, checksum);
            println(userOut, prefix + " (checksum: " + checksum + ").");
        } catch (IOException e) {
            println(userOut, "Failed to update local file: " + e.getMessage());
        }
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

    private boolean isOk(ServerResponse res, BufferedWriter userOut) throws IOException {
        if (res == null) {
            return false;
        }
        if (res.getKind() == ServerResponse.Kind.OK) {
            return true;
        }
        printUnexpected(res, userOut);
        return false;
    }

    private boolean requireRoom(boolean mustBeInRoom, BufferedWriter userOut) throws IOException {
        if (mustBeInRoom && state == null) {
            println(userOut, "Not in a room. Use CREATE or JOIN first.");
            return false;
        }
        if (!mustBeInRoom && state != null) {
            println(userOut, "Already in room " + currentRoomCode + ". Use LEAVE first.");
            return false;
        }
        return true;
    }

    private List<String> decodeState(String json, BufferedWriter userOut) throws IOException {
        try {
            List<NetworkDelta> deltas = DiffEngine.deserializeDeltas(json);
            if (deltas == null) {
                throw new IllegalArgumentException("State JSON must not be null");
            }
            return DiffEngine.applyDeltas(new ArrayList<>(), deltas);
        } catch (IllegalArgumentException | PatchFailedException | JsonSyntaxException e) {
            println(userOut, "Failed to decode remote document state: " + e.getMessage());
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
