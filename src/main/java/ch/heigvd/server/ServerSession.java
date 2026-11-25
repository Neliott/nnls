package ch.heigvd.server;

import java.util.Objects;
import java.util.Optional;

/**
 * Pure, socket-independent NNLSP state machine for a single client session.
 */
public class ServerSession {
    private final RoomRegistry roomRegistry;
    private SyncRoom currentRoom;

    /** Creates a session backed by the given room registry. */
    public ServerSession(RoomRegistry roomRegistry) {
        this.roomRegistry = Objects.requireNonNull(roomRegistry, "roomRegistry must not be null");
    }

    /** Handles a single client request line and returns the response, or null for blank lines. */
    public String handleMessage(String requestLine) {
        if (requestLine == null || requestLine.trim().isEmpty()) {
            return null;
        }
        String line = requestLine.trim();
        int space = line.indexOf(' ');
        String command = (space < 0) ? line : line.substring(0, space);
        String args = (space < 0) ? "" : line.substring(space + 1).trim();

        switch (command) {
            case "CREATE":
                return handleCreate(args);
            case "JOIN":
                return handleJoin(args);
            case "PULL":
                return handlePull(args);
            case "LEAVE":
                return handleLeave(args);
            case "QUIT":
                return handleQuit(args);
            default:
                return ProtocolError.UNKNOWN_COMMAND.format();
        }
    }

    /** Returns true if this session is currently in a room. */
    public boolean isInRoom() {
        return currentRoom != null;
    }

    /** Releases the active room reference on disconnect. */
    public void close() {
        currentRoom = null;
    }

    private String handleCreate(String args) {
        if (currentRoom != null) {
            return ProtocolError.INVALID_STATE.format();
        }
        if (!isSingleArg(args)) {
            return ProtocolError.BAD_ARGUMENTS.format();
        }
        currentRoom = roomRegistry.createRoom(args);
        return "OK " + currentRoom.getRoomCode();
    }

    private String handleJoin(String args) {
        if (currentRoom != null) {
            return ProtocolError.INVALID_STATE.format();
        }
        if (!isSingleArg(args)) {
            return ProtocolError.BAD_ARGUMENTS.format();
        }
        Optional<SyncRoom> room = roomRegistry.getRoom(args);
        if (!room.isPresent()) {
            return ProtocolError.ROOM_NOT_FOUND.format();
        }
        currentRoom = room.get();
        RoomState state = currentRoom.getState();
        return "OK " + state.filename + " " + state.checksum + " " + state.fullStateJson;
    }

    private String handlePull(String args) {
        if (currentRoom == null) {
            return ProtocolError.INVALID_STATE.format();
        }
        if (!isSingleArg(args)) {
            return ProtocolError.BAD_ARGUMENTS.format();
        }
        RoomState state = currentRoom.getState();
        return state.checksum.equals(args) ? "UP_TO_DATE" : "SYNC " + state.checksum + " " + state.fullStateJson;
    }

    private String handleLeave(String args) {
        if (currentRoom == null) {
            return ProtocolError.INVALID_STATE.format();
        }
        if (!args.isEmpty()) {
            return ProtocolError.BAD_ARGUMENTS.format();
        }
        currentRoom = null;
        return "OK";
    }

    private String handleQuit(String args) {
        if (!args.isEmpty()) {
            return ProtocolError.BAD_ARGUMENTS.format();
        }
        currentRoom = null;
        return "BYE";
    }

    private static boolean isSingleArg(String args) {
        return !args.isEmpty() && !args.contains(" ") && !args.contains("\t");
    }
}
