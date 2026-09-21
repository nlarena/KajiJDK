package com.sun.management;

import java.lang.management.MemoryUsage;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataView;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.TabularData;

/**
 * The data of a concrete garbage collection: when it was and how the memory was left.
 *
 * <h2>The two maps, which are the central datum</h2>
 *
 * <p>{@link #getMemoryUsageBeforeGc} and {@link #getMemoryUsageAfterGc} have one entry per
 * <strong>region</strong> of memory -- eden, survivor, old, metaspace -- and not a total. That
 * is what allows something useful to be said: a collection that emptied the eden and did not
 * move the old region was cheap and healthy; one that reduced the eden and besides made the
 * old one grow has just promoted objects that are going to cost dear afterwards.
 *
 * <p>A total before and after does not tell those two cases apart, and they are opposites.
 *
 * <h2>Why it implements {@link CompositeData}</h2>
 *
 * <p>So as to be able to travel over JMX without the client having this class. A remote monitor
 * receives a generic composite value, with the same items; if it besides has this class in its
 * classpath, it uses {@link #from} and recovers the typed accessors.
 *
 * <p>That is why there is no public constructor: a {@code GcInfo} is produced by the VM on
 * collecting, or is rebuilt from its open form. Making one by hand would be inventing a
 * collection that did not happen.
 *
 * @since 1.5
 */
public class GcInfo implements CompositeData, CompositeDataView {

    private final CompositeData cdata;
    private final long id;
    private final long startTime;
    private final long endTime;
    private final Map<String, MemoryUsage> usageBeforeGc;
    private final Map<String, MemoryUsage> usageAfterGc;

    private GcInfo(final CompositeData cd) {
        this.cdata = cd;
        this.id = ((Long) cd.get("id")).longValue();
        this.startTime = ((Long) cd.get("startTime")).longValue();
        this.endTime = ((Long) cd.get("endTime")).longValue();
        this.usageBeforeGc = readMap(cd, "memoryUsageBeforeGc");
        this.usageAfterGc = readMap(cd, "memoryUsageAfterGc");
    }

    /**
     * A {@code Map<String, MemoryUsage>} comes out of the open form as a table of
     * {@code (key, value)} rows.
     *
     * <p>It is the only way the open type system has of representing a map: there is no
     * "MapType", so it is encoded as a table indexed by the key. Undoing that encoding is all
     * this method does.
     */
    private static Map<String, MemoryUsage> readMap(final CompositeData cd, final String item) {
        if (!cd.containsKey(item)) {
            return Collections.emptyMap();
        }
        final Object value = cd.get(item);
        if (!(value instanceof TabularData)) {
            return Collections.emptyMap();
        }
        final Map<String, MemoryUsage> out = new TreeMap<String, MemoryUsage>();
        for (final Object row : ((TabularData) value).values()) {
            final CompositeData f = (CompositeData) row;
            out.put((String) f.get("key"), MemoryUsage.from((CompositeData) f.get("value")));
        }
        return Collections.unmodifiableMap(out);
    }

    /**
     * This collection's number, within those of its collector.
     *
     * <p>It is a counter per collector, not a global one: number 7 of the young collector and 7 of
     * the old one have nothing to do with each other.
     *
     * @return the number
     */
    public long getId() {
        return id;
    }

    /**
     * When it began, in milliseconds since the VM started.
     *
     * <p>Since the VM's start and not since the epoch: what is wanted to be measured is a duration
     * inside this execution, and a wall clock may jump backwards.
     *
     * @return the milliseconds since the start
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * When it finished, in milliseconds since the VM started.
     *
     * @return the milliseconds since the start
     */
    public long getEndTime() {
        return endTime;
    }

    /**
     * How long it lasted, in milliseconds.
     *
     * <p>It is not necessarily the pause the application suffered: a concurrent collector works
     * while the threads go on running, and there this duration is much greater than the real
     * pause.
     *
     * @return the duration
     */
    public long getDuration() {
        return endTime - startTime;
    }

    /**
     * How each region was before collecting.
     *
     * @return the map, from a region's name to its usage
     */
    public Map<String, MemoryUsage> getMemoryUsageBeforeGc() {
        return usageBeforeGc;
    }

    /**
     * How each region was left after collecting.
     *
     * @return the map, from a region's name to its usage
     */
    public Map<String, MemoryUsage> getMemoryUsageAfterGc() {
        return usageAfterGc;
    }

    /**
     * It rebuilds a {@code GcInfo} from its open form.
     *
     * @param cd the open form, or {@code null}
     * @return the object, or {@code null} if {@code cd} was {@code null}
     * @throws IllegalArgumentException if {@code cd} does not have a {@code GcInfo}'s shape
     */
    public static GcInfo from(final CompositeData cd) {
        if (cd == null) {
            return null;
        }
        if (!cd.containsKey("id") || !cd.containsKey("startTime") || !cd.containsKey("endTime")) {
            throw new IllegalArgumentException(
                    "the CompositeData does not have the shape of a GcInfo");
        }
        return new GcInfo(cd);
    }

    // ---- CompositeData, delegated to the open value it came out of ----
    //
    // To delegate and not to reimplement: the items are those the producer put, and a VM may
    // add its own. Answering from the fields above would make everything this class does not
    // know disappear, which is precisely what a generic monitor would want to see.

    /** {@inheritDoc} */
    public boolean containsKey(final String key) {
        return cdata.containsKey(key);
    }

    /** {@inheritDoc} */
    public boolean containsValue(final Object value) {
        return cdata.containsValue(value);
    }

    /** {@inheritDoc} */
    public Object get(final String key) {
        return cdata.get(key);
    }

    /** {@inheritDoc} */
    public Object[] getAll(final String[] keys) {
        return cdata.getAll(keys);
    }

    /** {@inheritDoc} */
    public CompositeType getCompositeType() {
        return cdata.getCompositeType();
    }

    /** {@inheritDoc} */
    public Collection<?> values() {
        return cdata.values();
    }

    /** {@inheritDoc} */
    public boolean equals(final Object obj) {
        return cdata.equals(obj);
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return cdata.hashCode();
    }

    /** {@inheritDoc} */
    public String toString() {
        return cdata.toString();
    }

    /**
     * This object's open form.
     *
     * <p>It returns the value it came out of, without building it again: it is the one that has
     * all the items, including those this class does not interpret.
     *
     * @param ct the asked-for type, which is ignored for what is said above
     * @return the open form
     */
    public CompositeData toCompositeData(final CompositeType ct) {
        return cdata;
    }
}
