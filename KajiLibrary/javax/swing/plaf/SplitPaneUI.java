package javax.swing.plaf;

import java.awt.Graphics;

import javax.swing.JSplitPane;

/**
 * A {@link JSplitPane}'s look and feel.
 *
 * <h2>The divider belongs to the look and feel</h2>
 *
 * <p>The six methods are about the divider: where it is, how far it can go, and how it is drawn.
 * The pane does not know because the divider is a component the look and feel assembles -- with
 * or without little arrows, of one width or another --, and its limits depend on the minimum
 * sizes of the two sides.
 */
public abstract class SplitPaneUI extends ComponentUI {

    protected SplitPaneUI() {
    }

    /** Puts the divider where both sides have their preferred size. */
    public abstract void resetToPreferredSizes(JSplitPane jc);

    public abstract void setDividerLocation(JSplitPane jc, int location);

    public abstract int getDividerLocation(JSplitPane jc);

    /** The furthest left the divider can go. */
    public abstract int getMinimumDividerLocation(JSplitPane jc);

    public abstract int getMaximumDividerLocation(JSplitPane jc);

    /**
     * It is called after drawing the children.
     *
     * <p>It is the hook for drawing on top of them: the divider's shadow while it is dragged has to
     * be seen over both sides, not underneath.
     */
    public abstract void finishedPaintingChildren(JSplitPane jc, Graphics g);
}
