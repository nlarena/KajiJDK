package java.lang.module;

import java.io.Closeable;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.stream.Stream;

// KajiLibrary's java.lang.module.ModuleReader -- reads the contents of a module reference. KajiJDK
// opens no modules, so no reader is ever produced; this is surface only.
public interface ModuleReader extends Closeable {

    /** Finds a resource in the module, returning its URI. */
    Optional<URI> find(String name) throws java.io.IOException;

    /** Opens a resource as a stream. */
    default Optional<InputStream> open(String name) throws java.io.IOException {
        return Optional.empty();
    }

    /** Reads a resource into a byte buffer. */
    default Optional<ByteBuffer> read(String name) throws java.io.IOException {
        return Optional.empty();
    }

    /** Releases a byte buffer obtained from {@link #read}. */
    default void release(ByteBuffer bb) {
    }

    /** Lists the resource names in the module. */
    Stream<String> list() throws java.io.IOException;

    // No `throws IOException`: KajiLibrary's Closeable.close() declares none.
    void close();
}
