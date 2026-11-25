package ch.heigvd.client;

import ch.heigvd.common.NetworkDelta;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Contract for local file I/O, synced baseline state, and conflict reconciliation. */
public interface LocalState {
    /** Reads all lines from the local file. */
    List<String> readLocalLines() throws IOException;

    /** Writes all lines to the local file. */
    void writeLocalLines(List<String> lines) throws IOException;

    /** Computes deltas between the synced baseline and the local file. */
    List<NetworkDelta> computeLocalDiff() throws IOException;

    /** Overwrites the local file and baseline with the remote state and checksum. */
    void acceptRemote(List<String> remoteLines, String validChecksum) throws IOException;

    /** Keeps local disk edits and rebases the synced baseline to the remote state. */
    void keepLocalAndRebase(List<String> remoteLines, String validChecksum);

    /** Updates the synced lines and checksum after a successful sync. */
    void updateSyncedState(List<String> lines, String checksum);

    /** Returns the synchronized local file path. */
    Path getLocalPath();

    /** Returns the last known server checksum. */
    String getCurrentChecksum();
}
