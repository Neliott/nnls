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

    @Test
    void handlesLeaveLifecycle() {
        assertEquals("ERROR 2 INVALID_STATE", sessionA.handleMessage("LEAVE"));
        sessionA.handleMessage("CREATE notes.txt");
        assertEquals("ERROR 5 BAD_ARGUMENTS", sessionA.handleMessage("LEAVE extra"));
        assertEquals("OK", sessionA.handleMessage("LEAVE"));
        assertFalse(sessionA.isInRoom());
        assertEquals("OK notes.txt 0 []", sessionA.handleMessage("JOIN ROOM1"));
    }

    @Test
    void rejectsPushAndPullOutsideRoomOrWithBadArgs() {
        assertEquals("ERROR 2 INVALID_STATE", sessionA.handleMessage("PUSH 0 []"));
        assertEquals("ERROR 2 INVALID_STATE", sessionA.handleMessage("PULL 0"));

        sessionA.handleMessage("CREATE notes.txt");
        assertEquals("ERROR 5 BAD_ARGUMENTS", sessionA.handleMessage("PUSH"));
        assertEquals("ERROR 5 BAD_ARGUMENTS", sessionA.handleMessage("PUSH 0"));
        assertEquals("ERROR 5 BAD_ARGUMENTS", sessionA.handleMessage("PUSH 0 not_json"));
        assertEquals("ERROR 5 BAD_ARGUMENTS", sessionA.handleMessage("PUSH 0 [\"bad\npayload\"]"));
        assertEquals("ERROR 5 BAD_ARGUMENTS", sessionA.handleMessage("PUSH 0 [\"INVALID_PATCH\"]"));
        assertEquals("ERROR 5 BAD_ARGUMENTS", sessionA.handleMessage("PULL"));
        assertEquals("ERROR 5 BAD_ARGUMENTS", sessionA.handleMessage("PULL 0 extra"));
    }

    @Test
    void handlesMultiClientSyncAndConflict() {
        assertEquals("OK ROOM1", sessionA.handleMessage("CREATE notes.txt"));
        String init = "[{\"type\":\"INSERT\",\"pos\":0,\"size\":0,\"lines\":[\"Hello World\"]}]";
        assertEquals("OK chk_v1", sessionA.handleMessage("PUSH 0 " + init));
        assertEquals(init, registry.lastAppliedJson);

        assertEquals("OK notes.txt chk_v1 " + init, sessionB.handleMessage("JOIN ROOM1"));
        assertEquals("UP_TO_DATE", sessionB.handleMessage("PULL chk_v1"));

        String aliceEdit = "[{\"type\":\"CHANGE\",\"pos\":0,\"size\":1,\"lines\":[\"Alice\"]}]";
        assertEquals("OK chk_v2", sessionA.handleMessage("PUSH chk_v1 " + aliceEdit));
        assertEquals("ERROR 4 OUTDATED_CHECKSUM chk_v2 " + aliceEdit,
                sessionB.handleMessage("PUSH chk_v1 [\"Bob\"]"));
        assertEquals("SYNC chk_v2 " + aliceEdit, sessionB.handleMessage("PULL chk_v1"));
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
