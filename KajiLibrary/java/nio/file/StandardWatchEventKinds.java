package java.nio.file;

// The four kinds of event the spec defines for the watch service.
//
// They are compared by **identity**, not by name: they are unique constants, and a foreign
// `WatchEvent.Kind` called `"ENTRY_CREATE"` is not this one. That is why the private implementation
// defines no `equals`.
//
// KajiJDK has no watch service --`FileSystem.newWatchService()` throws
// `UnsupportedOperationException`-- so these constants never arrive in an event. They exist because
// the code that registers a directory names them and has to compile.
public final class StandardWatchEventKinds {

    // Constants only: there is nothing to instantiate.
    private StandardWatchEventKinds() {
    }

    // The only implementation of `Kind`. Private on purpose: nobody should be able to make an event
    // kind that passes itself off as these.
    private static class StandardKind<T> implements WatchEvent.Kind<T> {

        private final String name;
        private final Class<T> kind;

        StandardKind(String name, Class<T> kind) {
            this.name = name;
            this.kind = kind;
        }

        public String name() {
            return this.name;
        }

        public Class<T> type() {
            return this.kind;
        }

        public String toString() {
            return this.name;
        }
    }

    /**
     * Events were lost.
     *
     * <p>Its context is `Object` and not `Path` because there is no path to report: what it says is
     * that the queue overflowed and there are changes that will not be seen.
     */
    public static final WatchEvent.Kind<Object> OVERFLOW =
            new StandardKind<Object>("OVERFLOW", Object.class);

    /** An entry was created in the watched directory. */
    public static final WatchEvent.Kind<Path> ENTRY_CREATE =
            new StandardKind<Path>("ENTRY_CREATE", Path.class);

    /** An entry was deleted. */
    public static final WatchEvent.Kind<Path> ENTRY_DELETE =
            new StandardKind<Path>("ENTRY_DELETE", Path.class);

    /** An entry was modified. */
    public static final WatchEvent.Kind<Path> ENTRY_MODIFY =
            new StandardKind<Path>("ENTRY_MODIFY", Path.class);
}
