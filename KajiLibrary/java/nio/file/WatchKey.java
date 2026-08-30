package java.nio.file;

import java.util.List;

// KajiLibrary's java.nio.file.WatchKey -- the token returned when a Watchable is registered with a
// WatchService. Inert surface in KajiJDK (there is no watch service to produce one).
public interface WatchKey {

    /** Whether this key is valid. */
    boolean isValid();

    /** Retrieves and removes the pending events for this key. */
    List<WatchEvent<?>> pollEvents();

    /** Resets this key, making it eligible to be requeued. */
    boolean reset();

    /** Cancels the registration with the watch service. */
    void cancel();

    /** The object for which this key was created. */
    Watchable watchable();
}
