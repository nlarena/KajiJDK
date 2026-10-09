package jdk.jfr.consumer;

import java.util.List;

import jdk.jfr.ValueDescriptor;

/**
 * A group of threads, as it was recorded.
 *
 * <p>{@link #getParent} puts together the chain up to the root group. It serves for grouping
 * threads by their origin --a pool, a container-- when the names of the threads do not say it.
 *
 * @since 9
 */
public final class RecordedThreadGroup extends RecordedObject {

    RecordedThreadGroup(List<ValueDescriptor> descriptors, Object[] values) {
        super(descriptors, values);
    }

    /**
     * The name of the group.
     *
     * @return the name
     */
    public String getName() {
        return getString("name");
    }

    /**
     * The group that contains it.
     *
     * @return the parent group, or {@code null} if this one is the root
     */
    public RecordedThreadGroup getParent() {
        return getValue("parent");
    }
}
