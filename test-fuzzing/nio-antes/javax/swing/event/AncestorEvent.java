package javax.swing.event;

import java.awt.AWTEvent;
import java.awt.Container;

import javax.swing.JComponent;

/**
 * An ancestor of the component was added, removed or moved.
 *
 * <h2>What listening to the ancestors is for</h2>
 *
 * <p>A component does not learn by itself that it stopped being on the screen: what happened was
 * that <em>its grandparent</em> was taken out of the window. Listening only to its own changes
 * leaves the component believing it is still visible.
 *
 * <p>It is what lets something release resources --a timer, a subscription-- when it stops being
 * seen, without having to watch the whole hierarchy by hand.
 *
 * <p>It carries <strong>two</strong> containers: the ancestor that changed and the parent it
 * <em>had</em>. The second is needed because by the time of the notice the link is already
 * broken, and without it there would be no way of knowing where it came from.
 */
public class AncestorEvent extends AWTEvent {

    private static final long serialVersionUID = 1L;

    /** An ancestor was added to the hierarchy or became visible. */
    public static final int ANCESTOR_ADDED = 1;

    /** An ancestor was removed or hidden. */
    public static final int ANCESTOR_REMOVED = 2;

    /** An ancestor moved. */
    public static final int ANCESTOR_MOVED = 3;

    /** The ancestor that changed. */
    Container ancestor;

    /** The parent that ancestor had. */
    Container ancestorParent;

    public AncestorEvent(JComponent source, int id, Container ancestor, Container ancestorParent) {
        super(source, id);
        this.ancestor = ancestor;
        this.ancestorParent = ancestorParent;
    }

    /** The ancestor that changed. */
    public Container getAncestor() {
        return this.ancestor;
    }

    /** The parent that ancestor had; see the class note. */
    public Container getAncestorParent() {
        return this.ancestorParent;
    }

    /** The component that listens, that is the descendant. */
    public JComponent getComponent() {
        return (JComponent) getSource();
    }
}
