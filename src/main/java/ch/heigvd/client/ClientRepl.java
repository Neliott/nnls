package ch.heigvd.client;

import ch.heigvd.common.DiffEngine;
import ch.heigvd.common.NetworkDelta;
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
            // 1. Connect to the server if not already connected
            if (!conn.isConnected()) {
                conn.connect();
            }
            println(userOut, "Connected to NNLS server. Type HELP for available commands.");

            // 2. Read-eval-print loop until QUIT, EOF, or disconnect
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

    /** Parses and executes a REPL command; returns false when the loop should exit. */
    private boolean dispatch(String input, BufferedReader userIn, BufferedWriter userOut)
            throws IOException {
        String[] tokens = input.split("\\s+", 2); // Splits a string into a maximum of two parts at the first group of whitespace characters
        String command = tokens[0].toUpperCase(Locale.ROOT); // Make it case insensitive
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
                return handlePull(userIn, userOut);
            case "LEAVE":
                return handleLeave(userOut);
            case "QUIT":
                return handleQuit(userOut);
            default:
                println(userOut, "Unknown command '" + tokens[0] + "'. Type HELP for commands.");
                return true;
        }
    }

    /** Creates a room from a local file and uploads its initial state; returns true to continue. */
    private boolean handleCreate(String args, BufferedWriter userOut) throws IOException {
        // 1. Validate session state and local file path
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

        // 2. Read local file contents before creating the room
        LocalState candidate = stateFactory.create(localPath);
        List<String> lines;
        try {
            lines = candidate.readLocalLines();
        } catch (IOException e) {
            println(userOut, "Cannot read local file '" + localPath + "': " + e.getMessage());
            return true;
        }

        // 3. Create the room on the server
        ServerResponse createRes = send("CREATE " + fileName, userOut);
        if (!isOk(createRes, userOut)) {
            return true;
        }
        state = candidate;
        currentRoomCode = createRes.getPayload();

        // 4. Upload initial document state (PUSH 0)
        String initJson = DiffEngine.serializeDeltas(
                DiffEngine.computeDeltas(Collections.emptyList(), lines));
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

    /** Joins a room and saves the remote file locally; returns true to continue the REPL. */
    private boolean handleJoin(String args, BufferedWriter userOut) throws IOException {
        // 1. Validate session state and target local directory
        if (!requireRoom(false, userOut) || args.isEmpty()) {
            if (state == null && args.isEmpty()) {
                println(userOut, "Usage: JOIN <roomCode> [directory]");
            }
            return true;
        }
        String[] joinArgs = args.split("\\s+", 2); // Splits a string into a maximum of two parts at the first group of whitespace characters
        Path targetDir = (joinArgs.length > 1) ? Paths.get(joinArgs[1]) : Paths.get(".");
        if (!targetDir.toFile().isDirectory()) {
            println(userOut, "Directory does not exist: " + targetDir);
            return true;
        }

        // 2. Request room state from the server and decode the document
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

        // 3. Save the remote file locally and initialize sync state
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

    /** Prints local deltas against the synced baseline; returns true to continue the REPL. */
    private boolean handleDiff(BufferedWriter userOut) throws IOException {
        if (!requireRoom(true, userOut)) {
            return true;
        }
        List<NetworkDelta> deltas = readDiff(userOut);
        if (deltas == null) {
            return true;
        }
        if (deltas.isEmpty()) {
            println(userOut, "No local changes (checksum: " + state.getSyncedChecksum() + ").");
            return true;
        }
        println(userOut, "Local changes (" + deltas.size() + " delta(s)):");
        for (NetworkDelta d : deltas) {
            println(userOut, "  " + d.type + " at line " + d.pos + " (size " + d.size + "): " + d.lines);
        }
        return true;
    }

    /** Displays the current local file with 0-based line numbers; returns true to continue. */
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
        println(userOut, "--- " + state.getLocalPath() + " (checksum: " + state.getSyncedChecksum() + ") ---");
        if (lines.isEmpty()) {
            println(userOut, "(empty file)");
        } else {
            for (int i = 0; i < lines.size(); i++) {
                println(userOut, i + ": " + lines.get(i));
            }
        }
        return true;
    }

    /** Sends local diffs to the server and triggers conflict resolution on ERROR 4; returns true to continue. */
    private boolean handlePush(BufferedReader userIn, BufferedWriter userOut) throws IOException {
        // 1. Validate room state and compute local diff
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

        // 2. Send patch and handle OK or conflict (ERROR 4)
        ServerResponse res = send(
                "PUSH " + state.getSyncedChecksum() + " " + DiffEngine.serializeDeltas(deltas), userOut);
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

    /** Prompts the user to accept remote or rebase local edits on conflict; returns false on EOF. */
    private boolean handleConflict(
            String errorData, BufferedReader userIn, BufferedWriter userOut) throws IOException {
        // 1. Extract valid checksum and decode remote document state
        String[] parts = errorData.split(" ", 2);
        if (parts.length < 2 || parts[0].isEmpty()) {
            println(userOut, "Conflict detected, but server omitted valid state data.");
            return true;
        }
        List<String> remoteLines = decodeState(parts[1], userOut);
        if (remoteLines == null) {
            return true;
        }
        return resolveConflict(parts[0], remoteLines, userIn, userOut);
    }

    /** Runs the interactive conflict prompt (1: accept remote, 2: keep local and rebase); returns false on EOF. */
    private boolean resolveConflict(
            String validChecksum,
            List<String> remoteLines,
            BufferedReader userIn,
            BufferedWriter userOut) throws IOException {
        println(userOut, "Conflict detected (server checksum: " + validChecksum + ").");
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
                    applyRemote(validChecksum, remoteLines, "Accepted remote version", userOut);
                    return true;
                case "2":
                    state.keepLocalAndRebase(remoteLines, validChecksum);
                    println(userOut, "Kept local edits; baseline rebased to " + validChecksum + ".");
                    return true;
                default:
                    println(userOut, "Invalid choice. Enter 1 or 2.");
                    break;
            }
        }
    }

    /** Fetches remote updates if the server checksum changed, prompting on local conflict; returns true to continue. */
    private boolean handlePull(BufferedReader userIn, BufferedWriter userOut) throws IOException {
        // 1. Query server for updates against current checksum
        if (!requireRoom(true, userOut)) {
            return true;
        }
        ServerResponse res = send("PULL " + state.getSyncedChecksum(), userOut);
        if (res == null) {
            return true;
        }
        if (res.getKind() == ServerResponse.Kind.UP_TO_DATE) {
            println(userOut, "Already up to date (checksum: " + state.getSyncedChecksum() + ").");
            return true;
        }
        if (res.getKind() != ServerResponse.Kind.SYNC) {
            printUnexpected(res, userOut);
            return true;
        }

        // 2. Decode SYNC payload and check for unpushed local edits before overwriting
        String[] parts = res.getPayload().split(" ", 2);
        if (parts.length < 2 || parts[0].isEmpty()) {
            println(userOut, "Received invalid SYNC payload from server.");
            return true;
        }
        List<String> remoteLines = decodeState(parts[1], userOut);
        if (remoteLines == null) {
            return true;
        }
        List<NetworkDelta> localDeltas = readDiff(userOut);
        if (localDeltas == null) {
            return true;
        }
        if (localDeltas.isEmpty()) {
            applyRemote(parts[0], remoteLines, "Pulled updates from server", userOut);
            return true;
        }
        return resolveConflict(parts[0], remoteLines, userIn, userOut);
    }

    /** Leaves the active room and resets local sync state; returns true to continue the REPL. */
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

    /** Sends QUIT to the server and returns false to exit the REPL loop on BYE or disconnect. */
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

    /** Computes local deltas or prints an I/O error and returns null. */
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

    /** Sends a request and returns the parsed response, or null on disconnect or malformed reply. */
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

    /** Returns true if the response is OK, otherwise prints the error/unexpected kind and returns false. */
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

    /** Checks whether the session matches the expected room state, printing guidance and returning false if not. */
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

    /** Reconstructs document lines from a full-state JSON delta array, or returns null on error. */
    private List<String> decodeState(String json, BufferedWriter userOut) throws IOException {
        try {
            List<NetworkDelta> deltas = DiffEngine.deserializeDeltas(json);
            if (deltas == null) {
                throw new IllegalArgumentException("State JSON must not be null");
            }
            return DiffEngine.applyDeltas(new ArrayList<>(), deltas);
        } catch (RuntimeException e) {
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
        userOut.write(message);
        userOut.newLine(); // Writes the OS-specific line separator (System.lineSeparator()). Ok cause we're in the console here!
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
