package javax.swing;

import java.io.Serializable;
import java.util.BitSet;
import java.util.EventListener;

import javax.swing.event.EventListenerList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * Which lines are chosen.
 *
 * <h2>A set of bits and two indices</h2>
 *
 * <p>What is chosen goes in a {@link BitSet}: a list of a million lines with two chosen takes
 * up the same as one of a thousand. Two indices are also carried that do not say what is chosen
 * but <em>how</em> it was reached: the anchor is where the selection started and the lead where
 * it is now. With both, dragging the mouse backwards may unmark what it marked forwards.
 *
 * <h2>The notices are gathered</h2>
 *
 * <p>While {@link #setValueIsAdjusting} is switched on, whoever listens knows that the
 * selection is half made and may update nothing until the end. It is what keeps dragging the
 * mouse over a hundred lines from making a hundred queries to a database.
 *
 * <p>Besides, each notice carries the range that changed, not the whole selection. The range is
 * gathered while several changes are made in a row and a single one is sent.
 */
public class DefaultListSelectionModel implements ListSelectionModel, Cloneable, Serializable {

    private static final int MIN = -1;
    private static final int MAX = Integer.MAX_VALUE;

    private int value = MIN;
    private BitSet bits = new BitSet(32);
    private int minIndex = MAX;
    private int maxIndex = MIN;
    private int anchorIndex = -1;
    private int leadIndex = -1;
    private int firstAdjustedIndex = MAX;
    private int lastAdjustedIndex = MIN;
    private boolean isAdjusting = false;
    private int firstChangedIndex = MAX;
    private int lastChangedIndex = MIN;
    private int selectionMode = MULTIPLE_INTERVAL_SELECTION;

    /** Those who listen. */
    protected EventListenerList listenerList = new EventListenerList();

    /** Whether notice has to be given when the anchor or the lead changes. */
    protected boolean leadAnchorNotificationEnabled = true;

    /** A model with nothing chosen. */
    public DefaultListSelectionModel() {
    }

    public int getMinSelectionIndex() {
        return isSelectionEmpty() ? -1 : minIndex;
    }

    public int getMaxSelectionIndex() {
        return maxIndex;
    }

    public boolean getValueIsAdjusting() {
        return isAdjusting;
    }

    public int getSelectionMode() {
        return selectionMode;
    }

    /**
     * How many lines may be chosen at a time.
     *
     * @throws IllegalArgumentException if it is not one of the three modes.
     */
    public void setSelectionMode(int selectionMode) {
        if (selectionMode != SINGLE_SELECTION && selectionMode != SINGLE_INTERVAL_SELECTION
                && selectionMode != MULTIPLE_INTERVAL_SELECTION) {
            throw new IllegalArgumentException("invalid selectionMode");
        }
        int oldMode = this.selectionMode;
        this.selectionMode = selectionMode;
        if (oldMode == selectionMode || isSelectionEmpty()) {
            // With the selection empty there is nothing to shrink, and it must not be touched: the
                        // ends hold the sentinels, and using them as indices would mark a line that
                        // does not exist.
            return;
        }
        if (selectionMode == SINGLE_SELECTION) {
            setSelectionInterval(maxIndex, maxIndex);
        } else if (selectionMode == SINGLE_INTERVAL_SELECTION) {
            setSelectionInterval(minIndex, maxIndex);
        }
    }

    public boolean isSelectedIndex(int index) {
        return ((index < minIndex) || (index > maxIndex)) ? false : bits.get(index);
    }

    public boolean isSelectionEmpty() {
        return (minIndex > maxIndex);
    }

    public void addListSelectionListener(ListSelectionListener l) {
        listenerList.add(ListSelectionListener.class, l);
    }

    public void removeListSelectionListener(ListSelectionListener l) {
        listenerList.remove(ListSelectionListener.class, l);
    }

    public ListSelectionListener[] getListSelectionListeners() {
        return listenerList.getListeners(ListSelectionListener.class);
    }

    /** It gives notice that the selection between those two indices changed. */
    protected void fireValueChanged(int firstIndex, int lastIndex) {
        fireValueChanged(firstIndex, lastIndex, getValueIsAdjusting());
    }

    /** It gives notice that the selection stopped being half made. */
    protected void fireValueChanged(boolean isAdjusting) {
        if (lastChangedIndex == MIN) {
            return;
        }
        int oldFirstChangedIndex = firstChangedIndex;
        int oldLastChangedIndex = lastChangedIndex;
        firstChangedIndex = MAX;
        lastChangedIndex = MIN;
        fireValueChanged(oldFirstChangedIndex, oldLastChangedIndex, isAdjusting);
    }

    protected void fireValueChanged(int firstIndex, int lastIndex, boolean isAdjusting) {
        Object[] listeners = listenerList.getListenerList();
        ListSelectionEvent e = null;
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == ListSelectionListener.class) {
                if (e == null) {
                    e = new ListSelectionEvent(this, firstIndex, lastIndex, isAdjusting);
                }
                ((ListSelectionListener) listeners[i + 1]).valueChanged(e);
            }
        }
    }

    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        return listenerList.getListeners(listenerType);
    }

    /** Whether moving the anchor or the lead counts as a change notice has to be given of. */
    public void setLeadAnchorNotificationEnabled(boolean flag) {
        leadAnchorNotificationEnabled = flag;
    }

    public boolean isLeadAnchorNotificationEnabled() {
        return leadAnchorNotificationEnabled;
    }

    /**
     * It leaves the selection empty.
     *
     * <p>It moves neither the anchor nor the lead. It looks like an omission and it is not: both
     * say where the user was coming from, and erasing what is chosen does not erase that walk. It
     * is what allows pressing Escape and then Shift+arrow to go on extending from where it was.
     */
    public void clearSelection() {
        removeRange(minIndex, maxIndex, false);
    }

    /** It leaves only that range chosen. */
    public void setSelectionInterval(int index0, int index1) {
        if (index0 == -1 || index1 == -1) {
            return;
        }
        if (getSelectionMode() == SINGLE_SELECTION) {
            index0 = index1;
        }
        updateLeadAnchorIndices(index0, index1);
        int clearMin = minIndex;
        int clearMax = maxIndex;
        int setMin = Math.min(index0, index1);
        int setMax = Math.max(index0, index1);
        changeSelection(clearMin, clearMax, setMin, setMax);
    }

    /** It adds that range to what is chosen. */
    public void addSelectionInterval(int index0, int index1) {
        if (index0 == -1 || index1 == -1) {
            return;
        }
        if (getSelectionMode() == SINGLE_SELECTION) {
            setSelectionInterval(index0, index1);
            return;
        }
        updateLeadAnchorIndices(index0, index1);
        int clearMin = MAX;
        int clearMax = MIN;
        int setMin = Math.min(index0, index1);
        int setMax = Math.max(index0, index1);
        changeSelection(clearMin, clearMax, setMin, setMax);
    }

    /** It removes that range from what is chosen. */
    public void removeSelectionInterval(int index0, int index1) {
        removeRange(index0, index1, true);
    }

    /** It removes that range; {@code moveLead} says whether the anchor and the lead also move. */
    private void removeRange(int index0, int index1, boolean moveLead) {
        if (index0 == -1 || index1 == -1) {
            return;
        }
        if (moveLead) {
            updateLeadAnchorIndices(index0, index1);
        }
        int clearMin = Math.min(index0, index1);
        int clearMax = Math.max(index0, index1);
        int setMin = MAX;
        int setMax = MIN;
        changeSelection(clearMin, clearMax, setMin, setMax);
    }

    /**
     * It shifts the selection because lines were inserted.
     *
     * <p>The list calls it when the data model changes. Without this, inserting a line above would
     * leave the one beside the one that was chosen chosen.
     */
    public void insertIndexInterval(int index, int length, boolean before) {
        int insMinIndex = (before) ? index : index + 1;
        int insMaxIndex = (insMinIndex + length) - 1;
        for (int i = maxIndex; i >= insMinIndex; i--) {
            setState(i + length, bits.get(i));
        }
        boolean setInsertedValues = ((getSelectionMode() == SINGLE_SELECTION)
                ? false : bits.get(index));
        for (int i = insMinIndex; i <= insMaxIndex; i++) {
            setState(i, setInsertedValues);
        }
        int leadIndex = this.leadIndex;
        if (leadIndex > index || (before && leadIndex == index)) {
            leadIndex = this.leadIndex + length;
        }
        int anchorIndex = this.anchorIndex;
        if (anchorIndex > index || (before && anchorIndex == index)) {
            anchorIndex = this.anchorIndex + length;
        }
        if (leadIndex != this.leadIndex || anchorIndex != this.anchorIndex) {
            updateLeadAnchorIndices(anchorIndex, leadIndex);
        }
        fireValueChanged();
    }

    /** It shifts the selection because lines were removed. */
    public void removeIndexInterval(int index0, int index1) {
        int rmMinIndex = Math.min(index0, index1);
        int rmMaxIndex = Math.max(index0, index1);
        int gapLength = (rmMaxIndex - rmMinIndex) + 1;
        for (int i = rmMinIndex; i <= maxIndex; i++) {
            setState(i, bits.get(i + gapLength));
        }
        int leadIndex = this.leadIndex;
        if (leadIndex == 0 && rmMinIndex == 0) {
            // It does not move.
        } else if (leadIndex > rmMaxIndex) {
            leadIndex = this.leadIndex - gapLength;
        } else if (leadIndex >= rmMinIndex) {
            leadIndex = rmMinIndex - 1;
        }
        int anchorIndex = this.anchorIndex;
        if (anchorIndex == 0 && rmMinIndex == 0) {
                // Nor does it.
        } else if (anchorIndex > rmMaxIndex) {
            anchorIndex = this.anchorIndex - gapLength;
        } else if (anchorIndex >= rmMinIndex) {
            anchorIndex = rmMinIndex - 1;
        }
        if (leadIndex != this.leadIndex || anchorIndex != this.anchorIndex) {
            updateLeadAnchorIndices(anchorIndex, leadIndex);
        }
        fireValueChanged();
    }

    /** It marks that the selection is half made; see the class note. */
    public void setValueIsAdjusting(boolean isAdjusting) {
        if (isAdjusting != this.isAdjusting) {
            this.isAdjusting = isAdjusting;
            this.fireValueChanged(isAdjusting);
        }
    }

    public String toString() {
        String s = ((getValueIsAdjusting()) ? "~" : "") + bits.toString();
        return getClass().getName() + " " + Integer.toString(hashCode()) + " " + s;
    }

    /** A copy with the same selection and without those who listen. */
    public Object clone() throws CloneNotSupportedException {
        DefaultListSelectionModel clone = (DefaultListSelectionModel) super.clone();
        clone.bits = (BitSet) bits.clone();
        clone.listenerList = new EventListenerList();
        return clone;
    }

    public int getAnchorSelectionIndex() {
        return anchorIndex;
    }

    public int getLeadSelectionIndex() {
        return leadIndex;
    }

    public void setAnchorSelectionIndex(int anchorIndex) {
        updateLeadAnchorIndices(anchorIndex, this.leadIndex);
        fireValueChanged();
    }

    /** It moves the lead without changing what is chosen. */
    public void moveLeadSelectionIndex(int leadIndex) {
        if (leadIndex == -1 && anchorIndex != -1) {
            return;
        }
        if (this.leadIndex == leadIndex) {
            return;
        }
        updateLeadAnchorIndices(anchorIndex, leadIndex);
        fireValueChanged();
    }

    /**
     * It moves the lead dragging the selection from the anchor.
     *
     * <p>It is what happens when dragging the mouse: what is left between the anchor and the new
     * lead takes the anchor's state, and what is left outside goes back to how it was. That way,
     * dragging backwards unmarks.
     */
    public void setLeadSelectionIndex(int leadIndex) {
        int anchorIndex = this.anchorIndex;
        if (getSelectionMode() == SINGLE_SELECTION) {
            setSelectionInterval(leadIndex, leadIndex);
            return;
        }
        if (anchorIndex == -1 || leadIndex == -1) {
            return;
        }
        if (this.leadIndex == -1) {
            this.leadIndex = leadIndex;
        }
        int oldMin = Math.min(this.anchorIndex, this.leadIndex);
        int oldMax = Math.max(this.anchorIndex, this.leadIndex);
        int newMin = Math.min(anchorIndex, leadIndex);
        int newMax = Math.max(anchorIndex, leadIndex);
        updateLeadAnchorIndices(anchorIndex, leadIndex);
        // The two cases are not symmetrical. Dragging from a chosen anchor marks the new and
                // unmarks what was left outside; from an unchosen anchor it is the other way round,
                // and besides the range that is in both must be left UNmarked. Hence the `false`:
                // it says which of the two wins in the part that overlaps.
        if (bits.get(this.anchorIndex)) {
            changeSelection(oldMin, oldMax, newMin, newMax);
        } else {
            changeSelection(newMin, newMax, oldMin, oldMax, false);
        }
    }

    // ---- the inside ----

    private void updateLeadAnchorIndices(int anchorIndex, int leadIndex) {
        if (leadAnchorNotificationEnabled) {
            if (this.anchorIndex != anchorIndex) {
                markAsDirty(this.anchorIndex);
                markAsDirty(anchorIndex);
            }
            if (this.leadIndex != leadIndex) {
                markAsDirty(this.leadIndex);
                markAsDirty(leadIndex);
            }
        }
        this.anchorIndex = anchorIndex;
        this.leadIndex = leadIndex;
    }

    private void markAsDirty(int r) {
        if (r == -1) {
            return;
        }
        firstAdjustedIndex = Math.min(firstAdjustedIndex, r);
        lastAdjustedIndex = Math.max(lastAdjustedIndex, r);
    }

    private void setState(int r, boolean state) {
        if (state) {
            set(r);
        } else {
            clear(r);
        }
    }

    private void set(int r) {
        if (bits.get(r)) {
            return;
        }
        bits.set(r);
        markAsDirty(r);
        minIndex = Math.min(minIndex, r);
        maxIndex = Math.max(maxIndex, r);
    }

    private void clear(int r) {
        if (!bits.get(r)) {
            return;
        }
        bits.clear(r);
        markAsDirty(r);
        // If an end was removed, the new one has to be looked for: the set does not carry it.
        if (r == minIndex) {
            for (minIndex = minIndex + 1; minIndex <= maxIndex; minIndex++) {
                if (bits.get(minIndex)) {
                    break;
                }
            }
        }
        if (r == maxIndex) {
            for (maxIndex = maxIndex - 1; minIndex <= maxIndex; maxIndex--) {
                if (bits.get(maxIndex)) {
                    break;
                }
            }
        }
        if (isSelectionEmpty()) {
            minIndex = MAX;
            maxIndex = MIN;
        }
    }

    private void changeSelection(int clearMin, int clearMax, int setMin, int setMax) {
        changeSelection(clearMin, clearMax, setMin, setMax, true);
    }

    private void changeSelection(int clearMin, int clearMax, int setMin, int setMax,
            boolean clearFirst) {
        for (int i = Math.min(setMin, clearMin); i <= Math.max(setMax, clearMax); i++) {
            boolean shouldClear = contains(clearMin, clearMax, i);
            boolean shouldSet = contains(setMin, setMax, i);
            if (shouldSet && shouldClear) {
                if (clearFirst) {
                    shouldClear = false;
                } else {
                    shouldSet = false;
                }
            }
            if (shouldSet) {
                set(i);
            }
            if (shouldClear) {
                clear(i);
            }
        }
        fireValueChanged();
    }

    private static boolean contains(int a, int b, int i) {
        return (i >= a) && (i <= b);
    }

    /** It sends the notice with the range that was gathered, if there is one. */
    private void fireValueChanged() {
        if (lastAdjustedIndex == MIN) {
            return;
        }
        if (getValueIsAdjusting()) {
            firstChangedIndex = Math.min(firstChangedIndex, firstAdjustedIndex);
            lastChangedIndex = Math.max(lastChangedIndex, lastAdjustedIndex);
        }
        int oldFirstAdjustedIndex = firstAdjustedIndex;
        int oldLastAdjustedIndex = lastAdjustedIndex;
        firstAdjustedIndex = MAX;
        lastAdjustedIndex = MIN;
        fireValueChanged(oldFirstAdjustedIndex, oldLastAdjustedIndex);
    }
}
