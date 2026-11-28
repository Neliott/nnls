package ch.heigvd.server;

import ch.heigvd.common.DiffEngine;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

/** Unit and concurrency tests for {@link Room}. */
class RoomTest {

    @Test
    void initialStateIsEmptyWithZeroChecksum() {
        Room room = new Room("AB12C", "notes.txt");
        RoomState state = room.getState();

        assertEquals("AB12C", room.getRoomCode());
        assertEquals("notes.txt", state.filename);
        assertEquals("0", state.checksum);
        assertEquals("[]", state.fullStateJson);
    }

    @Test
    void appliesValidPatchAndRejectsOutdatedOrInvalid() {
        Room room = new Room("AB12C", "notes.txt");
        String initPatch = "[{\"type\":\"INSERT\",\"pos\":0,\"size\":0,\"lines\":[\"hello\",\"world\"]}]";

        PatchResult ok = room.applyPatch("0", initPatch);
        assertEquals(PatchResult.Status.OK, ok.status);
        String expectedHash = DiffEngine.computeSha256(List.of("hello", "world"));
        assertEquals(expectedHash, ok.checksum);
        assertEquals(initPatch, room.getState().fullStateJson);

        PatchResult outdated = room.applyPatch("0", "[{\"type\":\"DELETE\",\"pos\":0,\"size\":1,\"lines\":[]}]");
        assertEquals(PatchResult.Status.OUTDATED, outdated.status);
        assertEquals(expectedHash, outdated.checksum);
        assertEquals(initPatch, outdated.fullStateJson);

        assertEquals(PatchResult.Status.INVALID, room.applyPatch(expectedHash, "[{bad").status);
        assertEquals(PatchResult.Status.INVALID, room.applyPatch(expectedHash, "[{\"type\":\"DELETE\",\"pos\":5,\"size\":1,\"lines\":[]}]").status);
        assertEquals(expectedHash, room.getChecksum());
    }

    @Test
    void concurrentPatchesAllowOnlyOneWinner() throws Exception {
        Room room = new Room("AB12C", "notes.txt");
        Callable<PatchResult> taskA = () -> room.applyPatch("0", "[{\"type\":\"INSERT\",\"pos\":0,\"size\":0,\"lines\":[\"A\"]}]");
        Callable<PatchResult> taskB = () -> room.applyPatch("0", "[{\"type\":\"INSERT\",\"pos\":0,\"size\":0,\"lines\":[\"B\"]}]");

        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            List<Future<PatchResult>> results = pool.invokeAll(List.of(taskA, taskB));
            PatchResult r1 = results.get(0).get();
            PatchResult r2 = results.get(1).get();

            boolean oneOkOneOutdated = (r1.status == PatchResult.Status.OK && r2.status == PatchResult.Status.OUTDATED)
                    || (r2.status == PatchResult.Status.OK && r1.status == PatchResult.Status.OUTDATED);
            assertTrue(oneOkOneOutdated, "Expected one OK and one OUTDATED, got: " + r1.status + ", " + r2.status);
        }
    }
}
