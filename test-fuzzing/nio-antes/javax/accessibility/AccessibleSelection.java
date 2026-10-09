package javax.accessibility;

/**
 * Implemented by what has children that can be **chosen**: a list, a tree, a table, a tabbed pane.
 *
 * <p>The children are named by their number within the parent, not by their number within the
 * selection. It is the numbering the whole package uses and it keeps adding an item from
 * renumbering what was chosen.
 */
public interface AccessibleSelection {

    /** How many children are chosen. */
    int getAccessibleSelectionCount();

    /**
     * The `i`-th chosen child.
     *
     * @return the child, or `null` if there are not that many
     */
    Accessible getAccessibleSelection(int i);

    /** Whether that child is chosen. */
    boolean isAccessibleChildSelected(int i);

    /** Adds that child to the selection. */
    void addAccessibleSelection(int i);

    /** Removes that child from the selection. */
    void removeAccessibleSelection(int i);

    /** Leaves the selection empty. */
    void clearAccessibleSelection();

    /** Chooses all the children, if the object allows it. */
    void selectAllAccessibleSelection();
}
