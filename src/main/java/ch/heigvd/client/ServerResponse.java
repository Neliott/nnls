package ch.heigvd.client;

/** Parsed representation of a single-line NNLSP server response. */
public class ServerResponse {
    public enum Kind { OK, UP_TO_DATE, SYNC, BYE, ERROR }

    private final Kind kind;
    private final String payload;
    private final int errorCode;
    private final String errorReason;
    private final String errorData;

    private ServerResponse(Kind kind) {
        this(kind, "");
    }

    private ServerResponse(Kind kind, String payload) {
        this(kind, payload, -1, null, "");
    }

    private ServerResponse(
            Kind kind, String payload, int errorCode, String errorReason, String errorData) {
        this.kind = kind;
        this.payload = payload;
        this.errorCode = errorCode;
        this.errorReason = errorReason;
        this.errorData = errorData;
    }

    /** Splits and validates a raw single-line NNLSP reply into its kind, payload, and optional error fields. */
    public static ServerResponse parse(String rawLine) {
        if (rawLine == null || rawLine.trim().isEmpty()) {
            throw new IllegalArgumentException("Empty server response");
        }
        String line = rawLine.trim();
        int space = line.indexOf(' ');
        String keyword = (space < 0) ? line : line.substring(0, space);
        String rest = (space < 0) ? "" : line.substring(space + 1).trim();

        switch (keyword) {
            case "OK":
                return new ServerResponse(Kind.OK, rest);
            case "UP_TO_DATE":
                if (!rest.isEmpty()) {
                    throw new IllegalArgumentException("Unexpected payload for UP_TO_DATE");
                }
                return new ServerResponse(Kind.UP_TO_DATE);
            case "SYNC":
                if (rest.isEmpty()) {
                    throw new IllegalArgumentException("Missing payload for SYNC");
                }
                return new ServerResponse(Kind.SYNC, rest);
            case "BYE":
                if (!rest.isEmpty()) {
                    throw new IllegalArgumentException("Unexpected payload for BYE");
                }
                return new ServerResponse(Kind.BYE);
            case "ERROR":
                return parseError(rest);
            default:
                throw new IllegalArgumentException("Unknown server response: " + keyword);
        }
    }

    private static ServerResponse parseError(String rest) {
        String[] parts = rest.split(" ", 3);
        if (parts.length < 2 || parts[1].trim().isEmpty()) {
            throw new IllegalArgumentException("Malformed ERROR response: " + rest);
        }
        int code;
        try {
            code = Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid error code: " + parts[0], e);
        }
        String data = (parts.length == 3) ? parts[2].trim() : "";
        return new ServerResponse(Kind.ERROR, rest, code, parts[1].trim(), data);
    }

    /** Returns the parsed response kind. */
    public Kind getKind() { return kind; }

    /** Returns the response payload after the keyword, or "" if none. */
    public String getPayload() { return payload; }

    /** Returns the numeric NNLSP error code, or -1 if this response is not an error. */
    public int getErrorCode() { return errorCode; }

    /** Returns the error reason token, or null if this response is not an error. */
    public String getErrorReason() { return errorReason; }

    /** Returns optional trailing error data (e.g., conflict state), or "" if absent or not an error. */
    public String getErrorData() { return errorData; }

    /** Translates a server ERROR code into a human-readable message for the terminal user. */
    public String formatErrorMessage() {
        switch (errorCode) {
            case 1: return "Server rejected request: unknown command.";
            case 2: return "Action not allowed in current session state.";
            case 3: return "Room not found on the server.";
            case 4: return "Conflict detected: remote document has changed.";
            case 5: return "Server rejected request: invalid arguments or malformed diff.";
            default: return "Server error " + errorCode + " (" + errorReason + ").";
        }
    }
}
