package javax.swing;

import javax.swing.event.ChangeListener;

/**
 * A chosen index, or none.
 *
 * <h2>When this is enough</h2>
 *
 * <p>An open tab, a highlighted menu option, a visible pane. Everything that has exactly one
 * chosen thing uses this model and not {@link ListSelectionModel}, which knows about ranges and
 * about anchor and lead: those three things mean nothing when there can only be one.
 *
 * <p>{@link #isSelected} and {@code getSelectedIndex() != -1} say the same. Both are there
 * because the second forces one to know that -1 is the special value.
 */
public interface SingleSelectionModel {

    /** The chosen index, or -1. */
    int getSelectedIndex();

    /** It chooses that index; with -1 none is left. */
    void setSelectedIndex(int index);

    void clearSelection();

    /** Whether there is one chosen. */
    boolean isSelected();

    void addChangeListener(ChangeListener listener);

    void removeChangeListener(ChangeListener listener);
}
