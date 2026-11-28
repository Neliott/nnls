package ch.heigvd.server;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

/** Unit tests for {@link RoomManager}. */
class RoomManagerTest {

    @Test
    void createsLooksUpAndRemovesRoom() {
        RoomManager manager = new RoomManager();
        SyncRoom room = manager.createRoom("notes.txt");

        assertTrue(room.getRoomCode().matches("[A-Z0-9]{5}"));
        assertSame(room, manager.getRoom(room.getRoomCode()).orElse(null));
        assertTrue(manager.getRoom("NOPE1").isEmpty());

        assertTrue(manager.removeRoom(room.getRoomCode()));
        assertTrue(manager.getRoom(room.getRoomCode()).isEmpty());
        assertFalse(manager.removeRoom(room.getRoomCode()));
    }

    @Test
    void concurrentCreateRoomGeneratesUniqueCodes() throws InterruptedException {
        RoomManager manager = new RoomManager();
        int threads = 8;
        int perThread = 25;
        Set<String> codes = ConcurrentHashMap.newKeySet();

        Callable<Void> task = () -> {
            for (int i = 0; i < perThread; i++) {
                codes.add(manager.createRoom("file.txt").getRoomCode());
            }
            return null;
        };

        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            pool.invokeAll(Collections.nCopies(threads, task));
        }

        assertEquals(threads * perThread, codes.size());
    }
}
