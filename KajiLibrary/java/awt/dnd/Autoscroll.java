package java.awt.dnd;

import java.awt.Insets;
import java.awt.Point;

/**
 * It is implemented by a component that wants to scroll by itself while something is dragged over
 * it.
 *
 * <p>It solves a real problem: to drop something at the end of a long list one has to get there,
 * and with the button held down the scroll bar cannot be used. The solution is for the component to
 * scroll by itself when the pointer comes near its edge.
 *
 * <p>The insets of {@link #getAutoscrollInsets} are **from the outside inwards**: they say at what
 * distance from the edge the sensitive zone starts. Big insets make the scrolling start straight
 * away; small ones, that one has to go almost to the edge.
 */
public interface Autoscroll {

    /** At what distance from each edge the zone that sets the scrolling off starts. */
    Insets getAutoscrollInsets();

    /** Scrolls one step, according to where the pointer is. */
    void autoscroll(Point cursorLocn);
}
