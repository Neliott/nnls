package ch.heigvd.server;

import java.util.Optional;

/** Registry contract for creating and looking up collaborative rooms. */
public interface RoomRegistry {
    /** Creates and registers a new room for the given filename. */
    SyncRoom createRoom(String filename);

    /** Looks up an existing room by its unique code. */
    Optional<SyncRoom> getRoom(String roomCode);
}
