package java.nio.file;

import java.io.Closeable;
import java.util.concurrent.TimeUnit;

// KajiLibrary's java.nio.file.WatchService -- a service that watches registered objects for changes.
// KajiJDK creates none (FileSystem.newWatchService rejects the request), so this is inert surface.
public interface WatchService extends Closeable {

    // Sin `throws IOException`, y eso **estrecha** lo que hereda: `java.io.Closeable.close()` si lo
    // declara. Estrechar es legal (JLS 8.4.8.3 solo prohibe ensanchar) y aca es honesto, porque
    // cerrar este servicio no toca nada que pueda fallar. El JDK lo declara igual que `Closeable`;
    // la diferencia solo se nota en que un `catch (IOException)` alrededor de este `close` sobra.
    void close();

    /** Retrieves and removes the next signalled key, or null if none is present. */
    WatchKey poll();

    /** Retrieves and removes the next signalled key, waiting up to the timeout. */
    WatchKey poll(long timeout, TimeUnit unit) throws InterruptedException;

    /** Retrieves and removes the next signalled key, waiting if necessary. */
    WatchKey take() throws InterruptedException;
}
