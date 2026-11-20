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

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;

        if (!(obj instanceof NetworkDelta delta)) return false;

        if (this.size != delta.size){
            return false;
        }
        if (this.pos != delta.pos){
            return false;
        }
        if (this.type != delta.type){
            return false;
        }

        return this.lines.equals(delta.lines);
    }

    @Override
    public String toString(){
        StringBuilder builder = new StringBuilder();
        builder.append("Type: ").append(type).append(", Pos: ").append(pos).append(", Size: ").append(size).append(", [");
        for (int i = 0; i < lines.size(); ++i){
            builder.append("\"").append(lines.get(i)).append("\"");
            if (i < lines.size()-1){
                builder.append(",");
            }
        }
        builder.append("]");
        return builder.toString();
    }
}
