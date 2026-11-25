package ch.heigvd.server;

import java.util.Objects;

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

    private String handleQuit(String args) {
        if (!args.isEmpty()) {
            return ProtocolError.BAD_ARGUMENTS.format();
        }
        currentRoom = null;
        return "BYE";
    }
}
