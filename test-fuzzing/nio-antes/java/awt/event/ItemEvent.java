package java.awt.event;

import java.awt.AWTEvent;
import java.awt.ItemSelectable;

/**
 * An item was chosen or stopped being chosen.
 *
 * <p>It brings **the item** and whether it ended up chosen or not, instead of bringing the whole
 * list. That is what makes attending to the event cheap in a list of thousands: what changed is
 * reported, not what there is.
 */
public class ItemEvent extends AWTEvent {

    private static final long serialVersionUID = -608708132447206933L;

    /** The item stopped being chosen. */
    public static final int DESELECTED = 2;

    /** The family's first identifier. */
    public static final int ITEM_FIRST = 701;

    /** The family's last identifier. */
    public static final int ITEM_LAST = 701;

    /** What is chosen changed. */
    public static final int ITEM_STATE_CHANGED = 701;

    /** The item ended up chosen. */
    public static final int SELECTED = 1;

    private final Object item;
    private final int stateChange;

    /**
     * With the source, the item and what it ended up as.
     *
     * @throws IllegalArgumentException if the source is `null`
     */
    public ItemEvent(ItemSelectable source, int id, Object item, int stateChange) {
        super(source, id);
        this.item = item;
        this.stateChange = stateChange;
    }

    /** Where it came from. */
    public ItemSelectable getItemSelectable() {
        return (ItemSelectable) this.source;
    }

    /** Which item changed. */
    public Object getItem() {
        return this.item;
    }

    /** {@link #SELECTED} or {@link #DESELECTED}. */
    public int getStateChange() {
        return this.stateChange;
    }

    public String paramString() {
        String type = this.id == ITEM_STATE_CHANGED ? "ITEM_STATE_CHANGED" : "unknown type";
        String state;
        if (this.stateChange == SELECTED) {
            state = "SELECTED";
        } else if (this.stateChange == DESELECTED) {
            state = "DESELECTED";
        } else {
            state = "unknown type";
        }
        return type + ",item=" + this.item + ",stateChange=" + state;
    }
}
