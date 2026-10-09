package javax.swing.event;

import java.util.EventObject;

/**
 * A list's selection changed.
 *
 * <p>The range it carries is <strong>where something may have changed</strong>, not what was left
 * selected. It is a real difference: whoever listens has to ask the model for those rows' final
 * state. The event bounds the work, it does not do it.
 *
 * <p>{@link #getValueIsAdjusting} at {@code true} means more are coming: the user is dragging.
 * Recomputing at every intermediate step is wasted work, and the last event --with the flag off--
 * is the one that counts.
 */
public class ListSelectionEvent extends EventObject {

    private static final long serialVersionUID = 1L;

    private int firstIndex;
    private int lastIndex;
    private boolean isAdjusting;

    public ListSelectionEvent(Object source, int firstIndex, int lastIndex, boolean isAdjusting) {
        super(source);
        this.firstIndex = firstIndex;
        this.lastIndex = lastIndex;
        this.isAdjusting = isAdjusting;
    }

    /** The first row that may have changed. */
    public int getFirstIndex() {
        return this.firstIndex;
    }

    /** The last row that may have changed, inclusive. */
    public int getLastIndex() {
        return this.lastIndex;
    }

    /** Whether more changes are coming. */
    public boolean getValueIsAdjusting() {
        return this.isAdjusting;
    }

    public String toString() {
        return getClass().getName() + "[firstIndex=" + String.valueOf(this.firstIndex)
                + ",lastIndex=" + String.valueOf(this.lastIndex)
                + ",isAdjusting=" + String.valueOf(this.isAdjusting) + "]";
    }
}
