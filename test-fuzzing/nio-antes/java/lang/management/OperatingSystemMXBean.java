package java.lang.management;

/**
 * KajiLibrary's java.lang.management.OperatingSystemMXBean -- the system the virtual machine runs on.
 *
 * <p>Five data. The first four are the usual system properties;
 * {@link #getSystemLoadAverage} is the only one that measures anything.
 *
 * <p>That one returns the <b>last minute</b>'s load average, or a <b>negative</b> if the platform
 * does not publish it -- which is Windows's case. It is a number relative to the processor count: it
 * has to be divided by {@link #getAvailableProcessors} to tell whether the machine is saturated.
 *
 * <p>{@link #getAvailableProcessors} can <b>change</b> between calls: in a container with a quota, or
 * on a virtual machine that resizes, it is not constant.
 */
public interface OperatingSystemMXBean extends PlatformManagedObject {

    /** The operating system's name. */
    String getName();

    /** The architecture. */
    String getArch();

    /** The version. */
    String getVersion();

    /** How many processors the virtual machine sees. See the class's note: it can change. */
    int getAvailableProcessors();

    /** The last minute's load, or a negative if it is not published. See the class's note. */
    double getSystemLoadAverage();
}
