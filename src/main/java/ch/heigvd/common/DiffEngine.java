package ch.heigvd.common;

import jdk.jshell.spi.ExecutionControl;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.Patch;
import com.google.gson.Gson;

import java.util.List;

public class DiffEngine {
    public static String computeSha256(List<String> lines){
        throw new ExecutionControl.NotImplementedException("computeSha256 not implemented");
    }

    public static List<NetworkDelta> computeDeltas(List<String> original, List<String> revised){
        // https://java-diff-utils.github.io/java-diff-utils/4.10/docs/apidocs/
        throw new ExecutionControl.NotImplementedException("computeDeltas not implemented");
    }

    public static List<String> applyDeltas(List<String> original, List<NetworkDelta> deltas){
        // https://java-diff-utils.github.io/java-diff-utils/4.10/docs/apidocs/
        throw new ExecutionControl.NotImplementedException("applyDeltas not implemented");
    }

    public static String serializeDeltas(List<NetworkDelta> deltas){
        // https://javadoc.io/static/com.google.code.gson/gson/2.8.5/com/google/gson/Gson.html
        throw new ExecutionControl.NotImplementedException("serializeDeltas not implemented");
    }

    public static List<NetworkDelta> deserializeDeltas(String json){
        // https://javadoc.io/static/com.google.code.gson/gson/2.8.5/com/google/gson/Gson.html
        throw new ExecutionControl.NotImplementedException("deserializeDeltas not implemented");
    }
}
