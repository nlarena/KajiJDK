package java.lang.management;

/**
 * KajiLibrary's java.lang.management.GarbageCollectorMXBean -- a garbage collector.
 *
 * <p>A {@link MemoryManagerMXBean} that also counts collections. There are several per virtual
 * machine: generational collectors have one for the young generation and another for the old, and
 * their numbers read very differently -- many fast young collections is healthy, many old ones is
 * not.
 *
 * <p>Both values are <b>cumulative</b>. For them to say anything the difference between two readings
 * has to be measured: the total time divided by the elapsed time is the fraction of the machine that
 * went into collecting, and that is the figure that matters.
 *
 * <p>Both can return -1 if the virtual machine does not keep the count.
 */
public interface GarbageCollectorMXBean extends MemoryManagerMXBean {

    /** How many collections it did, or -1. */
    long getCollectionCount();

    /** Accumulated milliseconds collecting, or -1. See the class's note. */
    long getCollectionTime();
}
