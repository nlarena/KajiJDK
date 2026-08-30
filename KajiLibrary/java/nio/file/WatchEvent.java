package java.nio.file;

// KajiLibrary's java.nio.file.WatchEvent -- an event on a watched object (a create/modify/delete,
// or an overflow). KajiJDK has no file system to watch, so this type is inert surface: it exists so
// Watchable/WatchKey/Path can name it.
public interface WatchEvent<T> {

    /** A kind of watch event, identified by name and context type. */
    interface Kind<T> {
        String name();

        Class<T> type();
    }

    /** A qualifier on how a watch is registered. */
    interface Modifier {
        String name();
    }

    /** The kind of this event. */
    Kind<T> kind();

    /** How many times this event was observed (coalesced). */
    int count();

    /** The context (for a path event, the affected path relative to the watched directory). */
    T context();
}
