package java.lang.management;

import javax.management.openmbean.CompositeData;

/**
 * KajiLibrary's java.lang.management.MonitorInfo -- a monitor a thread holds, and where it took it.
 *
 * <p>It adds to {@link LockInfo} the two things only monitors --{@code synchronized}'s locks-- have:
 * which stack frame was entered, and which frame that is.
 *
 * <p>That information is what makes a dump useful: knowing that a thread holds a lock is not enough,
 * one has to know <b>from where</b> in order to find the block that never ends.
 *
 * <p>{@code java.util.concurrent}'s locks do not have this and so come out as plain
 * {@code LockInfo}: they are taken with a method call and not with a block, so there is no frame
 * "containing" them.
 *
 * <h2>The depth can be -1</h2>
 *
 * <p>It means the virtual machine knows the monitor is held but not in which frame. There the frame
 * is null, and that is the only combination allowed with null: if the depth is 0 or more, the frame
 * has to be there.
 */
public class MonitorInfo extends LockInfo {

    /** Which frame it was taken in, or -1. */
    private final int stackDepth;

    /** Which frame that is, or null. */
    private final StackTraceElement stackFrame;

    /**
     * @param stackDepth the frame's index, or -1 if it is not known
     * @param stackFrame that frame; it has to be null if and only if the depth is negative
     * @throws NullPointerException if the class name is null
     * @throws IllegalArgumentException if the depth and the frame do not agree
     */
    public MonitorInfo(String className, int identityHashCode, int stackDepth,
                       StackTraceElement stackFrame) {
        super(className, identityHashCode);
        if (stackDepth >= 0 && stackFrame == null) {
            throw new IllegalArgumentException("Parameter stackDepth is " + stackDepth
                + " but stackFrame is null");
        }
        if (stackDepth < 0 && stackFrame != null) {
            throw new IllegalArgumentException("Parameter stackDepth is " + stackDepth
                + " but stackFrame is not null");
        }
        this.stackDepth = stackDepth;
        this.stackFrame = stackFrame;
    }

    /** Which frame it was taken in, or -1. See the class's note. */
    public int getLockedStackDepth() {
        return this.stackDepth;
    }

    /** Which frame that is, or null. */
    public StackTraceElement getLockedStackFrame() {
        return this.stackFrame;
    }

    /**
     * The same, read out of a {@link CompositeData}.
     *
     * @return the object, or null if the datum is null
     * @throws IllegalArgumentException if the datum does not describe a {@code MonitorInfo}
     */
    public static MonitorInfo from(CompositeData cd) {
        if (cd == null) {
            return null;
        }
        final String type = "MonitorInfo";
        Object frame = CompositeItems.optional(cd, "lockedStackFrame");
        StackTraceElement element = null;
        if (frame instanceof CompositeData) {
            element = StackTraceElements.from((CompositeData) frame);
        }
        return new MonitorInfo(CompositeItems.string(cd, "className", type),
                               CompositeItems.integer(cd, "identityHashCode", type),
                               CompositeItems.integer(cd, "lockedStackDepth", type),
                               element);
    }
}
