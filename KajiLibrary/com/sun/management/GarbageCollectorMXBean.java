package com.sun.management;

/**
 * A garbage collector, with the addition of being able to look at the <strong>last</strong>
 * collection.
 *
 * <p>The standard interface {@link java.lang.management.GarbageCollectorMXBean} only gives
 * totals: how many collections there were and how much time they added up to. It serves for a
 * trend and does not serve for diagnosing, because an average hides precisely the pause that is
 * of interest.
 *
 * <p>{@link #getLastGcInfo} is what is missing: of the last collection it gives when it began,
 * when it finished and how each region of memory was left before and after. With that it may be
 * said whether a concrete pause recovered something or was in vain.
 *
 * @since 1.5
 */
public interface GarbageCollectorMXBean extends java.lang.management.GarbageCollectorMXBean {

    /**
     * The data of this collector's last collection.
     *
     * @return the data, or {@code null} if there has not been one yet
     */
    GcInfo getLastGcInfo();
}
