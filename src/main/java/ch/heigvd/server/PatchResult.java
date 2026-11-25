package ch.heigvd.server;

/** Outcome of applying a delta patch to a {@link SyncRoom}. */
public class PatchResult {
    public enum Status {
        OK,
        OUTDATED,
        INVALID
    }

    public final Status status;
    public final String checksum;
    public final String fullStateJson;

    private PatchResult(Status status, String checksum, String fullStateJson) {
        this.status = status;
        this.checksum = checksum;
        this.fullStateJson = fullStateJson;
    }

    /** Creates a successful patch result with the new checksum. */
    public static PatchResult ok(String newChecksum) {
        return new PatchResult(Status.OK, newChecksum, null);
    }

    /** Creates a conflict result with the valid checksum and full JSON state. */
    public static PatchResult outdated(String validChecksum, String fullStateJson) {
        return new PatchResult(Status.OUTDATED, validChecksum, fullStateJson);
    }

    /** Creates an invalid patch result (malformed JSON or out-of-bounds delta). */
    public static PatchResult invalid() {
        return new PatchResult(Status.INVALID, null, null);
    }
}
