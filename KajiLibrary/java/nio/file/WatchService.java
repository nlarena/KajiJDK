package java.nio.file;

import java.io.Closeable;
import java.util.concurrent.TimeUnit;

// KajiLibrary's java.nio.file.WatchService -- a service that watches registered objects for changes.
// KajiJDK creates none (FileSystem.newWatchService rejects the request), so this is inert surface.
public interface WatchService extends Closeable {

    // No `throws IOException` here: KajiLibrary's Closeable.close() declares none, and an override
    // may not widen the throws clause.
    void close();

    /** Retrieves and removes the next signalled key, or null if none is present. */
    WatchKey poll();

    /** Retrieves and removes the next signalled key, waiting up to the timeout. */
    WatchKey poll(long timeout, TimeUnit unit) throws InterruptedException;

    /** Retrieves and removes the next signalled key, waiting if necessary. */
    WatchKey take() throws InterruptedException;
}
