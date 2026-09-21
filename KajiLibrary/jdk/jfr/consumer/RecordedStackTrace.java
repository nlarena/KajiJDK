package jdk.jfr.consumer;

import java.util.Collections;
import java.util.List;

import jdk.jfr.ValueDescriptor;

/**
 * A recorded stack of calls.
 *
 * <h2>Why it may be truncated</h2>
 *
 * <p>Because deep stacks cost: walking them takes time and keeping them takes space, and JFR has a
 * configurable cap. {@link #isTruncated} says whether that cap was reached.
 *
 * <p>Ignoring it leads to a classic false conclusion: grouping by the deepest frame and believing
 * that the cost is there, when in truncated stacks that frame is simply where JFR stopped looking.
 *
 * <p>The frames come from the most recent to the oldest, just as in a stack dump.
 *
 * @since 9
 */
public final class RecordedStackTrace extends RecordedObject {

    RecordedStackTrace(List<ValueDescriptor> descriptors, Object[] values) {
        super(descriptors, values);
    }

    /**
     * The frames, from the most recent to the oldest.
     *
     * @return the frames
     */
    public List<RecordedFrame> getFrames() {
        final List<RecordedFrame> v = getValue("frames");
        return v == null ? Collections.<RecordedFrame>emptyList() : v;
    }

    /**
     * Whether the stack was cut short by reaching the cap of depth.
     *
     * @return whether it is truncated
     */
    public boolean isTruncated() {
        return getBoolean("truncated");
    }
}
