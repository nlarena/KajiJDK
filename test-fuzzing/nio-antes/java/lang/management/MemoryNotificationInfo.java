package java.lang.management;

import javax.management.openmbean.CompositeData;

/**
 * KajiLibrary's java.lang.management.MemoryNotificationInfo -- a memory pool crossed a threshold.
 *
 * <p>It is what travels as the {@code userData} of the notification the memory MBean emits. It is
 * not sent as it stands: it is sent as a {@link CompositeData} and rebuilt on this side with
 * {@link #from}, which is why the class exists.
 *
 * <h2>The two thresholds, which are not the same</h2>
 *
 * <ul>
 *   <li>{@link #MEMORY_THRESHOLD_EXCEEDED} the <b>usage</b> threshold was exceeded: there is more
 *       occupied than was set, at this moment;
 *   <li>{@link #MEMORY_COLLECTION_THRESHOLD_EXCEEDED} the <b>usage after collection</b> threshold was
 *       exceeded: more than was set remained occupied <i>once the collector had been through</i>.
 * </ul>
 *
 * <p>The second is the one that matters for detecting a leak. The first fires all the time in a
 * healthy program, because memory rises before each collection; the second only fires if something
 * genuinely cannot be freed.
 *
 * <p>{@link #getCount} says how many times that threshold was crossed since it was set, not how many
 * notifications there were: the virtual machine does not send one per crossing.
 */
public class MemoryNotificationInfo {

    /** The usage threshold was exceeded. See the class's note. */
    public static final String MEMORY_THRESHOLD_EXCEEDED =
        "java.management.memory.threshold.exceeded";

    /** The usage-after-collection threshold was exceeded. See the class's note. */
    public static final String MEMORY_COLLECTION_THRESHOLD_EXCEEDED =
        "java.management.memory.collection.threshold.exceeded";

    /** Which pool. */
    private final String poolName;

    /** How it stood when it was crossed. */
    private final MemoryUsage usage;

    /** How many times it was crossed. */
    private final long count;

    /**
     * @throws NullPointerException if the name or the usage is null
     */
    public MemoryNotificationInfo(String poolName, MemoryUsage usage, long count) {
        if (poolName == null) {
            throw new NullPointerException("Null poolName");
        }
        if (usage == null) {
            throw new NullPointerException("Null usage");
        }
        this.poolName = poolName;
        this.usage = usage;
        this.count = count;
    }

    /** Which memory pool. */
    public String getPoolName() {
        return this.poolName;
    }

    /** How it stood at the moment of the crossing. */
    public MemoryUsage getUsage() {
        return this.usage;
    }

    /** How many times the threshold was crossed. See the class's note. */
    public long getCount() {
        return this.count;
    }

    /**
     * The same, read out of a {@link CompositeData}. See the class's note.
     *
     * @return the object, or null if the datum is null
     * @throws IllegalArgumentException if the datum does not describe a
     *     {@code MemoryNotificationInfo}
     */
    public static MemoryNotificationInfo from(CompositeData cd) {
        if (cd == null) {
            return null;
        }
        final String type = "MemoryNotificationInfo";
        Object u = CompositeItems.optional(cd, "usage");
        if (!(u instanceof CompositeData)) {
            throw new IllegalArgumentException(
                "Unexpected composite type for " + type + ": item usage is not a CompositeData");
        }
        return new MemoryNotificationInfo(CompositeItems.string(cd, "poolName", type),
                                          MemoryUsage.from((CompositeData) u),
                                          CompositeItems.longValue(cd, "count", type));
    }
}
