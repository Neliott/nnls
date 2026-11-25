package ch.heigvd.client;

import ch.heigvd.common.DiffEngine;
import ch.heigvd.common.NetworkDelta;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Class managing the client's file, used to read and write in the file,
 * as well as get the checksum and manage the synchronization
 */
public class ClientState {
    private final Path localPath;
    private List<String> syncedLines;
    private String syncedChecksum;

    public ClientState(Path path){
        this.localPath = path;
        syncedLines = List.of();
        syncedChecksum = null;
    }

    public List<String> readLocalLines(){
        try(FileReader reader = new FileReader(localPath.toFile(), StandardCharsets.UTF_8);
            BufferedReader in = new BufferedReader(reader)) {
            ArrayList<String> list = new ArrayList<>();
            String r;
            while((r = in.readLine()) != null){
                list.add(r);
            }
            return list;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeLocalLines(List<String> lines){
        try(FileWriter writer = new FileWriter(localPath.toFile(), StandardCharsets.UTF_8);
            BufferedWriter in = new BufferedWriter(writer)) {
            for (String line : lines){
                in.write(line+System.lineSeparator());
            }
            in.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public List<NetworkDelta> computeLocalDiff(){
        return DiffEngine.computeDeltas(syncedLines, readLocalLines());
    }

    public void acceptRemote(List<String> remoteLines, String validChecksum){
        writeLocalLines(remoteLines);
        syncedLines = new ArrayList<>(remoteLines);
        syncedChecksum = validChecksum;
    }

    public void keepLocalAndRebase(List<String> remoteLines, String validChecksum){
        updateSyncedState(remoteLines, validChecksum);
    }

    public void updateSyncedState(List<String> lines, String checksum){
        syncedLines = new ArrayList<>(lines);
        syncedChecksum = checksum;
    }

    public Path getLocalPath(){
        return localPath;
    }

    public String getSyncedChecksum(){
        return syncedChecksum;
    }

    public String getCurrentChecksum(){
        return DiffEngine.computeSha256(readLocalLines());
    }
}
