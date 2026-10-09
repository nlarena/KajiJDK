package com.sun.source.util;

/**
 * It learns about each phase of the compilation.
 *
 * <p>The two methods have an empty body on purpose: almost nobody needs both, and forcing an
 * empty one to be written contributes nothing. It is the same criterion as an event adapter's,
 * resolved with {@code default} instead of with an extra class.
 */
public interface TaskListener {

    /** A phase starts. */
    default void started(TaskEvent e) {
    }

    /** A phase finishes. */
    default void finished(TaskEvent e) {
    }
}
