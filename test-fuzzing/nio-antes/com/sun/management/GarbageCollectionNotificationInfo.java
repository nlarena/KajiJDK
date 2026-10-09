package com.sun.management;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataView;
import javax.management.openmbean.CompositeType;

/**
 * The notice that there was a garbage collection, with what is needed in order to know which
 * and how it came out.
 *
 * <h2>Why a notice and not a query</h2>
 *
 * <p>Because the collections happen when they want to. A monitor that asked periodically for
 * {@link GarbageCollectorMXBean#getLastGcInfo} would miss all those that happened between two
 * queries, which under a high load are almost all of them. Subscribing to the notice is the
 * only way of seeing them all without querying without stopping.
 *
 * <h2>The cause and the action, which is what is read first</h2>
 *
 * <p>{@link #getGcCause} says <strong>why</strong> it started: because the eden filled up,
 * because somebody called {@code System.gc()}, because the metaspace fell short. It is the
 * field that separates a normal collection from a symptom. {@link #getGcAction} says what it
 * did -- whether it was minor or major.
 *
 * <p>A single one of those fields is worth more than the total time: a thousand minor
 * collections because of a full eden are the normal working, and three major ones because of
 * {@code System.gc()} are a problem of the code.
 *
 * <h2>How it arrives</h2>
 *
 * <p>Inside a {@link javax.management.Notification} whose type is
 * {@link #GARBAGE_COLLECTION_NOTIFICATION}. What travels in the user data is a
 * {@link CompositeData}; {@link #from} turns it back into this object on the listener's side.
 *
 * @since 1.7
 */
public class GarbageCollectionNotificationInfo implements CompositeDataView {

    /** The kind of notification that carries one of these. */
    public static final String GARBAGE_COLLECTION_NOTIFICATION =
            "com.sun.management.gc.notification";

    private final String gcName;
    private final String gcAction;
    private final String gcCause;
    private final GcInfo gcInfo;

    /**
     * A notice.
     *
     * @param gcName the collector's name
     * @param gcAction what it did
     * @param gcCause why it started
     * @param gcInfo the collection's data
     * @throws NullPointerException if any of them is {@code null}
     */
    public GarbageCollectionNotificationInfo(final String gcName, final String gcAction,
            final String gcCause, final GcInfo gcInfo) {
        if (gcName == null) {
            throw new NullPointerException("gcName");
        }
        if (gcAction == null) {
            throw new NullPointerException("gcAction");
        }
        if (gcCause == null) {
            throw new NullPointerException("gcCause");
        }
        if (gcInfo == null) {
            throw new NullPointerException("gcInfo");
        }
        this.gcName = gcName;
        this.gcAction = gcAction;
        this.gcCause = gcCause;
        this.gcInfo = gcInfo;
    }

    /**
     * The collector's name, the same its MXBean gives.
     *
     * @return the name
     */
    public String getGcName() {
        return gcName;
    }

    /**
     * What this collection did, in free text.
     *
     * <p>Text and not an enum because each collector describes its phases in its own way, and
     * fixing a closed set would have left all the future collectors out.
     *
     * @return the action
     */
    public String getGcAction() {
        return gcAction;
    }

    /**
     * Why it started, in free text.
     *
     * @return the cause
     */
    public String getGcCause() {
        return gcCause;
    }

    /**
     * The collection's data.
     *
     * @return the data
     */
    public GcInfo getGcInfo() {
        return gcInfo;
    }

    /**
     * It rebuilds the notice from its open form.
     *
     * @param cd the open form, or {@code null}
     * @return the notice, or {@code null} if {@code cd} was {@code null}
     * @throws IllegalArgumentException if {@code cd} does not have this notice's shape
     */
    public static GarbageCollectionNotificationInfo from(final CompositeData cd) {
        if (cd == null) {
            return null;
        }
        if (!cd.containsKey("gcName") || !cd.containsKey("gcAction")
                || !cd.containsKey("gcCause") || !cd.containsKey("gcInfo")) {
            throw new IllegalArgumentException(
                    "the CompositeData does not have the shape of a "
                            + "GarbageCollectionNotificationInfo");
        }
        return new GarbageCollectionNotificationInfo((String) cd.get("gcName"),
                (String) cd.get("gcAction"), (String) cd.get("gcCause"),
                GcInfo.from((CompositeData) cd.get("gcInfo")));
    }

    /**
     * This notice's open form.
     *
     * @param ct the asked-for type
     * @return the open form
     * @throws UnsupportedOperationException if there is no way of building it
     */
    public CompositeData toCompositeData(final CompositeType ct) {
        // Building the open value needs the GcInfo's nested CompositeType to be built, which in
                // its turn needs the TabularType of the two MemoryUsage maps. That is produced by
                // the VM when it emits the notice; on the listener's side it is never needed,
                // because what arrives already comes in open form and the path that is used is
                // `from`.
        throw new UnsupportedOperationException(
                "the open form of this notice is built by the VM that emits it");
    }
}
