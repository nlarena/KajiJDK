package javax.swing;

import java.awt.Component;
import java.awt.FocusTraversalPolicy;

/**
 * A focus traversal policy that also knows about internal frames.
 *
 * <h2>What it adds, and why it is needed</h2>
 *
 * <p>{@link FocusTraversalPolicy} knows how to say which is a container's first component. That
 * is enough for a system window, which opens once. An <em>internal</em> frame is activated and
 * deactivated many times, and every time it is activated again the focus has to fall where the
 * user left it -- not on the first field.
 *
 * <p>{@link #getInitialComponent} is that question: "when this frame opens for the first time,
 * where does the focus go". It is different from "which is the first" and from "which is the
 * last that had it", and having it separate is what allows all three to be answered without
 * mixing them up.
 */
public abstract class InternalFrameFocusTraversalPolicy extends FocusTraversalPolicy {

    /** For the subclasses. */
    protected InternalFrameFocusTraversalPolicy() {
    }

    /**
     * Where the focus goes the first time that internal frame opens.
     *
     * <p>By default, the same as {@code getDefaultComponent}. A subclass separates it when the
     * frame has a field it is worth going to from the start.
     */
    public Component getInitialComponent(JInternalFrame frame) {
        return getDefaultComponent(frame);
    }
}
