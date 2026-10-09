package javax.swing;

import java.awt.Graphics2D;

/**
 * Something that knows how to draw itself in a rectangle.
 *
 * <h2>Why it is not simply an {@code Icon}</h2>
 *
 * <p>An icon has a size of its own and is drawn at a position. A painter does not: it receives
 * the width and the height on each call and adapts. That is the whole difference, and it is the
 * one needed in order to paint the background of a component that changes size.
 *
 * <p>The object it is passed is what is being painted -- normally the component -- so that the
 * painter can look at its state: whether it is pressed, whether it has the focus, whether it is
 * disabled.
 *
 * @param <T> what is painted
 * @since 1.7
 */
public interface Painter<T> {

    /**
     * It draws in that rectangle.
     *
     * @param g where to draw; the painter may modify it without restoring it
     * @param object what is being painted, or {@code null}
     * @param width the width
     * @param height the height
     */
    void paint(Graphics2D g, T object, int width, int height);
}
