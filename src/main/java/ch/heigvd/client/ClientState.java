package ch.heigvd.client;

import ch.heigvd.common.DiffEngine;
import ch.heigvd.common.NetworkDelta;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Class managing the client's file, used to read and write in the file,
 * as well as get the checksum and manage the synchronization
 */
public class ClientState implements LocalState {
    private final Path localPath;
    private List<String> syncedLines;
    private String syncedChecksum;

    public ClientState(Path path){
        Objects.requireNonNull(path, "path must not be null");
        this.localPath = path;
        syncedLines = List.of();
        syncedChecksum = "0";
    }

    public List<String> readLocalLines() throws IOException {
        try(FileReader reader = new FileReader(localPath.toFile(), StandardCharsets.UTF_8);
            BufferedReader in = new BufferedReader(reader)) {
            ArrayList<String> list = new ArrayList<>();
            String r;
            while((r = in.readLine()) != null){
                list.add(r);
            }
            return list;
        }
    }

    public void writeLocalLines(List<String> lines) throws IOException {
        Objects.requireNonNull(lines, "lines must not be null");
        try(FileWriter writer = new FileWriter(localPath.toFile(), StandardCharsets.UTF_8);
            BufferedWriter out = new BufferedWriter(writer)) {
            for (String line : lines){
                Objects.requireNonNull(line, "line must not be null");
                out.write(line+System.lineSeparator());
            }
            out.flush();
        }
    }
    public List<NetworkDelta> computeLocalDiff() throws IOException {
        return DiffEngine.computeDeltas(syncedLines, readLocalLines());
    }

    public void acceptRemote(List<String> remoteLines, String validChecksum) throws IOException {
        writeLocalLines(remoteLines);
        updateSyncedState(remoteLines, validChecksum);
    }

    public void keepLocalAndRebase(List<String> remoteLines, String validChecksum){
        updateSyncedState(remoteLines, validChecksum);
    }

    public void updateSyncedState(List<String> lines, String checksum){
        Objects.requireNonNull(lines, "lines must not be null");
        Objects.requireNonNull(checksum, "checksum must not be null");
        syncedLines = new ArrayList<>(lines);
        syncedChecksum = checksum;
    }

    public Path getLocalPath(){
        return localPath;
    }

    public String getSyncedChecksum(){
        return syncedChecksum;
    }

    public String getCurrentChecksum() throws IOException {
        return DiffEngine.computeSha256(readLocalLines());
    }
}
