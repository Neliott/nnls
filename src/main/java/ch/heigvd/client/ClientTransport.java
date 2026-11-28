package ch.heigvd.client;

import java.io.IOException;

/** Contract for client-side request-response transport to the NNLSP server. */
public interface ClientTransport extends AutoCloseable {
    /** Opens the connection to the server. */
    void connect() throws IOException;

    /** Sends one request line and returns the response line, or null on EOF. */
    String sendRequest(String request) throws IOException;

    /** Returns true if the connection is currently open. */
    boolean isConnected();

    /** Closes the connection and releases resources. */
    @Override
    void close() throws IOException;
}
