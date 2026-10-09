package javax.swing.border;

import java.awt.Component;
import java.awt.Component$BaselineResizeBehavior;
import java.awt.ComponentOrientation;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;

/**
 * The base of every border: it implements {@link Border}'s three methods without drawing
 * anything.
 *
 * <h2>Why a border that does nothing is useful</h2>
 *
 * <p>Because almost no concrete border needs the three methods. One that only reserves space
 * ({@link EmptyBorder}) does not paint; one that only paints a line does not need to rewrite the
 * computation of the inner box. Extending this class lets one write only what the border really
 * does.
 *
 * <p>The default values are the <em>neutral</em> ones: it does not paint, does not take up
 * space, is not opaque. All three are safe -- a half-written border looks as if it were not
 * there, instead of breaking the layout or leaving rubbish on the screen.
 *
 * <h2>The two forms of {@link #getBorderInsets}</h2>
 *
 * <p>The one-argument one creates a new {@link Insets}; the two-argument one
 * <strong>reuses</strong> the one it is given. The second exists because Swing's layout asks for
 * the insets many times per second and allocating an object on every query shows. Subclasses
 * override the two-argument one, and the one-argument one calls it with a fresh {@code Insets}:
 * that way there is a single place with the logic.
 */
public abstract class AbstractBorder implements Border, java.io.Serializable {

    private static final long serialVersionUID = -511181274974418195L;

    /** For the subclasses. */
    protected AbstractBorder() {
    }

    /** It draws nothing. */
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
    }

    /** It reserves no space. */
    public Insets getBorderInsets(Component c) {
        return getBorderInsets(c, new Insets(0, 0, 0, 0));
    }

    /**
     * It reserves no space, reusing {@code insets}.
     *
     * <p>It is the form subclasses override; see the class note.
     */
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.left = 0;
        insets.top = 0;
        insets.right = 0;
        insets.bottom = 0;
        return insets;
    }

    /** It is not opaque. */
    public boolean isBorderOpaque() {
        return false;
    }

    /** The rectangle left inside the border, that is the given one minus the insets. */
    public Rectangle getInteriorRectangle(Component c, int x, int y, int width, int height) {
        return getInteriorRectangle(c, this, x, y, width, height);
    }

    /**
     * The same, for any border.
     *
     * <p>Static because {@link CompoundBorder} needs it over its <em>outer</em> border, which is
     * not {@code this}. A {@code null} border reserves nothing, which is what allows writing "this
     * component's border, if it has one" without an {@code if}.
     */
    public static Rectangle getInteriorRectangle(Component c, Border b, int x, int y, int width,
            int height) {
        Insets insets;
        if (b != null) {
            insets = b.getBorderInsets(c);
        } else {
            insets = new Insets(0, 0, 0, 0);
        }
        return new Rectangle(x + insets.left, y + insets.top,
                width - insets.right - insets.left, height - insets.top - insets.bottom);
    }

    /**
     * This border's text baseline, or {@code -1} if it has none.
     *
     * <p>It exists so that a border with text --{@link TitledBorder}-- can line up with the one
     * next to it. The rest have nothing to line up.
     *
     * @throws IllegalArgumentException if {@code width} or {@code height} are negative
     */
    public int getBaseline(Component c, int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Width and height cannot be negative");
        }
        return -1;
    }

    /**
     * How the baseline moves when the component changes size.
     *
     * <p>The type goes with the <strong>binary</strong> name {@code
     * Component$BaselineResizeBehavior}: the Java name of a nested type from another file does not
     * resolve in our compiler (#101), and the detour through {@code import} emits a descriptor of a
     * class that does not exist (#208).
     */
    public Component$BaselineResizeBehavior getBaselineResizeBehavior(Component c) {
        if (c == null) {
            throw new NullPointerException("The component cannot be null");
        }
        return Component$BaselineResizeBehavior.OTHER;
    }

    /**
     * Whether {@code c} reads from left to right.
     *
     * <p>Package access and not public: the borders that draw something asymmetric --a
     * {@link TitledBorder}'s title-- use it, and it is not part of anybody else's contract.
     */
    static boolean isLeftToRight(Component c) {
        ComponentOrientation o = c.getComponentOrientation();
        return o.isLeftToRight();
    }
}
