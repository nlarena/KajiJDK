package javax.swing.text;

import java.awt.Graphics;
import java.awt.Point;

import javax.swing.event.ChangeListener;

/**
 * The typing cursor: where it will be typed, and how much is selected.
 *
 * <h2>Two numbers, not one</h2>
 *
 * <p>The <em>dot</em> ({@link #getDot}) is where the cursor is; the <em>mark</em>
 * ({@link #getMark}) is where the selection started. If they are equal there is no selection.
 * That is why there are two operations and not one: {@link #setDot} moves both and undoes the
 * selection, {@link #moveDot} moves only the dot and extends it. It is exactly the difference
 * between clicking and dragging.
 *
 * <p>The <em>magic position</em> is the column the cursor remembers when going up and down
 * through lines of different lengths: without it, going down through a short line and coming
 * back up would leave the cursor shifted.
 */
public interface Caret {

    /** It is installed on a component; it is where it hooks itself to listen. */
    void install(JTextComponent c);

    void deinstall(JTextComponent c);

    void paint(Graphics g);

    void addChangeListener(ChangeListener l);

    void removeChangeListener(ChangeListener l);

    boolean isVisible();

    void setVisible(boolean v);

    boolean isSelectionVisible();

    void setSelectionVisible(boolean v);

    /** See the interface note about the magic position. */
    void setMagicCaretPosition(Point p);

    Point getMagicCaretPosition();

    /** Every how many milliseconds it blinks; zero so that it does not blink. */
    void setBlinkRate(int rate);

    int getBlinkRate();

    int getDot();

    int getMark();

    /** It moves the cursor and undoes the selection. */
    void setDot(int dot);

    /** It moves the cursor without moving the mark: it extends the selection. */
    void moveDot(int dot);
}
