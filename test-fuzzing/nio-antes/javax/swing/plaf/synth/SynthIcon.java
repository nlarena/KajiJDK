package javax.swing.plaf.synth;

import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;

/**
 * An icon that knows what state whatever carries it is in.
 *
 * <h2>Why an icon needs the context</h2>
 *
 * <p>An ordinary {@link Icon} is always drawn the same. A Synth one is not: the tick of a check
 * box that is off, pressed or with the cursor over it are three different images, and even the
 * <strong>size</strong> may change between states. That is why the three {@code Icon} operations
 * have here a version that takes a {@link SynthContext}.
 *
 * <p>The three old ones go on working: they are {@code default} and call the new ones with a null
 * context. An icon that depends on the state will return something generic; one that does not,
 * the usual thing. That is what allows passing a {@code SynthIcon} to anything that expects an
 * {@code Icon}.
 */
public interface SynthIcon extends Icon {

    /**
     * The width in that context.
     *
     * @param context the context, or {@code null}
     * @return the width
     */
    int getIconWidth(SynthContext context);

    /**
     * The height in that context.
     *
     * @param context the context, or {@code null}
     * @return the height
     */
    int getIconHeight(SynthContext context);

    /**
     * It draws the icon in that context.
     *
     * @param context the context, or {@code null}
     * @param g where to draw
     * @param x the corner
     * @param y the corner
     * @param w the requested width
     * @param h the requested height
     */
    void paintIcon(SynthContext context, Graphics g, int x, int y, int w, int h);

    /** With no context; see the interface note. */
    default int getIconWidth() {
        return getIconWidth(null);
    }

    /** With no context. */
    default int getIconHeight() {
        return getIconHeight(null);
    }

    /** With no context, and with the size the icon says. */
    default void paintIcon(Component c, Graphics g, int x, int y) {
        paintIcon(null, g, x, y, getIconWidth(null), getIconHeight(null));
    }
}
