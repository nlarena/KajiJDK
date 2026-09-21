package javax.swing;

import java.awt.Dimension;
import java.awt.Rectangle;

/**
 * What a component may tell the {@link JScrollPane} that shows it.
 *
 * <p>Without this, a pane with bars treats its content as a dumb rectangle: it scrolls it one
 * pixel at a time and leaves it at its preferred size. Implementing it is how a list says "a
 * wheel moves one row, not three pixels" and how a text area says "make me as wide as the
 * viewport and do not give me a horizontal bar".
 *
 * <p>The last two are the ones that change what is seen most: answering {@code true} in
 * {@link #getScrollableTracksViewportWidth} forces the content to measure what the viewport
 * measures, and then there is never any need to scroll in that direction.
 */
public interface Scrollable {

    /** What size the viewport that shows it would like to have. */
    Dimension getPreferredScrollableViewportSize();

    /**
     * How much to advance in a small step -- an arrow, a wheel notch --, in pixels.
     *
     * @param visibleRect what is seen now, in the component's coordinates
     * @param orientation {@code SwingConstants.VERTICAL} or {@code HORIZONTAL}
     * @param direction negative upwards or to the left, positive the other way round
     */
    int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction);

    /** How much to advance in a big step -- a click on the track, page down --, in pixels. */
    int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction);

    /** Whether its width must always be the viewport's; see the interface note. */
    boolean getScrollableTracksViewportWidth();

    /** Whether its height must always be the viewport's. */
    boolean getScrollableTracksViewportHeight();
}
