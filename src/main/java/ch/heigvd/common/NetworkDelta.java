package ch.heigvd.common;

import com.github.difflib.patch.DeltaType;
import java.util.List;

public class NetworkDelta {
    public DeltaType type;
    public int pos;
    public int size;
    public List<String> lines;
}
