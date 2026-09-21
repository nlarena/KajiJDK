package com.sun.nio.file;

import java.nio.file.WatchEvent;

/**
 * Registration modifiers of a {@link java.nio.file.WatchService} outside the standard set.
 */
public enum ExtendedWatchEventModifier implements WatchEvent.Modifier {

    /**
     * To watch the whole tree and not only the registered directory.
     *
     * <p>It is a capability of the operating system, not a loop the JDK does on its own: it only
     * works where the system knows how to watch recursively --Windows does it, Linux does not--
     * and where it does not, registering with this throws {@link UnsupportedOperationException}.
     * There each subdirectory has to be registered by hand, with what that implies: those that are
     * created afterwards are not left watched until somebody registers them.
     */
    FILE_TREE
}
