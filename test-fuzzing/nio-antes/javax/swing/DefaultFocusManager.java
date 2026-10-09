package javax.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.FocusTraversalPolicy;

/**
 * Swing's focus manager from before Java 1.4.
 *
 * <h2>What it still does</h2>
 *
 * <p>Little: its five methods delegate to a {@link LayoutFocusTraversalPolicy}, which is the
 * policy Swing uses all the same without going through here. The class is kept because it is
 * public and because old code subclasses it in order to change the tab order -- which today is
 * done by giving the container a policy, with no manager in between.
 *
 * <p>See {@link FocusManager}'s note for the whole context.
 */
public class DefaultFocusManager extends FocusManager {

    /** The policy the five methods delegate to. */
    final FocusTraversalPolicy gluePolicy = new LayoutFocusTraversalPolicy();

    /** The usual manager. */
    public DefaultFocusManager() {
        setDefaultFocusTraversalPolicy(gluePolicy);
    }

    /** The component that comes next in the walk. */
    public Component getComponentAfter(Container aContainer, Component aComponent) {
        return gluePolicy.getComponentAfter(aContainer, aComponent);
    }

    /** The previous one. */
    public Component getComponentBefore(Container aContainer, Component aComponent) {
        return gluePolicy.getComponentBefore(aContainer, aComponent);
    }

    /** The first one. */
    public Component getFirstComponent(Container aContainer) {
        return gluePolicy.getFirstComponent(aContainer);
    }

    /** The last one. */
    public Component getLastComponent(Container aContainer) {
        return gluePolicy.getLastComponent(aContainer);
    }

    /**
     * Whether the first goes before the second in the walk.
     *
     * <p>It sorts by position, like {@link LayoutFocusTraversalPolicy}: above before below and, in
     * the same row, left before right.
     */
    public boolean compareTabOrder(Component a, Component b) {
        int ay = a.getY();
        int by = b.getY();
        int ah = a.getHeight();
        int bh = b.getHeight();
        boolean sameRow = (ay < by + bh) && (by < ay + ah);
        if (!sameRow) {
            return ay < by;
        }
        return a.getX() < b.getX();
    }
}
