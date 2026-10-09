package javax.swing;

import java.awt.AWTError;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.LayoutManager;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;

/**
 * A lightweight container that only knows how to use {@link BoxLayout}, and the filler pieces
 * that go with it.
 *
 * <h2>The container</h2>
 *
 * <p>It paints nothing by default -- it is transparent -- and does not allow its arrangement to
 * be changed: that is all it adds over {@link JComponent}. Its value is in the factory methods:
 * a {@code createHorizontalBox()} says in one line what with {@code new JPanel} and
 * {@code setLayout} takes three.
 *
 * <h2>The filler pieces</h2>
 *
 * <p>A {@link Filler} is an invisible component that exists only in order to take up room. With
 * the three sizes equal it is a <em>strut</em> -- a gap of a fixed size --; with an enormous
 * maximum it is <em>glue</em>, which absorbs all the leftover space and pushes the rest. Two
 * pieces of glue, one on each side, centre; one alone in front aligns to the end.
 *
 * <p>Glue is this family's answer to "I want that button to end up on the right": there is no
 * constraint that says so, there is something that takes up the middle.
 */
public class Box extends JComponent implements Accessible {

    /** A box on that axis; see {@link BoxLayout}'s constants. */
    public Box(int axis) {
        super();
        super.setLayout(new BoxLayout(this, axis));
    }

    /** A horizontal box. */
    public static Box createHorizontalBox() {
        return new Box(BoxLayout.X_AXIS);
    }

    /** A vertical box. */
    public static Box createVerticalBox() {
        return new Box(BoxLayout.Y_AXIS);
    }

    /** A gap of a fixed size in both directions. */
    public static Component createRigidArea(Dimension d) {
        return new Filler(d, d, d);
    }

    /** A gap of a fixed width, which takes up no height and may stretch in height. */
    public static Component createHorizontalStrut(int width) {
        return new Filler(new Dimension(width, 0), new Dimension(width, 0),
                new Dimension(width, Short.MAX_VALUE));
    }

    /** A gap of a fixed height, which takes up no width and may stretch in width. */
    public static Component createVerticalStrut(int height) {
        return new Filler(new Dimension(0, height), new Dimension(0, height),
                new Dimension(Short.MAX_VALUE, height));
    }

    /** Glue in both directions; see the class note. */
    public static Component createGlue() {
        return new Filler(new Dimension(0, 0), new Dimension(0, 0),
                new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
    }

    /** Horizontal glue. */
    public static Component createHorizontalGlue() {
        return new Filler(new Dimension(0, 0), new Dimension(0, 0),
                new Dimension(Short.MAX_VALUE, 0));
    }

    /** Vertical glue. */
    public static Component createVerticalGlue() {
        return new Filler(new Dimension(0, 0), new Dimension(0, 0),
                new Dimension(0, Short.MAX_VALUE));
    }

    /** An {@link AWTError}: a box is its {@link BoxLayout}, without it it is nothing. */
    public void setLayout(LayoutManager l) {
        throw new AWTError("Illegal request");
    }

    /** It paints the background if it is opaque; by default it is not. */
    protected void paintComponent(Graphics g) {
        if (ui != null) {
            Graphics scratchGraphics = (g == null) ? null : g.create();
            try {
                ui.update(scratchGraphics, this);
            } finally {
                scratchGraphics.dispose();
            }
        } else if (isOpaque()) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    /** With no accessibility context: there is no assistive technology that reads it on this VM. */
    public AccessibleContext getAccessibleContext() {
        return null;
    }

    /**
     * An invisible component with the three sizes to order; see {@link Box}'s note.
     *
     * <p>It is public and not anonymous because the sizes can be changed afterwards, with
     * {@link #changeShape}: a strut that grows when the window does.
     */
    public static class Filler extends JComponent implements Accessible {

        /** A filler with those three sizes. */
        public Filler(Dimension min, Dimension pref, Dimension max) {
            setMinimumSize(min);
            setPreferredSize(pref);
            setMaximumSize(max);
        }

        /** It changes its three sizes at once and asks to be laid out again. */
        public void changeShape(Dimension min, Dimension pref, Dimension max) {
            setMinimumSize(min);
            setPreferredSize(pref);
            setMaximumSize(max);
            revalidate();
        }

        /**
         * It paints the background if it is opaque; by default it is not, and that is why it is not
         * seen.
         */
        protected void paintComponent(Graphics g) {
            if (ui != null) {
                Graphics scratchGraphics = (g == null) ? null : g.create();
                try {
                    ui.update(scratchGraphics, this);
                } finally {
                    scratchGraphics.dispose();
                }
            } else if (isOpaque()) {
                g.setColor(getBackground());
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        }

        /**
         * With no accessibility context: there is no assistive technology that reads it on this VM.
         */
        public AccessibleContext getAccessibleContext() {
            return null;
        }
    }
}
