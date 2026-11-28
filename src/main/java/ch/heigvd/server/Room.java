package ch.heigvd.server;

import ch.heigvd.common.DiffEngine;
import ch.heigvd.common.NetworkDelta;
import com.github.difflib.patch.DeltaType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Thread-safe collaborative document room implementing {@link SyncRoom}. */
public class Room implements SyncRoom {
    private final String roomCode;
    private final String filename;
    private List<String> lines;
    private String checksum;

    /** Creates an empty room with initial checksum {@code "0"}. */
    public Room(String roomCode, String filename) {
        this.roomCode = Objects.requireNonNull(roomCode, "roomCode must not be null");
        this.filename = Objects.requireNonNull(filename, "filename must not be null");
        this.lines = new ArrayList<>();
        this.checksum = "0";
    }

    @Override
    public String getRoomCode() {
        return roomCode;
    }

    /** Returns the document filename associated with this room. */
    public String getFilename() {
        return filename;
    }

    /** Returns the current SHA-256 checksum (or {@code "0"} when empty). */
    public synchronized String getChecksum() {
        return checksum;
    }

    @Override
    public synchronized RoomState getState() {
        return new RoomState(filename, checksum, serializeFullState());
    }

    @Override
    public synchronized PatchResult applyPatch(String prevChecksum, String jsonDeltas) {
        if (!checksum.equals(prevChecksum)) {
            return PatchResult.outdated(checksum, serializeFullState());
        }

        List<NetworkDelta> deltas;
        try {
            deltas = DiffEngine.deserializeDeltas(jsonDeltas);
        } catch (RuntimeException e) {
            return PatchResult.invalid();
        }
        if (deltas == null) {
            return PatchResult.invalid();
        }

        List<String> updated;
        try {
            updated = DiffEngine.applyDeltas(lines, deltas);
        } catch (RuntimeException e) {
            return PatchResult.invalid();
        }

        this.lines = updated;
        this.checksum = DiffEngine.computeSha256(updated);
        return PatchResult.ok(this.checksum);
    }

    private String serializeFullState() {
        if (lines.isEmpty()) {
            return "[]";
        }
        return DiffEngine.serializeDeltas(
                List.of(new NetworkDelta(DeltaType.INSERT, 0, 0, lines))
        );
    }
}
