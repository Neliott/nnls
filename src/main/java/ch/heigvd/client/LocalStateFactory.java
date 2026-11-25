package ch.heigvd.client;

import java.nio.file.Path;

/** Factory creating {@link LocalState} instances for a target local file path. */
public interface LocalStateFactory {
    /** Creates a local state manager bound to the given file path. */
    LocalState create(Path localPath);
}
