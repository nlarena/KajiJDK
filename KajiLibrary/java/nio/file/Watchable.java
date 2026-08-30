package java.nio.file;

import java.io.IOException;

// KajiLibrary's java.nio.file.Watchable -- an object (typically a Path) that can be registered with
// a WatchService. KajiJDK has no watch service, so implementors reject registration.
public interface Watchable {

    /** Registers this object with a watch service for the given event kinds and modifiers. */
    WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events,
            WatchEvent.Modifier... modifiers) throws IOException;

    /** Registers this object with a watch service for the given event kinds. */
    WatchKey register(WatchService watcher, WatchEvent.Kind<?>... events) throws IOException;
}
