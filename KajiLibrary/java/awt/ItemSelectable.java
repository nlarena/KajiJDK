package java.awt;

import java.awt.event.ItemListener;

/**
 * Something with items that can be selected: a list, a drop-down, a check box.
 *
 * <p>What they have in common is not how they look but what they announce: which of their items are
 * selected, and whom to notify when that changes.
 */
public interface ItemSelectable {

    /** The selected items, or `null` if there are none. */
    Object[] getSelectedObjects();

    /** Adds someone to notify of the changes. */
    void addItemListener(ItemListener l);

    /** Removes that listener. */
    void removeItemListener(ItemListener l);
}
