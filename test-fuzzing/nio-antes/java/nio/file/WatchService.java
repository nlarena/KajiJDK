package java.nio.file;

import java.io.Closeable;
import java.util.concurrent.TimeUnit;

// KajiLibrary's java.nio.file.WatchService -- a service that watches registered objects for
// changes. KajiJDK creates none (FileSystem.newWatchService rejects the request), so this is inert
// surface.
public interface WatchService extends Closeable {

    // With no `throws IOException`, and that **narrows** what it inherits:
    // `java.io.Closeable.close()` does declare it. Narrowing is legal (JLS 8.4.8.3 only forbids
    // widening) and here it is honest, because closing this service touches nothing that can fail.
    // The JDK declares it the same as `Closeable`; the difference only shows in that a `catch
    // (IOException)` around this `close` is superfluous.
    void close();

    /** Retrieves and removes the next signalled key, or null if none is present. */
    WatchKey poll();

    /** Retrieves and removes the next signalled key, waiting up to the timeout. */
    WatchKey poll(long timeout, TimeUnit unit) throws InterruptedException;

    /** Retrieves and removes the next signalled key, waiting if necessary. */
    WatchKey take() throws InterruptedException;
}
