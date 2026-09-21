package com.sun.management;

/**
 * The operating system, with what the standard interface does not dare to promise.
 *
 * <h2>Why there is an extended version</h2>
 *
 * <p>Because {@link java.lang.management.OperatingSystemMXBean} only declares what exists on
 * <strong>every</strong> system where a JVM runs: the name, the architecture, the number of
 * processors. The physical memory, the swap and the process's CPU time are not there because
 * they cannot be measured everywhere.
 *
 * <p>This interface adds them. The price is that it is no longer portable: whoever uses it has
 * to check with {@code instanceof} that the platform's bean should be of this type.
 *
 * <h2>The pairs of methods that seem repeated</h2>
 *
 * <p>{@link #getFreePhysicalMemorySize} and {@link #getFreeMemorySize} return the same, and the
 * same happens with the other two pairs. The first of each pair is the old name, which was left
 * as a {@code default} delegating to the new one; the second is the one that has to be
 * implemented.
 *
 * <p>The renaming was not cosmetic: inside a container, "physical memory" is a lie -- what the
 * process may use is the container's limit, not what the machine has. The new names say
 * "memory" plainly precisely so as not to promise where it comes from.
 *
 * @since 1.5
 */
public interface OperatingSystemMXBean extends java.lang.management.OperatingSystemMXBean {

    /**
     * The virtual memory the process has reserved, in bytes.
     *
     * @return the bytes, or {@code -1} if it cannot be measured
     */
    long getCommittedVirtualMemorySize();

    /**
     * The total size of the swap area, in bytes.
     *
     * @return the bytes
     */
    long getTotalSwapSpaceSize();

    /**
     * How much is left free of the swap area, in bytes.
     *
     * @return the bytes
     */
    long getFreeSwapSpaceSize();

    /**
     * The CPU time consumed by the process, in nanoseconds.
     *
     * <p>The precision may be much worse than a nanosecond; the unit only fixes the scale.
     *
     * @return the nanoseconds, or {@code -1} if it cannot be measured
     */
    long getProcessCpuTime();

    /**
     * The free memory, in bytes.
     *
     * @return the bytes
     * @deprecated The name promises the machine's physical memory, which inside a container is not
     *     what the process may use. Use {@link #getFreeMemorySize}.
     */
    @Deprecated(since = "14")
    default long getFreePhysicalMemorySize() {
        return getFreeMemorySize();
    }

    /**
     * The free memory, in bytes.
     *
     * @return the bytes
     */
    long getFreeMemorySize();

    /**
     * The total memory, in bytes.
     *
     * @return the bytes
     * @deprecated For the same reason as {@link #getFreePhysicalMemorySize}. Use
     *     {@link #getTotalMemorySize}.
     */
    @Deprecated(since = "14")
    default long getTotalPhysicalMemorySize() {
        return getTotalMemorySize();
    }

    /**
     * The total memory, in bytes.
     *
     * @return the bytes
     */
    long getTotalMemorySize();

    /**
     * The CPU load of the whole system, between 0.0 and 1.0.
     *
     * @return the load, or a negative value if it could not be measured
     * @deprecated Use {@link #getCpuLoad}, which is the same value with a name that does not
     *     suggest that it should be different from the process's load by saying "system".
     */
    @Deprecated(since = "14")
    default double getSystemCpuLoad() {
        return getCpuLoad();
    }

    /**
     * The CPU load of the whole system, between 0.0 and 1.0.
     *
     * <p>The value is an average over the interval since the previous query. The first query has
     * no interval to average over and that is why it returns a negative value.
     *
     * @return the load, or a negative value if it could not be measured
     */
    double getCpuLoad();

    /**
     * The CPU load this process causes, between 0.0 and 1.0.
     *
     * <p>It is a fraction of <strong>all</strong> the available CPU: on a machine of eight cores,
     * a process that saturates one core gives around 0.125 and not 1.0.
     *
     * @return the load, or a negative value if it could not be measured
     */
    double getProcessCpuLoad();
}
