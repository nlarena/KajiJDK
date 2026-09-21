package javax.swing.plaf.basic;

import java.awt.Container;
import java.awt.Dimension;

import javax.swing.BoxLayout;
import javax.swing.JPopupMenu;
import javax.swing.plaf.UIResource;

/**
 * The layout of a menu bar and of a popup menu.
 *
 * <p>It is a {@link BoxLayout} with two small and necessary additions.
 *
 * <h2>An empty menu takes up nothing</h2>
 *
 * <p>A {@link JPopupMenu} with no items measures zero by zero, and not whatever its margins
 * measure. Without that, a context menu that has nothing to show would appear all the same as a
 * little rectangle a few pixels across. It is measured: an empty menu bar measures 0 x 2 -- its
 * margins -- and an empty popup menu measures 0 x 0.
 *
 * <h2>The accelerators' column</h2>
 *
 * <p>Before measuring, the menu forgets the largest accelerator width it had computed. That
 * number is what lines up every item's {@code Ctrl-O} in a column, and it has to be recomputed
 * each time: if an item changes its accelerator and the number were left stale, the column comes
 * out crooked or the menu wider than it needs to be.
 *
 * <p>It is a {@link UIResource} so that installing another look and feel replaces it; a layout
 * the program set is respected.
 */
public class DefaultMenuLayout extends BoxLayout implements UIResource {

    /** Where the menu keeps the largest accelerator width; see the class note. */
    static final String MAX_ACCELERATOR_WIDTH = "maxAccWidth";

    public DefaultMenuLayout(Container target, int axis) {
        super(target, axis);
    }

    /** See the class note. */
    public Dimension preferredLayoutSize(Container target) {
        if (target instanceof JPopupMenu) {
            JPopupMenu popupMenu = (JPopupMenu) target;
            popupMenu.putClientProperty(MAX_ACCELERATOR_WIDTH, null);
            if (popupMenu.getComponentCount() == 0) {
                return new Dimension(0, 0);
            }
        }
        // The BoxLayout keeps the children's sizes; they have to be made to be forgotten so that
                // it asks again.
        super.invalidateLayout(target);
        return super.preferredLayoutSize(target);
    }
}
