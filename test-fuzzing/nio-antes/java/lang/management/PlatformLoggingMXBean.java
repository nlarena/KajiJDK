package java.lang.management;

import java.util.List;

/**
 * KajiLibrary's java.lang.management.PlatformLoggingMXBean -- changing log levels from outside.
 *
 * <p>It exists so a logger's detail can be turned up in a program that is already running, without
 * restarting it or touching files. It is what a management console does when it offers a level
 * selector.
 *
 * <p>Everything is handled with <b>strings</b> and not with {@code java.util.logging}'s types, on
 * purpose: that way a remote console can use this MBean without having those classes.
 *
 * <p>There are two absences that mean different things, and confusing them is the typical mistake:
 *
 * <ul>
 *   <li>{@link #getLoggerLevel} returns the <b>empty string</b> if the logger has no level of its own
 *       and inherits it from its parent;
 *   <li>it returns <b>null</b> if no logger with that name exists.
 * </ul>
 *
 * <p>And {@link #setLoggerLevel} with null goes back to inheriting from the parent, which is not the
 * same as switching it off.
 */
public interface PlatformLoggingMXBean extends PlatformManagedObject {

    /** The names of the loggers that exist now. */
    List<String> getLoggerNames();

    /** That logger's level; empty if it inherits it, null if it does not exist. See the class's
     * note. */
    String getLoggerLevel(String loggerName);

    /**
     * It sets its level; null makes it inherit from the parent.
     *
     * @throws IllegalArgumentException if the level is not a known one
     */
    void setLoggerLevel(String loggerName, String levelName);

    /** The parent's name, or the empty string if it is the root; null if it does not exist. */
    String getParentLoggerName(String loggerName);
}
