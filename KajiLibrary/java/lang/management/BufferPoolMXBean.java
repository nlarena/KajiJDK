package java.lang.management;

/**
 * KajiLibrary's java.lang.management.BufferPoolMXBean -- {@code java.nio}'s buffers.
 *
 * <p>There are two: {@code "direct"} and {@code "mapped"}. They are memory that is <b>not on the
 * heap</b>, so it does not show up in {@link MemoryMXBean#getHeapMemoryUsage} and the collector does
 * not see it directly.
 *
 * <p>That makes them the cause of a whole class of problems: a program exhausting the machine's
 * memory while the heap looks calm is nearly always leaking direct buffers. They are freed when the
 * Java object referencing them is collected, and that can take arbitrarily long.
 *
 * <p>{@link #getTotalCapacity} is what the buffers say they have; {@link #getMemoryUsed} is what the
 * operating system genuinely reserved, and it can be -1 if it is not known. The two together say
 * whether there is fragmentation.
 */
public interface BufferPoolMXBean extends PlatformManagedObject {

    /** {@code "direct"} o {@code "mapped"}. */
    String getName();

    /** How many buffers there are in the pool. */
    long getCount();

    /** How much they say they take up, in bytes. */
    long getTotalCapacity();

    /** How much they genuinely take up, or -1. See the class's note. */
    long getMemoryUsed();
}
