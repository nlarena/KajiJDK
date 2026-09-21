package java.lang.management;

/**
 * KajiLibrary's java.lang.management.MemoryPoolMXBean -- one of the virtual machine's memory pools.
 *
 * <p>Each pool --the young generation, the old one, the method area, the code cache-- has one. It is
 * where the fine-grained numbers live that {@link MemoryMXBean} sums up into two.
 *
 * <h2>The two thresholds</h2>
 *
 * <p>It is the part that gives this MBean its value and the part that gets used wrongly. There are
 * two, and they do not measure the same thing:
 *
 * <ul>
 *   <li>the <b>usage</b> one ({@link #setUsageThreshold}) is crossed when the pool goes over that
 *       size at any moment. In a healthy program it is crossed all the time, because memory rises
 *       between collections;
 *   <li>the <b>usage after collection</b> one ({@link #setCollectionUsageThreshold}) is crossed only
 *       if, after the collector has been through, there is <b>still</b> more than that size in use.
 * </ul>
 *
 * <p>The second is the one for detecting a leak. Setting 0 switches the threshold off.
 *
 * <p>Not every pool supports both; they have to be asked about with
 * {@link #isUsageThresholdSupported} and {@link #isCollectionUsageThresholdSupported} first, because
 * using them without support throws {@link UnsupportedOperationException}.
 *
 * <h2>{@link #getPeakUsage} and {@link #resetPeakUsage}</h2>
 *
 * <p>The peak is since the virtual machine started or since the counter was last reset. Resetting it
 * before an expensive operation and reading it afterwards is the way of measuring how much memory
 * that operation asks for, without earlier peaks contaminating it.
 */
public interface MemoryPoolMXBean extends PlatformManagedObject {

    /** Its name. */
    String getName();

    /** Heap or non-heap. */
    MemoryType getType();

    /** How it stands now, or null if it cannot be known. */
    MemoryUsage getUsage();

    /** The peak reached. See the class's note. */
    MemoryUsage getPeakUsage();

    /** It resets the peak to the current usage. */
    void resetPeakUsage();

    /** Whether it is still valid; it can stop being so. */
    boolean isValid();

    /** Which managers manage it. */
    String[] getMemoryManagerNames();

    /**
     * The usage threshold, in bytes; 0 if it is off.
     *
     * @throws UnsupportedOperationException if this pool does not support it
     */
    long getUsageThreshold();

    /**
     * It sets it; 0 switches it off.
     *
     * @throws IllegalArgumentException if it is negative or greater than the maximum
     * @throws UnsupportedOperationException if this pool does not support it
     */
    void setUsageThreshold(long threshold);

    /**
     * Whether it was crossed.
     *
     * @throws UnsupportedOperationException if this pool does not support it
     */
    boolean isUsageThresholdExceeded();

    /**
     * How many times it was crossed.
     *
     * @throws UnsupportedOperationException if this pool does not support it
     */
    long getUsageThresholdCount();

    /** Whether this pool supports the usage threshold. */
    boolean isUsageThresholdSupported();

    /**
     * The usage-after-collection threshold. See the class's note.
     *
     * @throws UnsupportedOperationException if this pool does not support it
     */
    long getCollectionUsageThreshold();

    /**
     * It sets it; 0 switches it off.
     *
     * @throws IllegalArgumentException if it is negative or greater than the maximum
     * @throws UnsupportedOperationException if this pool does not support it
     */
    void setCollectionUsageThreshold(long threshold);

    /**
     * Whether it was crossed.
     *
     * @throws UnsupportedOperationException if this pool does not support it
     */
    boolean isCollectionUsageThresholdExceeded();

    /**
     * How many times it was crossed.
     *
     * @throws UnsupportedOperationException if this pool does not support it
     */
    long getCollectionUsageThresholdCount();

    /** How it stood after the last collection, or null if there never was one. */
    MemoryUsage getCollectionUsage();

    /** Whether this pool supports the usage-after-collection threshold. */
    boolean isCollectionUsageThresholdSupported();
}
