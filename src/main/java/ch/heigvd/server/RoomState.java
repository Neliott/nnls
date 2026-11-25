package ch.heigvd.server;

/** Immutable snapshot of a room's document state. */
public class RoomState {
    public final String filename;
    public final String checksum;
    public final String fullStateJson;

    /** Creates a room state snapshot. */
    public RoomState(String filename, String checksum, String fullStateJson) {
        this.filename = filename;
        this.checksum = checksum;
        this.fullStateJson = fullStateJson;
    }
}
