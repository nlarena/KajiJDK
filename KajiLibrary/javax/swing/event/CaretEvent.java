package javax.swing.event;

import java.util.EventObject;

/**
 * The text cursor moved.
 *
 * <p>It carries <strong>two</strong> positions and not one, and there is the whole content of the
 * class: the <em>dot</em> is where the cursor is and the <em>mark</em> is where the selection
 * started. When they coincide nothing is selected.
 *
 * <p>The mark may be greater than the dot --selecting backwards-- so whoever wants the range has
 * to order them. Always returning the lower one first would lose the direction, which is what
 * decides which way the selection grows if the user goes on dragging.
 */
public abstract class CaretEvent extends EventObject {

    private static final long serialVersionUID = 1L;

    /** @param source whose cursor it is */
    public CaretEvent(Object source) {
        super(source);
    }

    /** Where the cursor is. */
    public abstract int getDot();

    /** Where the selection started; the same as the dot if there is no selection. */
    public abstract int getMark();
}
