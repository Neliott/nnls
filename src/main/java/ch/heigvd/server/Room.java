package ch.heigvd.server;

public class Room implements SyncRoom{
    public Room(String id, String filename){

    }

    @Override
    public String getRoomCode() {
        return "";
    }

    @Override
    public RoomState getState() {
        return null;
    }

    @Override
    public PatchResult applyPatch(String prevChecksum, String jsonDeltas) {
        return null;
    }
}
