package java.lang.management;

/**
 * KajiLibrary's java.lang.management.ClassLoadingMXBean -- how many classes were loaded.
 *
 * <p>Three counters, and the relation between them is what makes the MBean useful:
 * {@link #getLoadedClassCount} is the ones loaded <b>now</b>, and equals the total loaded minus the
 * unloaded.
 *
 * <p>The unloaded growing is normal --a loader being freed takes its classes with it--; the currently
 * loaded growing without stopping in a stable program is the signature of a class loader leak, which
 * is the hardest leak to find by hand.
 *
 * <p>{@link #setVerbose} switches on the same tracking as the {@code -verbose:class} option, and it
 * can be turned on and off while running.
 */
public interface ClassLoadingMXBean extends PlatformManagedObject {

    /** How many were loaded since the virtual machine started. */
    long getTotalLoadedClassCount();

    /** How many are loaded now. See the class's note. */
    int getLoadedClassCount();

    /** How many were unloaded. */
    long getUnloadedClassCount();

    /** Whether it is tracking class loading. */
    boolean isVerbose();

    /** It switches tracking on or off. */
    void setVerbose(boolean value);
}
