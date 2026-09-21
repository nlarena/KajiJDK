package javax.swing.event;

import java.util.EventObject;

/**
 * A list's contents changed.
 *
 * <p>It describes a <strong>range</strong> and not an element, because adding ten rows one at a
 * time would fire ten notices and ten repaints. Both indices are inclusive.
 *
 * <p>The distinction between {@link #CONTENTS_CHANGED} and the other two is the one that matters
 * to whoever listens: with the first the number of elements did not change, so repainting is
 * enough; with the others the layout has to be redone.
 */
public class ListDataEvent extends EventObject {

    private static final long serialVersionUID = 2805090815656617888L;

    /** Elements changed, without changing how many there are. */
    public static final int CONTENTS_CHANGED = 0;

    /** Elements were added. */
    public static final int INTERVAL_ADDED = 1;

    /** Elements were removed. */
    public static final int INTERVAL_REMOVED = 2;

    private int type;
    private int index0;
    private int index1;

    /** The indices are kept in order, so that whoever listens does not have to order them. */
    public ListDataEvent(Object source, int type, int index0, int index1) {
        super(source);
        this.type = type;
        this.index0 = Math.min(index0, index1);
        this.index1 = Math.max(index0, index1);
    }

    /** Which of the three kinds of change it was. */
    public int getType() {
        return this.type;
    }

    /** The range's first index. */
    public int getIndex0() {
        return this.index0;
    }

    /** The range's last index, inclusive. */
    public int getIndex1() {
        return this.index1;
    }

    public String toString() {
        return getClass().getName() + "[type=" + String.valueOf(this.type)
                + ",index0=" + String.valueOf(this.index0)
                + ",index1=" + String.valueOf(this.index1) + "]";
    }
}
