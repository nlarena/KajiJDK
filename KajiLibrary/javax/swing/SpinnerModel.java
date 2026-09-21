package javax.swing;

import javax.swing.event.ChangeListener;

/**
 * The sequence a {@link JSpinner} moves along.
 *
 * <h2>A sequence, not a range</h2>
 *
 * <p>The model does not say how many elements there are nor allow going to number
 * such-and-such: it only knows which is the current value, which comes afterwards and which came
 * before. With that it is enough for a control that only has two arrows, and along the way it
 * allows infinite sequences -- a date, a number with no cap -- that could not be enumerated.
 *
 * <h2>Null means it is over</h2>
 *
 * <p>{@link #getNextValue} and {@link #getPreviousValue} return null when there is no next or
 * previous. It is what the control uses in order to switch an arrow off. It is not an error: it
 * is the end of the sequence.
 *
 * <h2>The value may go outside</h2>
 *
 * <p>{@link #setValue} accepts whatever it is given as long as it is of the type the model
 * understands, even though it ends up outside the bounds. Clipping silently would hide the
 * mistake of whoever set it; the control learns about it all the same, because from there the
 * arrows return null.
 */
public interface SpinnerModel {

    /** The current value. */
    Object getValue();

    /**
     * It changes the value.
     *
     * @throws IllegalArgumentException if the model does not understand that value.
     */
    void setValue(Object value);

    /** The next one, or null if there is none; see the interface note. */
    Object getNextValue();

    /** The previous one, or null if there is none. */
    Object getPreviousValue();

    /** It listens to the value changes. */
    void addChangeListener(ChangeListener l);

    void removeChangeListener(ChangeListener l);
}
