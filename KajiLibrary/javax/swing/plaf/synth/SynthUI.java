package javax.swing.plaf.synth;

import java.awt.Graphics;

import javax.swing.JComponent;

/**
 * What every Synth look and feel has to know how to answer.
 *
 * <p>Two methods, and they are enough for the whole look.
 *
 * <p>{@link #getContext} returns that component's {@link SynthContext}: which region it is, what
 * state it is in and which style it gets. Everything else in Synth starts there -- the colours,
 * the typeface, the margins and the painter come from the context's style --, and that is why the
 * method is called on every drawing and not once on installing: the state changes with the mouse
 * and with the focus.
 *
 * <p>It inherits {@link SynthConstants} for nothing more than letting the classes that implement
 * it write {@code ENABLED} instead of {@code SynthConstants.ENABLED}.
 */
public interface SynthUI extends SynthConstants {

    /**
     * That component's context, with its state right now.
     *
     * @param c the component
     * @return the context
     */
    SynthContext getContext(JComponent c);

    /**
     * It draws the border.
     *
     * @param context what is being drawn
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    void paintBorder(SynthContext context, Graphics g, int x, int y, int w, int h);
}
