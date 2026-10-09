package javax.swing.plaf;

import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JList;

/**
 * A {@link JList}'s look and feel.
 *
 * <h2>Three questions of geometry</h2>
 *
 * <p>Everything the list cannot answer on its own because it depends on how it is drawn: which
 * row falls on a point, where a row starts, and how much several take up. The list forwards them
 * to the look and feel and does not compute them, because a row's height is decided by whoever
 * draws and the arrangement in columns is decided by the look and feel.
 */
public abstract class ListUI extends ComponentUI {

    protected ListUI() {
    }

    /** Which row falls on that point, or -1. */
    public abstract int locationToIndex(JList<?> list, Point location);

    /** That row's top left corner, or null. */
    public abstract Point indexToLocation(JList<?> list, int index);

    /** The rectangle the rows between those two indices take up. */
    public abstract Rectangle getCellBounds(JList<?> list, int index1, int index2);
}
