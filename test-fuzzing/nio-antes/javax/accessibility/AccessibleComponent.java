package javax.accessibility;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.FocusListener;

/**
 * The graphical part of an accessible object: where it is, what size, what colour.
 *
 * <p>It is almost a mirror of {@code java.awt.Component}, and that duplication is deliberate. An
 * accessible object **does not have to be** an AWT component: it may be a cell of a hand-drawn
 * spreadsheet or an element of an interface engine of its own. Declaring the geometry here allows
 * an assistive technology to draw a box around either of the two.
 *
 * <p>{@link #getAccessibleAt} is the one that makes "what is under the pointer?" possible, which is
 * the question almost every inspection starts with.
 */
public interface AccessibleComponent {

    /** The background colour, or `null` if it does not support it. */
    Color getBackground();

    /** Changes the background colour. */
    void setBackground(Color c);

    /** The text colour, or `null` if it does not support it. */
    Color getForeground();

    /** Changes the text colour. */
    void setForeground(Color c);

    /** The cursor, or `null` if it does not support it. */
    Cursor getCursor();

    /** Changes the cursor. */
    void setCursor(Cursor cursor);

    /** The font, or `null` if it does not support it. */
    Font getFont();

    /** Changes the font. */
    void setFont(Font f);

    /** That font's metrics, or `null` if it does not support it. */
    FontMetrics getFontMetrics(Font f);

    /** Whether it responds to user input. */
    boolean isEnabled();

    /** Enables or disables it. */
    void setEnabled(boolean b);

    /** Whether it is declared visible. */
    boolean isVisible();

    /** Shows or hides it. */
    void setVisible(boolean b);

    /** Whether it is really seen, counting its ancestors. */
    boolean isShowing();

    /** Whether that point, relative to the object, falls inside. */
    boolean contains(Point p);

    /**
     * Where it is on the screen.
     *
     * @return the point, or `null` if it is not on screen
     */
    Point getLocationOnScreen();

    /** Where it is, relative to its parent. */
    Point getLocation();

    /** Moves it. */
    void setLocation(Point p);

    /** Its rectangle, relative to the parent. */
    Rectangle getBounds();

    /** Changes its rectangle. */
    void setBounds(Rectangle r);

    /** Its size. */
    Dimension getSize();

    /** Changes its size. */
    void setSize(Dimension d);

    /**
     * Which accessible child falls at that point.
     *
     * @return the child, or `null` if none
     */
    Accessible getAccessibleAt(Point p);

    /** Whether it can receive the focus. */
    boolean isFocusTraversable();

    /** Asks for the focus. */
    void requestFocus();

    /** Adds somebody to notify of focus changes. */
    void addFocusListener(FocusListener l);

    /** Removes that listener. */
    void removeFocusListener(FocusListener l);
}
