package javax.swing.plaf;

import java.awt.Rectangle;

import javax.swing.JTabbedPane;

/**
 * A {@link JTabbedPane}'s look and feel.
 *
 * <h2>Three questions about the tabs</h2>
 *
 * <p>Which tab falls on a point, where a tab is, and how many rows they ended up in. The pane
 * cannot answer them: a tab's size depends on the typeface and on the decoration the look and
 * feel draws, and how many rows are left depends on that and on the width.
 */
public abstract class TabbedPaneUI extends ComponentUI {

    protected TabbedPaneUI() {
    }

    /** Which tab falls on that point, or -1. */
    public abstract int tabForCoordinate(JTabbedPane pane, int x, int y);

    /** That tab's rectangle. */
    public abstract Rectangle getTabBounds(JTabbedPane pane, int index);

    /** How many rows the tabs ended up in. */
    public abstract int getTabRunCount(JTabbedPane pane);
}
