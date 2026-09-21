package java.lang.management;

import javax.management.openmbean.CompositeData;

/**
 * KajiLibrary's java.lang.management.MemoryUsage -- how much memory there is and how much is used.
 *
 * <p>Four numbers, and the difference between them is the thing to understand:
 *
 * <ul>
 *   <li>{@link #getInit} what was asked for at start-up;
 *   <li>{@link #getUsed} what is occupied now;
 *   <li>{@link #getCommitted} what the operating system has <b>genuinely reserved</b> for the
 *       virtual machine. Always greater than or equal to what is used, and it can go down if the
 *       virtual machine gives memory back;
 *   <li>{@link #getMax} the ceiling, if there is one.
 * </ul>
 *
 * <p>The one that gets misread is {@code committed}. A program watching {@code used/max} to tell
 * whether it is near the limit is in for surprises: what matters for performance is how much is left
 * before {@code committed} has to grow.
 *
 * <p>{@code init} and {@code max} may be -1, which means "undefined". The other two may not.
 *
 * <p>It is immutable: a snapshot of the moment it was asked for, not a live view.
 */
public class MemoryUsage {

    /** What was asked for at start-up, or -1. */
    private final long init;

    /** What is occupied now. */
    private final long used;

    /** What is reserved from the operating system. */
    private final long committed;

    /** The ceiling, or -1. */
    private final long max;

    /**
     * @throws IllegalArgumentException if some value is negative without being the -1 that is allowed
     *     there, if what is used exceeds what is reserved, or if what is reserved exceeds the ceiling
     */
    public MemoryUsage(long init, long used, long committed, long max) {
        if (init < -1) {
            throw new IllegalArgumentException(
                "init parameter = " + init + " is negative but not -1.");
        }
        if (max < -1) {
            throw new IllegalArgumentException(
                "max parameter = " + max + " is negative but not -1.");
        }
        if (used < 0) {
            throw new IllegalArgumentException("used parameter = " + used + " is negative.");
        }
        if (committed < 0) {
            throw new IllegalArgumentException(
                "committed parameter = " + committed + " is negative.");
        }
        if (used > committed) {
            throw new IllegalArgumentException(
                "used = " + used + " should be <= committed = " + committed);
        }
        if (max >= 0 && committed > max) {
            throw new IllegalArgumentException(
                "committed = " + committed + " should be < max = " + max);
        }
        this.init = init;
        this.used = used;
        this.committed = committed;
        this.max = max;
    }

    /** What was asked for at start-up, or -1. */
    public long getInit() {
        return this.init;
    }

    /** What is occupied now. */
    public long getUsed() {
        return this.used;
    }

    /** What is reserved from the operating system. See the class's note. */
    public long getCommitted() {
        return this.committed;
    }

    /** The ceiling, or -1. */
    public long getMax() {
        return this.max;
    }

    /** The four numbers, each in bytes and in kilobytes. */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("init = ").append(this.init).append('(').append(this.init >> 10).append("K) ");
        buf.append("used = ").append(this.used).append('(').append(this.used >> 10).append("K) ");
        buf.append("committed = ").append(this.committed).append('(')
            .append(this.committed >> 10).append("K) ");
        buf.append("max = ").append(this.max).append('(').append(this.max >> 10).append("K)");
        return buf.toString();
    }

    /**
     * The same, read out of a {@link CompositeData}.
     *
     * <p>It is how it arrives from a remote virtual machine: the open datum travels over the network
     * and the object is rebuilt on this side.
     *
     * @return the object, or null if the datum is null
     * @throws IllegalArgumentException if the datum does not describe a {@code MemoryUsage}
     */
    public static MemoryUsage from(CompositeData cd) {
        if (cd == null) {
            return null;
        }
        final String type = "MemoryUsage";
        return new MemoryUsage(CompositeItems.longValue(cd, "init", type),
                               CompositeItems.longValue(cd, "used", type),
                               CompositeItems.longValue(cd, "committed", type),
                               CompositeItems.longValue(cd, "max", type));
    }
}
