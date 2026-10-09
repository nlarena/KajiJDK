package javax.swing;

import javax.swing.event.ChangeListener;

/**
 * A value with a range and a width: what lies behind a scroll bar, a progress bar or a
 * slider.
 *
 * <h2>Four numbers with one rule</h2>
 *
 * <p>Minimum, value, extent and maximum, always in that order:
 *
 * <pre>{@code   minimum <= value <= value + extent <= maximum}</pre>
 *
 * <p>The <em>extent</em> is what is seen at once: in a scroll bar it is the window's height over
 * the document's height, and that is why the thumb has a size. A slider uses an extent of zero,
 * and then the value may reach as far as the maximum.
 *
 * <p>The rule keeps itself: whoever puts in a number that breaks it does not get an error, they
 * get the numbers settled. Raising the minimum above the value drags the value along; shrinking
 * the maximum shrinks the extent first and the value afterwards. It is deliberate: a model that
 * threw exceptions would force every caller to order its changes, and the order depends on which
 * way it moves.
 *
 * <p>{@link #setValueIsAdjusting} marks the changes of a series -- dragging the thumb -- so that
 * whoever listens can wait for the release before doing something expensive.
 */
public interface BoundedRangeModel {

    int getMinimum();

    void setMinimum(int newMinimum);

    int getMaximum();

    void setMaximum(int newMaximum);

    int getValue();

    void setValue(int newValue);

    /** It marks the beginning or the end of a series of changes; see the interface note. */
    void setValueIsAdjusting(boolean b);

    boolean getValueIsAdjusting();

    /** What is seen at once; see the interface note. */
    int getExtent();

    void setExtent(int newExtent);

    /** It changes the four numbers at once, giving notice only once. */
    void setRangeProperties(int value, int extent, int min, int max, boolean adjusting);

    void addChangeListener(ChangeListener x);

    void removeChangeListener(ChangeListener x);
}
