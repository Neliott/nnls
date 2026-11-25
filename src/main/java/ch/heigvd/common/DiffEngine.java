package ch.heigvd.common;

import com.github.difflib.patch.*;

import com.github.difflib.DiffUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.security.MessageDigest;

public class DiffEngine {
    private static final Gson GSON = new Gson();
    private static final Type DELTA_LIST_TYPE = new TypeToken<List<NetworkDelta>>() {}.getType();

    public static String computeSha256(List<String> lines){
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < lines.size(); ++i){
                builder.append(lines.get(i));
                if (i != lines.size()-1){
                    builder.append("\n");
                }
            }
            byte[] shaData = digest.digest(builder.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(shaData);
        }catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<NetworkDelta> computeDeltas(List<String> original, List<String> revised){
        if (original == null) throw new RuntimeException("Original cannot be null");
        if (revised == null) throw new RuntimeException("Revised cannot be null");

        Patch<String> patch = DiffUtils.diff(original, revised);
        List<AbstractDelta<String>> deltas = patch.getDeltas();
        List<NetworkDelta> networkDeltas = new ArrayList<>();

        //extract the target to make a minimized delta
        for (AbstractDelta<String> delta : deltas){
            Chunk<String> chunk = delta.getTarget();
            int chunkSize = switch (delta.getType()){
                case INSERT -> 0;
                case DELETE, CHANGE -> delta.getSource().size();
                default -> throw new RuntimeException("Trying to compute a patch with a delta of unsupported type.");
            };
            networkDeltas.add(new NetworkDelta(
                            delta.getType(),
                            chunk.getPosition(),
                            chunkSize,
                            chunk.getLines())
            );
        }

        return networkDeltas;
    }

    public static List<String> applyDeltas(List<String> original, List<NetworkDelta> deltas) {
        if (original == null) throw new RuntimeException("Original text cannot be null");
        if (deltas == null) throw new RuntimeException("Deltas cannot be null");

        //using DiffUtils here would mean having to reconstruct the source chunk,
        // it is easier, thus, to remake the logic
        ArrayList<String> revised = new ArrayList<String>(original);

        for (NetworkDelta delta : deltas) {
            if (delta == null) {
                throw new RuntimeException("Delta cannot be null");
            }
            if (delta.pos < 0 || delta.size < 0) {
                throw new RuntimeException("Delta position and size must be non-negative: pos=" + delta.pos + ", size=" + delta.size);
            }

            switch (delta.type) {
                case CHANGE -> {
                    if (delta.lines == null) {
                        throw new RuntimeException("CHANGE delta lines cannot be null");
                    }
                    if (delta.pos + delta.size > revised.size()) {
                        throw new RuntimeException("CHANGE delta out of bounds: pos=" + delta.pos + ", size=" + delta.size + ", currentSize=" + revised.size());
                    }
                    revised.subList(delta.pos, delta.pos + delta.size).clear();
                    revised.addAll(delta.pos, delta.lines);
                }
                case DELETE -> {
                    if (delta.pos + delta.size > revised.size()) {
                        throw new RuntimeException("DELETE delta out of bounds: pos=" + delta.pos + ", size=" + delta.size + ", currentSize=" + revised.size());
                    }
                    revised.subList(delta.pos, delta.pos + delta.size).clear();
                }
                case INSERT -> {
                    if (delta.lines == null) {
                        throw new RuntimeException("INSERT delta lines cannot be null");
                    }
                    if (delta.pos > revised.size()) {
                        throw new RuntimeException("INSERT delta out of bounds: pos=" + delta.pos + ", currentSize=" + revised.size());
                    }
                    revised.addAll(delta.pos, delta.lines);
                }
                default -> throw new RuntimeException("Trying to apply a patch with a delta of unsupported type.");
            }
        }

        return revised;
    }

    public static String serializeDeltas(List<NetworkDelta> deltas){
        // https://javadoc.io/static/com.google.code.gson/gson/2.8.5/com/google/gson/Gson.html
        return GSON.toJson(deltas);
    }

    public static List<NetworkDelta> deserializeDeltas(String json){
        // https://javadoc.io/static/com.google.code.gson/gson/2.8.5/com/google/gson/Gson.html
        return GSON.fromJson(json, DELTA_LIST_TYPE);
    }
}
