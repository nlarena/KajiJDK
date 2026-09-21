package javax.swing.text;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;

/**
 * A real component embedded in the text: a button, a field, whatever.
 *
 * <h2>The view does not draw it</h2>
 *
 * <p>The component is added to the editor's container and draws itself, like any child. This
 * view only <em>places</em> it: it sets the size and the position it gets in the text. Hence
 * {@link #paint} paints nothing beyond arranging it.
 *
 * <p>That brings a consequence worth knowing: the component goes on existing even if its
 * stretch of text falls outside the view, and its preferred size rules over the paragraph's
 * layout.
 */
public class ComponentView extends View {

    private Component createdC;
    private Invalidator c;

    public ComponentView(Element elem) {
        super(elem);
    }

    /**
     * The component to show; the one the element has as an attribute.
     *
     * <p>A subclass may build it instead of taking it from the attribute: it is how something that
     * was not in the document gets embedded.
     */
    protected Component createComponent() {
        AttributeSet attr = getElement().getAttributes();
        Component comp = StyleConstants.getComponent(attr);
        return comp;
    }

    public final Component getComponent() {
        return createdC;
    }

    /** It does not draw: it arranges. See the class note. */
    public void paint(Graphics g, Shape a) {
        if (c != null) {
            Rectangle alloc = (a instanceof Rectangle) ? (Rectangle) a : a.getBounds();
            c.setBounds(alloc.x, alloc.y, alloc.width, alloc.height);
        }
    }

    public float getPreferredSpan(int axis) {
        if ((axis != X_AXIS) && (axis != Y_AXIS)) {
            throw new IllegalArgumentException("Invalid axis: " + axis);
        }
        if (c != null) {
            Dimension size = c.getPreferredSize();
            if (axis == View.X_AXIS) {
                return size.width;
            }
            return size.height;
        }
        return 0;
    }

    public float getMinimumSpan(int axis) {
        if ((axis != X_AXIS) && (axis != Y_AXIS)) {
            throw new IllegalArgumentException("Invalid axis: " + axis);
        }
        if (c != null) {
            Dimension size = c.getMinimumSize();
            if (axis == View.X_AXIS) {
                return size.width;
            }
            return size.height;
        }
        return 0;
    }

    public float getMaximumSpan(int axis) {
        if ((axis != X_AXIS) && (axis != Y_AXIS)) {
            throw new IllegalArgumentException("Invalid axis: " + axis);
        }
        if (c != null) {
            Dimension size = c.getMaximumSize();
            if (axis == View.X_AXIS) {
                return size.width;
            }
            return size.height;
        }
        return 0;
    }

    /** It is aligned in the middle, like any view with no baseline of its own. */
    public float getAlignment(int axis) {
        return super.getAlignment(axis);
    }

    /**
     * On entering the tree, it adds the component to the editor; on leaving, it removes it.
     *
     * <p>It is the only place where a view touches the component hierarchy, and that is why it is
     * carefully tied to the view's life cycle.
     */
    public void setParent(View p) {
        super.setParent(p);
        if (p != null) {
            Container host = getContainer();
            if (host != null) {
                if (createdC == null) {
                    createdC = createComponent();
                    if (createdC != null) {
                        c = new Invalidator(createdC, this);
                        host.add(createdC);
                    }
                }
            }
        } else {
            if (c != null) {
                Container host = c.getParent();
                if (host != null) {
                    host.remove(c.getComponent());
                }
                c = null;
                createdC = null;
            }
        }
    }

    /** It arranges the component inside the editor. */
    void setComponentParent() {
    }

    public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException {
        int p0 = getStartOffset();
        int p1 = getEndOffset();
        if ((pos >= p0) && (pos <= p1)) {
            Rectangle r = a.getBounds();
            if (pos == p1) {
                r.x = r.x + r.width;
            }
            r.width = 0;
            return r;
        }
        throw new BadLocationException(pos + " not in range " + p0 + "," + p1, pos);
    }

    public int viewToModel(float x, float y, Shape a, Position.Bias[] bias) {
        Rectangle alloc = (Rectangle) a;
        if (x < alloc.x + (alloc.width / 2)) {
            bias[0] = Position.Bias.Forward;
            return getStartOffset();
        }
        bias[0] = Position.Bias.Backward;
        return getEndOffset();
    }

    /**
     * It wraps the component so as to know when it changes size.
     *
     * <p>An embedded component that changes size has to make the paragraph be redone, and the only
     * way of hearing about it is to listen to it.
     */
    static class Invalidator {

        private final Component comp;
        private final ComponentView view;

        Invalidator(Component comp, ComponentView view) {
            this.comp = comp;
            this.view = view;
        }

        Component getComponent() {
            return comp;
        }

        Container getParent() {
            return comp.getParent();
        }

        Dimension getPreferredSize() {
            return comp.getPreferredSize();
        }

        Dimension getMinimumSize() {
            return comp.getMinimumSize();
        }

        Dimension getMaximumSize() {
            return comp.getMaximumSize();
        }

        void setBounds(int x, int y, int w, int h) {
            comp.setBounds(x, y, w, h);
        }
    }
}
