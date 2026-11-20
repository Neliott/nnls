package ch.heigvd.common;

import com.github.difflib.patch.*;

import com.github.difflib.DiffUtils;
import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.security.MessageDigest;

public class DiffEngine {
    public static String computeSha256(List<String> lines){
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] shaData = digest.digest(lines.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(shaData);
        }catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<NetworkDelta> computeDeltas(List<String> original, List<String> revised){
        Patch<String> patch = DiffUtils.diff(original, revised);
        List<AbstractDelta<String>> deltas = patch.getDeltas();
        List<NetworkDelta> networkDeltas = new ArrayList<>();

        //extract the target to make a minimized delta
        for (AbstractDelta<String> delta : deltas){
            Chunk<String> chunk = deltas.getFirst().getTarget();
            networkDeltas.add(new NetworkDelta(delta.getType(), chunk.getPosition(), chunk.size(), chunk.getLines()));
        }

        return networkDeltas;
    }

    public static List<String> applyDeltas(List<String> original, List<NetworkDelta> deltas) throws PatchFailedException {
        //reconstruct the patch
        Patch<String> patch = new Patch<>(deltas.size());
        Chunk<String> emptyChunk = new Chunk<String>(0, List.of());

        for (NetworkDelta delta : deltas){
            patch.addDelta(switch (delta.type){
                case CHANGE -> new ChangeDelta<>(
                        new Chunk<>(
                                delta.pos,
                                List.copyOf(original.subList(delta.pos, delta.pos + delta.size))),
                        new Chunk<>(delta.pos, delta.lines));
                case DELETE -> new DeleteDelta<>(
                        new Chunk<>(
                                delta.pos,
                                List.copyOf(original.subList(delta.pos, delta.pos + delta.size))),
                        new Chunk<>(delta.pos, delta.lines));
                case INSERT -> new InsertDelta<>(
                        new Chunk<>(
                                delta.pos,
                                List.of()),
                        new Chunk<>(delta.pos, delta.lines));
                default -> throw new RuntimeException("Trying to apply a patch with a delta of unsupported type.");
            });
        }

        return DiffUtils.patch(original, patch);
    }

    public static String serializeDeltas(List<NetworkDelta> deltas){
        // https://javadoc.io/static/com.google.code.gson/gson/2.8.5/com/google/gson/Gson.html
        return "serializeDeltas not implemented";
    }

    public static List<NetworkDelta> deserializeDeltas(String json){
        // https://javadoc.io/static/com.google.code.gson/gson/2.8.5/com/google/gson/Gson.html
        return List.of(new NetworkDelta(DeltaType.EQUAL,0,0,List.of("deserializeDeltas not implemented")));
    }
}
