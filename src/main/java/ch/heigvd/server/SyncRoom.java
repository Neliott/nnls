package ch.heigvd.server;

/** Thread-safe room contract used by {@link ServerSession}. */
public interface SyncRoom {
    /** Returns the unique 5-character room identifier. */
    String getRoomCode();

    /** Atomically captures the room's current filename, checksum, and full JSON state. */
    RoomState getState();

    /** Atomically verifies {@code prevChecksum} and applies {@code jsonDeltas} if current. */
    PatchResult applyPatch(String prevChecksum, String jsonDeltas);
}
