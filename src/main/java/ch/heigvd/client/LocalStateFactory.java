package ch.heigvd.client;

import java.nio.file.Path;

/** Instantiates a {@link LocalState} tracker when entering a room for a given file on disk. */
public interface LocalStateFactory {
    /** Builds a tracker bound to the given file path. */
    LocalState create(Path localPath);
}
