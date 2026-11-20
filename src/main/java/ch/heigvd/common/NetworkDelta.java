package ch.heigvd.common;

import com.github.difflib.patch.DeltaType;
import java.util.List;

public class NetworkDelta {
    public DeltaType type;
    public int pos;
    public int size;
    public List<String> lines;

    public NetworkDelta(DeltaType type, int pos, int size, List<String> lines){
        this.type = type;
        this.pos = pos;
        this.size = size;
        this.lines = lines;
    }
}
