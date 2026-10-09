package java.lang.management;

/**
 * KajiLibrary's java.lang.management.MemoryManagerMXBean -- whoever manages a memory pool.
 *
 * <p>A manager handles one or more pools; a pool can have more than one manager. The relation is
 * many to many, which is why {@link #getMemoryPoolNames} returns an array and not a name.
 *
 * <p>{@link #isValid} can go from true to false: the virtual machine can retire a manager while
 * running, and from then on the MBean still exists but reports nothing more. It has to be asked
 * before believing the other methods.
 *
 * <p>{@link GarbageCollectorMXBean} is the subinterface for the managers that also collect.
 */
public interface MemoryManagerMXBean extends PlatformManagedObject {

    /** Its name. */
    String getName();

    /** Whether it is still valid. See the class's note. */
    boolean isValid();

    /** Which pools it manages. */
    String[] getMemoryPoolNames();
}
