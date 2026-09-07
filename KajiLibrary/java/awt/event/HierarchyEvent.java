package java.awt.event;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Container;

/**
 * The tree of components above a component changed.
 *
 * <p>It serves to hear about things that do not happen in the component but **above** it: that it
 * was added to a window, that an ancestor was hidden, that the window containing it moved. A
 * component cannot see that by looking at itself.
 *
 * <p>The flags say what changed, and `SHOWING_CHANGED` is the most useful: it means the component
 * came to be seen or stopped being seen **for real**, counting that all its ancestors are visible. It
 * is the right signal for starting and stopping an animation.
 */
public class HierarchyEvent extends AWTEvent {

    private static final long serialVersionUID = -5337576970038043990L;

    /** An ancestor changed place. */
    public static final int ANCESTOR_MOVED = 1401;

    /** An ancestor changed size. */
    public static final int ANCESTOR_RESIZED = 1402;

    /** Whether the component can be displayed changed. */
    public static final int DISPLAYABILITY_CHANGED = 2;

    /** The tree changed. */
    public static final int HIERARCHY_CHANGED = 1400;

    /** The family's first identifier. */
    public static final int HIERARCHY_FIRST = 1400;

    /** The family's last identifier. */
    public static final int HIERARCHY_LAST = 1402;

    /** The component changed parent. */
    public static final int PARENT_CHANGED = 1;

    /** Whether the component is seen for real changed. */
    public static final int SHOWING_CHANGED = 4;

    private final Component changed;
    private final Container changedParent;
    private final long changeFlags;

    /**
     * With no flags, for the ancestor events.
     *
     * @throws IllegalArgumentException if the source is `null`
     */
    public HierarchyEvent(Component source, int id, Component changed, Container changedParent) {
        this(source, id, changed, changedParent, 0);
    }

    /**
     * With the flags of what changed.
     *
     * @throws IllegalArgumentException if the source is `null`
     */
    public HierarchyEvent(Component source, int id, Component changed, Container changedParent,
            long changeFlags) {
        super(source, id);
        this.changed = changed;
        this.changedParent = changedParent;
        this.changeFlags = changeFlags;
    }

    /** The component that receives the notice. */
    public Component getComponent() {
        if (this.source instanceof Component) {
            return (Component) this.source;
        }
        return null;
    }

    /** The component above that actually changed. */
    public Component getChanged() {
        return this.changed;
    }

    /** That component's parent, before or after the change depending on what happened. */
    public Container getChangedParent() {
        return this.changedParent;
    }

    /** What changed, as a combination of flags. */
    public long getChangeFlags() {
        return this.changeFlags;
    }

    public String paramString() {
        String type;
        if (this.id == HIERARCHY_CHANGED) {
            type = "HIERARCHY_CHANGED";
        } else if (this.id == ANCESTOR_MOVED) {
            type = "ANCESTOR_MOVED";
        } else if (this.id == ANCESTOR_RESIZED) {
            type = "ANCESTOR_RESIZED";
        } else {
            type = "unknown type";
        }
        return type + " (" + this.changed + "," + this.changedParent + "),changeFlags="
                + this.changeFlags;
    }
}
