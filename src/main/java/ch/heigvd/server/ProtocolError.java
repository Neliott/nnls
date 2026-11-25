package ch.heigvd.server;

/** NNLSP protocol error codes as defined in PROTOCOL.md. */
public enum ProtocolError {
    UNKNOWN_COMMAND(1),
    INVALID_STATE(2),
    ROOM_NOT_FOUND(3),
    OUTDATED_CHECKSUM(4),
    BAD_ARGUMENTS(5);

    private final int code;

    ProtocolError(int code) {
        this.code = code;
    }

    /** Formats this error without trailing data. */
    public String format() {
        return "ERROR " + code + " " + name();
    }

    /** Formats this error with optional trailing data. */
    public String format(String data) {
        if (data == null || data.trim().isEmpty()) {
            return format();
        }
        return format() + " " + data;
    }
}
