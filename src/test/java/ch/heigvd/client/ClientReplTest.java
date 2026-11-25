package ch.heigvd.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.heigvd.common.DiffEngine;
import ch.heigvd.common.NetworkDelta;
import ch.heigvd.server.PatchResult;
import ch.heigvd.server.RoomRegistry;
import ch.heigvd.server.RoomState;
import ch.heigvd.server.ServerSession;
import ch.heigvd.server.SyncRoom;
import com.github.difflib.patch.DeltaType;
import com.github.difflib.patch.PatchFailedException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit tests for {@link ClientRepl} using injectable streams and in-memory stubs. */
class ClientReplTest {

    @TempDir
    Path tempDir;

    @Test
    void handlesHelpUnknownCommandAndQuitCaseInsensitively() {
        InMemoryFiles files = new InMemoryFiles();
        SessionTransport transport = new SessionTransport(new ServerSession(new InMemoryRegistry()));

        String out = runRepl(transport, files, "help\nUNKNOWN\nquit\n");

        assertTrue(out.contains("Commands:"));
        assertTrue(out.contains("Unknown command 'UNKNOWN'"));
        assertTrue(out.contains("Goodbye!"));
        assertEquals(List.of("QUIT"), transport.sent);
    }

    @Test
    void handlesCreateShowDiffPushAndLeaveWithServerSession() {
        InMemoryFiles files = new InMemoryFiles();
        Path file = tempDir.resolve("notes.txt");
        files.disk.put(file, List.of("Hello", "World"));
        SessionTransport transport = new SessionTransport(new ServerSession(new InMemoryRegistry()));

        String out = runRepl(
                transport,
                files,
                "CREATE " + file + "\nshow\ndiff\npush\nleave\nquit\n");

        assertTrue(out.contains("Room ROOM1 created."));
        assertTrue(out.contains("0: Hello"));
        assertTrue(out.contains("1: World"));
        assertTrue(out.contains("No local changes"));
        assertTrue(out.contains("No local changes to push."));
        assertTrue(out.contains("Left room ROOM1."));
        assertTrue(out.contains("Goodbye!"));
    }

    @Test
    void handlesJoinIntoDirectoryAndPullSync() {
        InMemoryFiles files = new InMemoryFiles();
        String v1 = DiffEngine.serializeDeltas(List.of(
                new NetworkDelta(DeltaType.INSERT, 0, 0, List.of("v1"))));
        String v2 = DiffEngine.serializeDeltas(List.of(
                new NetworkDelta(DeltaType.INSERT, 0, 0, List.of("v1", "v2"))));

        ScriptedTransport transport = new ScriptedTransport(List.of(
                "OK shared.txt chk_1 " + v1,
                "UP_TO_DATE",
                "SYNC chk_2 " + v2,
                "BYE"));

        String out = runRepl(transport, files,
                "JOIN ROOM1 " + tempDir + "\nPULL\nPULL\nSHOW\nQUIT\n");

        Path joined = tempDir.resolve("shared.txt").normalize();
        assertEquals(List.of("v1", "v2"), files.disk.get(joined));
        assertTrue(out.contains("Joined room ROOM1. Saved 'shared.txt'"));
        assertTrue(out.contains("Already up to date"));
        assertTrue(out.contains("Pulled updates from server (checksum: chk_2)."));
        assertTrue(out.contains("1: v2"));
    }

    @Test
    void handlesConflictAcceptRemoteAndKeepLocalStrategies() {
        String remoteJson = DiffEngine.serializeDeltas(List.of(
                new NetworkDelta(DeltaType.INSERT, 0, 0, List.of("Hello", "Alice"))));

        InMemoryFiles files1 = new InMemoryFiles();
        Path doc1 = tempDir.resolve("a.txt");
        files1.disk.put(doc1, List.of("Hello", "Init"));
        ScriptedTransport t1 = new ScriptedTransport(List.of(
                "OK ROOM1", "OK chk_1", "ERROR 4 OUTDATED_CHECKSUM chk_2 " + remoteJson, "BYE"));
        String out1 = runRepl(t1, new EditingFactory(files1, doc1, List.of("Hello", "Bob")),
                "CREATE " + doc1 + "\nPUSH\n1\nQUIT\n");

        assertTrue(out1.contains("Conflict detected"));
        assertTrue(out1.contains("Accepted remote version (checksum: chk_2)."));
        assertEquals(List.of("Hello", "Alice"), files1.disk.get(doc1));

        InMemoryFiles files2 = new InMemoryFiles();
        Path doc2 = tempDir.resolve("b.txt");
        files2.disk.put(doc2, List.of("Hello", "Init"));
        ScriptedTransport t2 = new ScriptedTransport(List.of(
                "OK ROOM1", "OK chk_1", "ERROR 4 OUTDATED_CHECKSUM chk_2 " + remoteJson,
                "OK chk_3", "BYE"));
        String out2 = runRepl(t2, new EditingFactory(files2, doc2, List.of("Hello", "Bob")),
                "CREATE " + doc2 + "\nPUSH\nbad\n2\nPUSH\nQUIT\n");

        assertTrue(out2.contains("Invalid choice."));
        assertTrue(out2.contains("Kept local edits; baseline rebased to chk_2."));
        assertTrue(out2.contains("Push successful. New checksum: chk_3"));
        assertEquals(List.of("Hello", "Bob"), files2.disk.get(doc2));
        assertTrue(t2.sent.get(3).startsWith("PUSH chk_2 "));
    }

    @Test
    void handlesErrorsAndServerDisconnect() {
        ScriptedTransport transport = new ScriptedTransport(List.of(
                "ERROR 1 UNKNOWN_COMMAND",
                "ERROR 2 INVALID_STATE",
                "ERROR 3 ROOM_NOT_FOUND",
                "ERROR 5 BAD_ARGUMENTS"));

        String out = runRepl(transport, new InMemoryFiles(),
                "JOIN R1\nJOIN R2\nJOIN R3\nJOIN R4\nLEAVE\nQUIT\n");

        assertTrue(out.contains("unknown command"));
        assertTrue(out.contains("session state"));
        assertTrue(out.contains("Room not found"));
        assertTrue(out.contains("invalid arguments"));
        assertTrue(out.contains("Not in a room."));
        assertTrue(out.contains("Server closed the connection."));
        assertFalse(transport.connected);
    }

    private String runRepl(ClientTransport transport, LocalStateFactory factory, String script) {
        ByteArrayInputStream in = new ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new ClientRepl(transport, factory, in, out).start();
        return out.toString(StandardCharsets.UTF_8);
    }

    private static class InMemoryFiles implements LocalStateFactory {
        final Map<Path, List<String>> disk = new HashMap<>();

        @Override
        public LocalState create(Path localPath) {
            return new InMemoryState(this, localPath);
        }
    }

    private static class EditingFactory implements LocalStateFactory {
        private final InMemoryFiles files;
        private final Path target;
        private final List<String> pendingEdit;
        private boolean applied = false;

        EditingFactory(InMemoryFiles files, Path target, List<String> pendingEdit) {
            this.files = files;
            this.target = target;
            this.pendingEdit = pendingEdit;
        }

        @Override
        public LocalState create(Path localPath) {
            return new InMemoryState(files, localPath) {
                @Override
                public List<NetworkDelta> computeLocalDiff() throws IOException {
                    if (!applied) {
                        applied = true;
                        files.disk.put(target, pendingEdit);
                    }
                    return super.computeLocalDiff();
                }
            };
        }
    }

    private static class InMemoryState implements LocalState {
        private final InMemoryFiles files;
        private final Path path;
        private List<String> synced = new ArrayList<>();
        private String checksum = "0";

        InMemoryState(InMemoryFiles files, Path path) {
            this.files = files;
            this.path = path;
        }

        @Override
        public List<String> readLocalLines() throws IOException {
            List<String> lines = files.disk.get(path);
            if (lines == null) {
                throw new FileNotFoundException(path.toString());
            }
            return new ArrayList<>(lines);
        }

        @Override
        public void writeLocalLines(List<String> lines) {
            files.disk.put(path, new ArrayList<>(lines));
        }

        @Override
        public List<NetworkDelta> computeLocalDiff() throws IOException {
            return DiffEngine.computeDeltas(synced, readLocalLines());
        }

        @Override
        public void acceptRemote(List<String> remoteLines, String validChecksum) {
            writeLocalLines(remoteLines);
            updateSyncedState(remoteLines, validChecksum);
        }

        @Override
        public void keepLocalAndRebase(List<String> remoteLines, String validChecksum) {
            updateSyncedState(remoteLines, validChecksum);
        }

        @Override
        public void updateSyncedState(List<String> lines, String checksum) {
            this.synced = new ArrayList<>(lines);
            this.checksum = checksum;
        }

        @Override
        public Path getLocalPath() { return path; }

        @Override
        public String getCurrentChecksum() { return checksum; }
    }

    private static class SessionTransport implements ClientTransport {
        private final ServerSession session;
        final List<String> sent = new ArrayList<>();
        boolean connected = false;

        SessionTransport(ServerSession session) { this.session = session; }

        @Override public void connect() { connected = true; }
        @Override public boolean isConnected() { return connected; }
        @Override public void close() { connected = false; session.close(); }

        @Override
        public String sendRequest(String request) {
            sent.add(request);
            String res = session.handleMessage(request);
            if ("BYE".equals(res)) {
                connected = false;
            }
            return res;
        }
    }

    private static class ScriptedTransport implements ClientTransport {
        private final List<String> responses;
        final List<String> sent = new ArrayList<>();
        private int index = 0;
        boolean connected = false;

        ScriptedTransport(List<String> responses) { this.responses = responses; }

        @Override public void connect() { connected = true; }
        @Override public boolean isConnected() { return connected; }
        @Override public void close() { connected = false; }

        @Override
        public String sendRequest(String request) {
            sent.add(request);
            if (index >= responses.size()) {
                connected = false;
                return null;
            }
            String res = responses.get(index++);
            if ("BYE".equals(res)) {
                connected = false;
            }
            return res;
        }
    }

    private static class InMemoryRegistry implements RoomRegistry {
        private final Map<String, InMemoryRoom> rooms = new HashMap<>();
        private int counter = 0;

        @Override
        public SyncRoom createRoom(String filename) {
            String code = "ROOM" + (++counter);
            InMemoryRoom room = new InMemoryRoom(code, filename);
            rooms.put(code, room);
            return room;
        }

        @Override
        public Optional<SyncRoom> getRoom(String roomCode) {
            return Optional.ofNullable(rooms.get(roomCode));
        }

        private static class InMemoryRoom implements SyncRoom {
            private final String code;
            private final String filename;
            private List<String> lines = new ArrayList<>();
            private String checksum = "0";

            InMemoryRoom(String code, String filename) {
                this.code = code;
                this.filename = filename;
            }

            @Override public String getRoomCode() { return code; }

            @Override
            public RoomState getState() {
                String json = lines.isEmpty() ? "[]" : DiffEngine.serializeDeltas(
                        Collections.singletonList(new NetworkDelta(DeltaType.INSERT, 0, 0, lines)));
                return new RoomState(filename, checksum, json);
            }

            @Override
            public PatchResult applyPatch(String prevChecksum, String jsonDeltas) {
                if (!checksum.equals(prevChecksum)) {
                    return PatchResult.outdated(checksum, getState().fullStateJson);
                }
                try {
                    lines = DiffEngine.applyDeltas(lines, DiffEngine.deserializeDeltas(jsonDeltas));
                    checksum = DiffEngine.computeSha256(lines);
                    return PatchResult.ok(checksum);
                } catch (PatchFailedException | RuntimeException e) {
                    return PatchResult.invalid();
                }
            }
        }
    }
}
