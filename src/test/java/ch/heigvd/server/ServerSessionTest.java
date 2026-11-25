package ch.heigvd.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** In-memory unit tests for {@link ServerSession}. */
class ServerSessionTest {
    private InMemoryRoomRegistry registry;
    private ServerSession sessionA;
    private ServerSession sessionB;

    @BeforeEach
    void setUp() {
        registry = new InMemoryRoomRegistry();
        sessionA = new ServerSession(registry);
        sessionB = new ServerSession(registry);
    }

    @Test
    void ignoresEmptyAndNullLines() {
        assertNull(sessionA.handleMessage(null));
        assertNull(sessionA.handleMessage(""));
        assertNull(sessionA.handleMessage("   \t  "));
    }

    @Test
    void rejectsUnknownCommand() {
        assertEquals("ERROR 1 UNKNOWN_COMMAND", sessionA.handleMessage("HELLO"));
        assertEquals("ERROR 1 UNKNOWN_COMMAND", sessionA.handleMessage("create notes.txt"));
    }

    @Test
    void handlesQuit() {
        assertEquals("ERROR 5 BAD_ARGUMENTS", sessionA.handleMessage("QUIT extra"));
        assertEquals("BYE", sessionA.handleMessage("QUIT"));
        assertFalse(sessionA.isInRoom());
    }

    @Test
    void handlesCreateAndJoinValidation() {
        assertEquals("ERROR 5 BAD_ARGUMENTS", sessionA.handleMessage("CREATE"));
        assertEquals("ERROR 5 BAD_ARGUMENTS", sessionA.handleMessage("CREATE my notes.txt"));
        assertEquals("ERROR 3 ROOM_NOT_FOUND", sessionA.handleMessage("JOIN WRONG"));
        assertEquals("ERROR 5 BAD_ARGUMENTS", sessionA.handleMessage("JOIN"));
        assertEquals("ERROR 5 BAD_ARGUMENTS", sessionA.handleMessage("JOIN A7B9X extra"));

        assertEquals("OK ROOM1", sessionA.handleMessage("CREATE notes.txt"));
        assertTrue(sessionA.isInRoom());
        assertEquals("ERROR 2 INVALID_STATE", sessionA.handleMessage("CREATE other.txt"));
        assertEquals("ERROR 2 INVALID_STATE", sessionA.handleMessage("JOIN ROOM1"));
    }

    private static class InMemoryRoomRegistry implements RoomRegistry {
        private final Map<String, InMemoryRoom> rooms = new HashMap<>();
        private int counter = 0;
        String lastAppliedJson;

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

        private class InMemoryRoom implements SyncRoom {
            private final String roomCode;
            private final String filename;
            private String checksum = "0";
            private String fullStateJson = "[]";
            private int version = 0;

            InMemoryRoom(String roomCode, String filename) {
                this.roomCode = roomCode;
                this.filename = filename;
            }

            @Override
            public String getRoomCode() {
                return roomCode;
            }

            @Override
            public RoomState getState() {
                return new RoomState(filename, checksum, fullStateJson);
            }

            @Override
            public PatchResult applyPatch(String prevChecksum, String jsonDeltas) {
                if (jsonDeltas.contains("INVALID_PATCH")) {
                    return PatchResult.invalid();
                }
                if (!checksum.equals(prevChecksum)) {
                    return PatchResult.outdated(checksum, fullStateJson);
                }
                lastAppliedJson = jsonDeltas;
                checksum = "chk_v" + (++version);
                fullStateJson = jsonDeltas;
                return PatchResult.ok(checksum);
            }
        }
    }
}
