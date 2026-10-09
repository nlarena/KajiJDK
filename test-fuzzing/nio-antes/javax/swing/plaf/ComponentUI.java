package javax.swing.plaf;

import java.awt.Component$BaselineResizeBehavior;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.accessibility.Accessible;
import javax.swing.JComponent;

/**
 * A component's "look and feel" half: whoever draws and measures it, separate from whoever
 * models it.
 *
 * <h2>The idea that holds all of Swing up</h2>
 *
 * <p>A {@link JComponent} knows what it is --a button, a table-- but not how it looks. That is
 * known by its {@code ComponentUI}, which can be changed on the fly: it is what allows the same
 * application to look like Windows, like Metal or like whatever a new look and feel decides,
 * without touching the model. Hence every method takes the component as a parameter: <strong>a
 * UI does not keep the component</strong>, and one same UI may serve several.
 *
 * <h2>{@link #update} against {@link #paint}</h2>
 *
 * <p>They are two methods because they are two responsibilities. {@code update} clears the
 * background if the component is opaque and then calls {@code paint}; {@code paint} draws the
 * content and knows nothing about the background. A look and feel that overrides only
 * {@code paint} keeps the clearing; one that overrides {@code update} chooses not to clear --
 * which is what a translucent component does.
 *
 * <h2>Everything returns "I do not know"</h2>
 *
 * <p>The measurements return {@code null} and {@link #getBaseline} returns {@code -1}: it is the
 * signal that the UI has no opinion and the component falls back on its own computation. An
 * empty UI is therefore valid and breaks nothing, which is the reason this class is concrete and
 * not an interface.
 *
 * <p>{@link #createUI} throws: each subclass shadows it with a static method of its own, and
 * calling this one directly is a program error. It is what the JDK does.
 */
public abstract class ComponentUI {

    /** For the subclasses. */
    public ComponentUI() {
    }

    /** This UI starts serving {@code c}: it installs colours, font, border, listeners. */
    public void installUI(JComponent c) {
    }

    /** It undoes exactly what {@link #installUI} did. */
    public void uninstallUI(JComponent c) {
    }

    /** Draws the content. Without the background: that belongs to {@link #update}. */
    public void paint(Graphics g, JComponent c) {
    }

    /** Clears the background if the component is opaque, and then draws. */
    public void update(Graphics g, JComponent c) {
        if (c.isOpaque()) {
            g.setColor(c.getBackground());
            g.fillRect(0, 0, c.getWidth(), c.getHeight());
        }
        paint(g, c);
    }

    /** The preferred size, or {@code null} if this UI has no opinion. */
    public Dimension getPreferredSize(JComponent c) {
        return null;
    }

    /** The minimum size; by default, the preferred one. */
    public Dimension getMinimumSize(JComponent c) {
        return getPreferredSize(c);
    }

    /** The maximum size; by default, the preferred one. */
    public Dimension getMaximumSize(JComponent c) {
        return getPreferredSize(c);
    }

    /**
     * Whether the point falls inside the component.
     *
     * <p>It exists so that a look and feel can give a component a shape that is not its rectangle
     * --a round button-- and so that clicks outside that shape go straight through.
     */
    public boolean contains(JComponent c, int x, int y) {
        return c.inside(x, y);
    }

    /**
     * @throws Error always: each subclass provides its own, static and with the same name
     */
    public static ComponentUI createUI(JComponent c) {
        throw new Error("ComponentUI.createUI is not implemented; every subclass shadows it");
    }

    /**
     * The component's baseline, or {@code -1} if it has none.
     *
     * @throws IllegalArgumentException if the width or the height are negative
     */
    public int getBaseline(JComponent c, int width, int height) {
        if (c == null) {
            throw new NullPointerException("The component cannot be null");
        }
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Width and height cannot be negative");
        }
        return -1;
    }

    /**
     * How the baseline moves when the size changes.
     *
     * <p>With the binary name {@code Component$BaselineResizeBehavior}: a nested type from another
     * file does not resolve by its Java name in our compiler (#101).
     */
    public Component$BaselineResizeBehavior getBaselineResizeBehavior(JComponent c) {
        if (c == null) {
            throw new NullPointerException("The component cannot be null");
        }
        return Component$BaselineResizeBehavior.OTHER;
    }

    /** How many accessible children it has; by default, the container's children. */
    public int getAccessibleChildrenCount(JComponent c) {
        return c.getComponentCount();
    }

    /** Accessible child number {@code i}, or {@code null} if it is not {@link Accessible}. */
    public Accessible getAccessibleChild(JComponent c, int i) {
        if (i < 0 || i >= c.getComponentCount()) {
            return null;
        }
        java.awt.Component child = c.getComponent(i);
        if (child instanceof Accessible) {
            return (Accessible) child;
        }
        return null;
    }
}
