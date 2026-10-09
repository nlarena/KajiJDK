package javax.swing.border;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

/**
 * The frame that surrounds a component.
 *
 * <h2>Why an object and not a property of the component</h2>
 *
 * <p>Because that way borders <strong>compose</strong>: {@link CompoundBorder} puts one inside
 * another and the result is another {@code Border}, indistinguishable from the basic ones. If the
 * frame were a handful of fields in the component --thickness, colour, style-- that combination
 * would not exist.
 *
 * <p>Hence too a border is normally <strong>immutable and shareable</strong>: it keeps nothing of
 * the component that uses it, so one same instance serves a hundred buttons.
 *
 * <h2>The three methods, and why {@link #isBorderOpaque} is not redundant</h2>
 *
 * <p>Painting and declaring how much space it takes are the two obvious ones. The third is a
 * promise Swing uses to optimize: an opaque border covers <em>all</em> the pixels of its area, so
 * whatever is underneath does not need drawing. Lying there does not break the layout -- it
 * leaves rubbish on the screen.
 */
public interface Border {

    /** It is drawn around {@code c}, in the given rectangle. */
    void paintBorder(Component c, Graphics g, int x, int y, int width, int height);

    /** How much space is reserved on each side. */
    Insets getBorderInsets(Component c);

    /** Whether it covers all the pixels of its area; see the interface note. */
    boolean isBorderOpaque();
}
