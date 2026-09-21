package javax.swing.plaf.multi;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Vector;
import javax.accessibility.Accessible;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.TreeUI;
import javax.swing.tree.TreePath;

/**
 * The Tree look and feel that shares out every call among several.
 *
 * <h2>What it is</h2>
 *
 * <p>It keeps a list of Tree looks and feels and passes every operation to them all. What it
 * returns is what the first one answered, which is the main look and feel's; the rest
 * hear about it all the same, and that is the point.
 *
 * <h2>What having several is for</h2>
 *
 * <p>For hanging on a look and feel another one that does not draw: a screen reader, a logger of
 * what the user does, a help system that follows the focus. Those observers need the same calls as
 * the real look and feel --to install themselves, to hear about every drawing-- and have no reason
 * to know that there is another.
 *
 * <p>Without this mechanism each look and feel would have to be wrapped by hand. With it, they are
 * listed in a property and {@link MultiLookAndFeel} builds the list.
 *
 * <h2>Why the first one rules</h2>
 *
 * <p>A method returns a single value and there are several answers. Choosing the first --and not
 * combining them nor keeping the last-- is what keeps the main look and feel in charge: the
 * auxiliaries watch, they do not decide.
 */
public class MultiTreeUI extends TreeUI {

    /**
     * The looks and feels that are handled, in order.
     *
     * <p>The first one is the main one. The order is fixed by {@link MultiLookAndFeel#createUIs}
     * and is not a detail: it is what decides who answers.
     */
    protected Vector<ComponentUI> uis = new Vector<ComponentUI>();

    /** One with no look and feel; {@link #createUI} adds them. */
    public MultiTreeUI() {
    }

    /**
     * The looks and feels that are handled.
     *
     * @return a new array, with the main one first
     */
    public ComponentUI[] getUIs() {
        return MultiLookAndFeel.uisToArray(uis);
    }

    /**
     * The look and feel for that component.
     *
     * <p>It returns one of these only if there is more than one look and feel configured. With a
     * single one it returns that one, without wrapping it: sharing out among one is not needed and
     * would cost one extra call per operation.
     *
     * @param a the component
     * @return the look and feel
     */
    public static ComponentUI createUI(JComponent a) {
        MultiTreeUI mui = new MultiTreeUI();
        return MultiLookAndFeel.createUIs(mui, mui.uis, a);
    }


    /**
     * It asks them all and answers with the first.
     *
     * @param jTree the {@code JTree}
     * @param treePath the {@code TreePath}
     * @return whatever the first one answered
     */
    public Rectangle getPathBounds(JTree jTree, TreePath treePath) {
        Rectangle returnValue = ((TreeUI) uis.elementAt(0)).getPathBounds(jTree, treePath);
        for (int i = 1; i < uis.size(); i++) {
            ((TreeUI) uis.elementAt(i)).getPathBounds(jTree, treePath);
        }
        return returnValue;
    }

    /**
     * It asks them all and answers with the first.
     *
     * @param jTree the {@code JTree}
     * @param i2 the {@code int}
     * @return whatever the first one answered
     */
    public TreePath getPathForRow(JTree jTree, int i2) {
        TreePath returnValue = ((TreeUI) uis.elementAt(0)).getPathForRow(jTree, i2);
        for (int i = 1; i < uis.size(); i++) {
            ((TreeUI) uis.elementAt(i)).getPathForRow(jTree, i2);
        }
        return returnValue;
    }

    /**
     * It asks them all and answers with the first.
     *
     * @param jTree the {@code JTree}
     * @param treePath the {@code TreePath}
     * @return whatever the first one answered
     */
    public int getRowForPath(JTree jTree, TreePath treePath) {
        int returnValue = ((TreeUI) uis.elementAt(0)).getRowForPath(jTree, treePath);
        for (int i = 1; i < uis.size(); i++) {
            ((TreeUI) uis.elementAt(i)).getRowForPath(jTree, treePath);
        }
        return returnValue;
    }

    /**
     * It asks them all and answers with the first.
     *
     * @param jTree the {@code JTree}
     * @return whatever the first one answered
     */
    public int getRowCount(JTree jTree) {
        int returnValue = ((TreeUI) uis.elementAt(0)).getRowCount(jTree);
        for (int i = 1; i < uis.size(); i++) {
            ((TreeUI) uis.elementAt(i)).getRowCount(jTree);
        }
        return returnValue;
    }

    /**
     * It asks them all and answers with the first.
     *
     * @param jTree the {@code JTree}
     * @param i2 the {@code int}
     * @param i3 the {@code int}
     * @return whatever the first one answered
     */
    public TreePath getClosestPathForLocation(JTree jTree, int i2, int i3) {
        TreePath returnValue = ((TreeUI) uis.elementAt(0)).getClosestPathForLocation(jTree, i2, i3);
        for (int i = 1; i < uis.size(); i++) {
            ((TreeUI) uis.elementAt(i)).getClosestPathForLocation(jTree, i2, i3);
        }
        return returnValue;
    }

    /**
     * It asks them all and answers with the first.
     *
     * @param jTree the {@code JTree}
     * @return whatever the first one answered
     */
    public boolean isEditing(JTree jTree) {
        boolean returnValue = ((TreeUI) uis.elementAt(0)).isEditing(jTree);
        for (int i = 1; i < uis.size(); i++) {
            ((TreeUI) uis.elementAt(i)).isEditing(jTree);
        }
        return returnValue;
    }

    /**
     * It asks them all and answers with the first.
     *
     * @param jTree the {@code JTree}
     * @return whatever the first one answered
     */
    public boolean stopEditing(JTree jTree) {
        boolean returnValue = ((TreeUI) uis.elementAt(0)).stopEditing(jTree);
        for (int i = 1; i < uis.size(); i++) {
            ((TreeUI) uis.elementAt(i)).stopEditing(jTree);
        }
        return returnValue;
    }

    /**
     * It asks them all and answers with the first.
     *
     * @param jTree the {@code JTree}
     */
    public void cancelEditing(JTree jTree) {
        for (int i = 0; i < uis.size(); i++) {
            ((TreeUI) uis.elementAt(i)).cancelEditing(jTree);
        }
    }

    /**
     * It asks them all and answers with the first.
     *
     * @param jTree the {@code JTree}
     * @param treePath the {@code TreePath}
     */
    public void startEditingAtPath(JTree jTree, TreePath treePath) {
        for (int i = 0; i < uis.size(); i++) {
            ((TreeUI) uis.elementAt(i)).startEditingAtPath(jTree, treePath);
        }
    }

    /**
     * It asks them all and answers with the first.
     *
     * @param jTree the {@code JTree}
     * @return whatever the first one answered
     */
    public TreePath getEditingPath(JTree jTree) {
        TreePath returnValue = ((TreeUI) uis.elementAt(0)).getEditingPath(jTree);
        for (int i = 1; i < uis.size(); i++) {
            ((TreeUI) uis.elementAt(i)).getEditingPath(jTree);
        }
        return returnValue;
    }

    /**
     * Whether the point falls inside the component.
     *
     * @param jComponent the {@code JComponent}
     * @param i2 the {@code int}
     * @param i3 the {@code int}
     * @return whatever the first one answered
     */
    public boolean contains(JComponent jComponent, int i2, int i3) {
        boolean returnValue = ((TreeUI) uis.elementAt(0)).contains(jComponent, i2, i3);
        for (int i = 1; i < uis.size(); i++) {
            ((TreeUI) uis.elementAt(i)).contains(jComponent, i2, i3);
        }
        return returnValue;
    }

    /**
     * It redraws the background and then the component.
     *
     * @param graphics the {@code Graphics}
     * @param jComponent the {@code JComponent}
     */
    public void update(Graphics graphics, JComponent jComponent) {
        for (int i = 0; i < uis.size(); i++) {
            ((TreeUI) uis.elementAt(i)).update(graphics, jComponent);
        }
    }

    /**
     * It installs itself on the component.
     *
     * @param jComponent the {@code JComponent}
     */
    public void installUI(JComponent jComponent) {
        for (int i = 0; i < uis.size(); i++) {
            ((TreeUI) uis.elementAt(i)).installUI(jComponent);
        }
    }

    /**
     * It uninstalls itself from the component.
     *
     * @param jComponent the {@code JComponent}
     */
    public void uninstallUI(JComponent jComponent) {
        for (int i = 0; i < uis.size(); i++) {
            ((TreeUI) uis.elementAt(i)).uninstallUI(jComponent);
        }
    }

    /**
     * It draws the component.
     *
     * @param graphics the {@code Graphics}
     * @param jComponent the {@code JComponent}
     */
    public void paint(Graphics graphics, JComponent jComponent) {
        for (int i = 0; i < uis.size(); i++) {
            ((TreeUI) uis.elementAt(i)).paint(graphics, jComponent);
        }
    }

    /**
     * The size it would prefer to have.
     *
     * @param jComponent the {@code JComponent}
     * @return whatever the first one answered
     */
    public Dimension getPreferredSize(JComponent jComponent) {
        Dimension returnValue = ((TreeUI) uis.elementAt(0)).getPreferredSize(jComponent);
        for (int i = 1; i < uis.size(); i++) {
            ((TreeUI) uis.elementAt(i)).getPreferredSize(jComponent);
        }
        return returnValue;
    }

    /**
     * The smallest size it can manage with.
     *
     * @param jComponent the {@code JComponent}
     * @return whatever the first one answered
     */
    public Dimension getMinimumSize(JComponent jComponent) {
        Dimension returnValue = ((TreeUI) uis.elementAt(0)).getMinimumSize(jComponent);
        for (int i = 1; i < uis.size(); i++) {
            ((TreeUI) uis.elementAt(i)).getMinimumSize(jComponent);
        }
        return returnValue;
    }

    /**
     * The largest size it accepts.
     *
     * @param jComponent the {@code JComponent}
     * @return whatever the first one answered
     */
    public Dimension getMaximumSize(JComponent jComponent) {
        Dimension returnValue = ((TreeUI) uis.elementAt(0)).getMaximumSize(jComponent);
        for (int i = 1; i < uis.size(); i++) {
            ((TreeUI) uis.elementAt(i)).getMaximumSize(jComponent);
        }
        return returnValue;
    }

    /**
     * How many accessible children it has.
     *
     * @param jComponent the {@code JComponent}
     * @return whatever the first one answered
     */
    public int getAccessibleChildrenCount(JComponent jComponent) {
        int returnValue = ((TreeUI) uis.elementAt(0)).getAccessibleChildrenCount(jComponent);
        for (int i = 1; i < uis.size(); i++) {
            ((TreeUI) uis.elementAt(i)).getAccessibleChildrenCount(jComponent);
        }
        return returnValue;
    }

    /**
     * The accessible child at that position.
     *
     * @param jComponent the {@code JComponent}
     * @param i2 the {@code int}
     * @return whatever the first one answered
     */
    public Accessible getAccessibleChild(JComponent jComponent, int i2) {
        Accessible returnValue = ((TreeUI) uis.elementAt(0)).getAccessibleChild(jComponent, i2);
        for (int i = 1; i < uis.size(); i++) {
            ((TreeUI) uis.elementAt(i)).getAccessibleChild(jComponent, i2);
        }
        return returnValue;
    }
}
