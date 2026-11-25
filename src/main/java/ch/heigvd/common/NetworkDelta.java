package ch.heigvd.common;

import com.github.difflib.patch.DeltaType;
import java.util.List;
import java.util.Objects;

/**
 * Structure representing a delta in a text document in a network friendly way
 * <br/>`pos` describes the position at which to apply this delta
 * <br/>`size` describes the number of lines of the original document that are replaced in a CHANGE or the number of lines removed in a DELETE.
 * It DOES NOT represent the number of lines in a modification or the number of lines to insert
 */
public class NetworkDelta {
    public DeltaType type;
    public int pos;
    public int size;
    public List<String> lines;

    public NetworkDelta(){
        type = DeltaType.EQUAL;
        pos = 0;
        size = 0;
        lines = List.of();
    }

    public NetworkDelta(DeltaType type, int pos, int size, List<String> lines){
        this.type = type;
        this.pos = pos;
        this.size = size;
        this.lines = lines;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, pos, size, lines);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
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
        if (lines != null) {
            for (int i = 0; i < lines.size(); ++i) {
                if (lines.get(i) != null) {
                    builder.append("\"").append(lines.get(i)).append("\"");
                }
                if (i < lines.size() - 1) {
                    builder.append(",");
                }
            }
        }
        builder.append("]");
        return builder.toString();
    }
}
