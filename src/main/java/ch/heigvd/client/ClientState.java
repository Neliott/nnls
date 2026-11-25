package ch.heigvd.client;

import ch.heigvd.common.NetworkDelta;

import java.nio.file.Path;
import java.util.List;

public class ClientState {
    private Path localPath;
    private List<String> syncedLines;
    private String currentChecksum;

    public ClientState(Path path);

    public List<String> readLocalLines(List<String> lines);
    public void writeLocalLines(List<String> lines);
    public List<NetworkDelta> computeLocalDiff();
    public void acceptRemote(List<String> remoteLines, String validChecksum);
    public void keepLocalAndRebase(List<String> remoteLines, String validChecksum);
    public void updateSyncedState(List<String> lines, String checksum);
    public Path getLocalPath();
    public String getCurrentChecksum();
}
