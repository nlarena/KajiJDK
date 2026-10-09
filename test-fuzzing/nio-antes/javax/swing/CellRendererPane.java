package javax.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;

/**
 * The place where the components that draw cells live.
 *
 * <h2>Why a container that does not contain is needed</h2>
 *
 * <p>A list of a thousand elements does not have a thousand components: it has <em>one</em>,
 * which is configured and drawn a thousand times in a thousand different places. That borrowed
 * component has to be somewhere -- Swing asks that a component have a parent in order to
 * measure itself and to draw itself --, but it must not take part in anything: it cannot be
 * walked through with the tab key, it is not repainted when it changes, it does not inherit
 * the validation.
 *
 * <p>This pane is that place. It is a real container, but
 * <strong>switched off on purpose</strong>:
 *
 * <ul>
 * <li>{@link #invalidate} does nothing -- a renderer that invalidates itself must not
 *     invalidate the whole list;</li>
 * <li>{@link #paint} and {@link #update} do nothing -- the renderers are painted by whoever
 *     uses them, when their turn comes, and not by this pane on its own;</li>
 * <li>{@link #addImpl} takes the component out of wherever it was before adding it.</li>
 * </ul>
 *
 * <h2>How it is used</h2>
 *
 * <p>With {@link #paintComponent}: it is passed the already configured renderer, the list's
 * {@code Graphics} and the rectangle it goes in. The pane places it, draws it there and leaves
 * it as it was. The component never learns that it was drawn a thousand times.
 */
public class CellRendererPane extends Container implements Accessible {

    protected AccessibleContext accessibleContext = null;

    /** An empty pane, invisible and with no layout. */
    public CellRendererPane() {
        super();
        setLayout(null);
        setVisible(false);
    }

    /** It does nothing; see the class note. */
    public void invalidate() {
    }

    /** It does nothing; see the class note. */
    public void paint(Graphics g) {
    }

    /** It does nothing; it does not even clear the background. */
    public void update(Graphics g) {
    }

    /**
     * It adds the component, taking it out of wherever it was first.
     *
     * <p>A renderer shared between two lists would end up with the last one that used it as its
     * parent; this moves it instead of leaving it in both.
     */
    protected void addImpl(Component x, Object constraints, int index) {
        if (x.getParent() == this) {
            return;
        }
        super.addImpl(x, constraints, index);
    }

    /**
     * It draws the component in that rectangle of the given {@code Graphics}.
     *
     * <p>With {@code shouldValidate} at true it is validated first; that is needed when the
     * renderer has children to lay out, and it is expensive, which is why it is not the usual
     * thing.
     */
    public void paintComponent(Graphics g, Component c, Container p, int x, int y, int w, int h,
            boolean shouldValidate) {
        if (c == null) {
            if (p != null) {
                Color background = p.getBackground();
                g.setColor(background);
                g.fillRect(x, y, w, h);
            }
            return;
        }
        if (c.getParent() != this) {
            this.add(c);
        }
        c.setBounds(x, y, w, h);
        if (shouldValidate) {
            c.validate();
        }
        // The origin is translated instead of asking the component for a {@code Graphics} of its
                // own: the component is not on the screen and does not have one.
        Graphics cg = g.create(x, y, w, h);
        try {
            c.paint(cg);
        } finally {
            cg.dispose();
        }
        // It is taken out of where it ended up so that a repaint of the list does not draw it
                // again on its own, now at the last place it was.
        c.setBounds(-w, -h, 0, 0);
    }

    /** Without validating. */
    public void paintComponent(Graphics g, Component c, Container p, int x, int y, int w, int h) {
        paintComponent(g, c, p, x, y, w, h, false);
    }

    /** With the rectangle given in one go. */
    public void paintComponent(Graphics g, Component c, Container p, Rectangle r) {
        paintComponent(g, c, p, r.x, r.y, r.width, r.height);
    }

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }
}
