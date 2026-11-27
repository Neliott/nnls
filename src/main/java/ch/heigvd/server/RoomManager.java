package ch.heigvd.server;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class RoomManager implements RoomRegistry {
    private static final int ID_LENGTH = 5;
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private ConcurrentHashMap <String, Room> rooms;

    public RoomManager(){
        this.rooms = new ConcurrentHashMap<>();
    }

    public SyncRoom createRoom(String filename){
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

    public void removeRoom(String roomId){
        rooms.remove(roomId);
    }

    private String getRandomUniqueID(){
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
