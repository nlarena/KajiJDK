package javax.swing;

import java.awt.Component;
import java.awt.Container;
import java.io.Serializable;
import java.util.Comparator;

/**
 * The focus policy Swing uses: it walks in the order things are seen.
 *
 * <h2>Top to bottom, left to right</h2>
 *
 * <p>It is the only thing it adds over {@link SortingFocusTraversalPolicy}: the comparator. It
 * sorts by the vertical coordinate and, within the same row, by the horizontal one. It is how
 * one reads, and that is why it agrees with what the user expects of the tab key without
 * anybody configuring anything.
 *
 * <p>"The same row" is not "the same exact coordinate": two aligned fields may be a pixel
 * apart by their edges. They are taken to be of the same row when they overlap vertically, which
 * is what keeps a taller button beside a field from falling out of order.
 *
 * <h2>It also filters a little more</h2>
 *
 * <p>{@link #accept} also skips read-only text components: they can be focused, but there is
 * nothing to do there with the keyboard, and stopping at them is more of a nuisance than a
 * help.
 */
public class LayoutFocusTraversalPolicy extends SortingFocusTraversalPolicy
        implements Serializable {

    /** With the reading order. */
    public LayoutFocusTraversalPolicy() {
        super(new ByPosition());
    }

    /** With another criterion; only for the library's subclasses. */
    LayoutFocusTraversalPolicy(Comparator<? super Component> c) {
        super(c);
    }

    public Component getComponentAfter(Container aContainer, Component aComponent) {
        return super.getComponentAfter(aContainer, aComponent);
    }

    public Component getComponentBefore(Container aContainer, Component aComponent) {
        return super.getComponentBefore(aContainer, aComponent);
    }

    public Component getFirstComponent(Container aContainer) {
        return super.getFirstComponent(aContainer);
    }

    public Component getLastComponent(Container aContainer) {
        return super.getLastComponent(aContainer);
    }

    /**
     * Besides what the class above asks for, it skips read-only text.
     *
     * <p>See the class note.
     */
    protected boolean accept(Component aComponent) {
        if (!super.accept(aComponent)) {
            return false;
        }
        if (aComponent instanceof javax.swing.text.JTextComponent) {
            javax.swing.text.JTextComponent t = (javax.swing.text.JTextComponent) aComponent;
            if (!t.isEditable()) {
                return false;
            }
        }
        return true;
    }

    /** Top to bottom, and within the same row, left to right. */
    private static class ByPosition implements Comparator<Component>, Serializable {

        public int compare(Component a, Component b) {
            if (a == b) {
                return 0;
            }
            int ay = a.getY();
            int by = b.getY();
            int ah = a.getHeight();
            int bh = b.getHeight();
            // They overlap vertically: the same row. See the class note.
            boolean sameRow = (ay < by + bh) && (by < ay + ah);
            if (!sameRow) {
                return (ay < by) ? -1 : 1;
            }
            int ax = a.getX();
            int bx = b.getX();
            if (ax != bx) {
                return (ax < bx) ? -1 : 1;
            }
            if (ay != by) {
                return (ay < by) ? -1 : 1;
            }
            // The same exact position: it is broken by identity so that the order is total and
                        // stable. Without this, two overlapping components could swap between two
                        // walks and the tab key would become unpredictable.
            return (System.identityHashCode(a) < System.identityHashCode(b)) ? -1 : 1;
        }
    }
}
