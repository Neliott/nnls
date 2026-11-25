package ch.heigvd.client;

import ch.heigvd.common.NetworkDelta;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Contract for tracking a file on disk against the last version confirmed by the server. */
public interface LocalState {
    /** Loads the current text lines from the tracked file on disk. */
    List<String> readLocalLines() throws IOException;

    /** Replaces the contents of the tracked file on disk with the given lines. */
    void writeLocalLines(List<String> lines) throws IOException;

    /** Compares the server-confirmed reference snapshot against the file on disk to produce line patches. */
    List<NetworkDelta> computeLocalDiff() throws IOException;

    /** Discards unpushed edits by writing the server's version to disk and resetting the reference snapshot. */
    void acceptRemote(List<String> remoteLines, String validChecksum) throws IOException;

    /** Preserves the file on disk while moving the reference snapshot to the server's version so the next push diffs against it. */
    void keepLocalAndRebase(List<String> remoteLines, String validChecksum);

    /** Records the given lines and hash in memory as the new server-confirmed reference snapshot without modifying disk. */
    void updateSyncedState(List<String> lines, String checksum);

    /** Returns the path of the tracked file on disk. */
    Path getLocalPath();

    /** Returns the SHA-256 hash of the current reference snapshot (or "0" before initial upload). */
    String getCurrentChecksum();
}
