package ch.heigvd.server;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/** Thread-safe {@link RoomRegistry} managing active rooms by 5-character alphanumeric codes. */
public class RoomManager implements RoomRegistry {
    private static final int ID_LENGTH = 5;
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private final ConcurrentHashMap<String, Room> rooms;

    /** Creates an empty room manager. */
    public RoomManager() {
        this.rooms = new ConcurrentHashMap<>();
    }

    @Override
    public SyncRoom createRoom(String filename) {
        String id;
        Room room;
        do {
            id = getRandomUniqueID();
            room = new Room(id, filename);
        } while (rooms.putIfAbsent(id, room) != null);
        return room;
    }

    @Override
    public Optional<SyncRoom> getRoom(String roomCode) {
        return Optional.ofNullable(rooms.get(roomCode));
    }

    /** Removes a room by its code, returning {@code true} if it existed. */
    public boolean removeRoom(String roomId) {
        return rooms.remove(roomId) != null;
    }

    private String getRandomUniqueID() {
        String id;
        StringBuilder sb = new StringBuilder(ID_LENGTH);
        do {
            for (int i = 0; i < ID_LENGTH; i++) {
                sb.append(ALPHANUMERIC.charAt(ThreadLocalRandom.current().nextInt(ALPHANUMERIC.length())));
            }
            id = sb.toString();
            sb.setLength(0);
        } while (rooms.containsKey(id));
        return id;
    }
}
