package java.awt;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.EventListener;
import java.util.Vector;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleSelection;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;

/**
 * A list of lines where one or several can be chosen.
 *
 * <p>Unlike {@link Choice}, the list shows several lines at once, may have nothing selected, and in
 * multiple mode takes any number. It generates two different events and they are not to be
 * confused: an {@link ItemEvent} when the selection changes, and an {@link ActionEvent} only when
 * the user **double-clicks** or presses Enter, that is, when they confirm.
 *
 * <p><strong>Two deliberate divergences from the JDK</strong>, both for the same reason: there the
 * native widget carries the selection, and here there is no native widget. The JDK, without one,
 * leaves the selection pointing at the **old** lines after inserting or removing in the middle, and
 * leaves several selected after switching to single mode. Those are two states the class itself
 * says do not exist, and that are never seen there because the system widget fixes them. Here the
 * class fixes them: {@link #add(String, int)} and {@link #delItems} shift the selection along with
 * the lines, and {@link #setMultipleMode setMultipleMode(false)} trims it to one.
 */
public class List extends Component implements ItemSelectable, Accessible {

    private static final long serialVersionUID = -3304312411574666869L;

    private static int listCounter = 0;

    /** How many lines a list that did not say how many shows. */
    static final int DEFAULT_VISIBLE_ROWS = 4;

    /** The lines. */
    Vector<String> items = new Vector<String>();

    /** How many lines are seen at once. */
    int rows = 0;

    /** Whether it takes more than one selected. */
    boolean multipleMode = false;

    /** The selected positions, in order. */
    int[] selected = new int[0];

    /** The line that was asked to be kept in view, or -1. */
    int visibleIndex = -1;

    /** The action listeners, chained. */
    transient ActionListener actionListener;

    /** The selection ones. */
    transient ItemListener itemListener;

    /** A list of four lines, single selection. */
    public List() throws HeadlessException {
        this(0, false);
    }

    /** A list of that many lines, single selection. */
    public List(int rows) throws HeadlessException {
        this(rows, false);
    }

    /** A list of that many lines, in whichever mode is asked for. */
    public List(int rows, boolean multipleMode) throws HeadlessException {
        this.rows = rows != 0 ? rows : DEFAULT_VISIBLE_ROWS;
        this.multipleMode = multipleMode;
    }

    String constructComponentName() {
        synchronized (List.class) {
            String n = "list" + listCounter;
            listCounter = listCounter + 1;
            return n;
        }
    }

    /** Declares it showable. */
    public void addNotify() {
        super.addNotify();
    }

    /** Declares it no longer showable. */
    public void removeNotify() {
        super.removeNotify();
    }

    /** How many lines it has. */
    public int getItemCount() {
        return this.items.size();
    }

    /**
     * How many lines it has.
     *
     * @deprecated it is from the 1.0 naming. Use {@link #getItemCount}.
     */
    @Deprecated
    public int countItems() {
        return this.getItemCount();
    }

    /**
     * The line at that position.
     *
     * @throws ArrayIndexOutOfBoundsException if there is no such position
     */
    public String getItem(int index) {
        return this.getItemImpl(index);
    }

    final String getItemImpl(int index) {
        return this.items.elementAt(index);
    }

    /** Every line. */
    public synchronized String[] getItems() {
        String[] r = new String[this.items.size()];
        this.items.copyInto(r);
        return r;
    }

    /** Adds a line at the end. */
    public void add(String item) {
        this.add(item, -1);
    }

    /**
     * Adds a line at the end.
     *
     * @deprecated it is from the 1.0 naming. Use {@link #add(String)}.
     */
    @Deprecated
    public void addItem(String item) {
        this.addItem(item, -1);
    }

    /**
     * Inserts a line at that position.
     *
     * @param index where to put it; a negative position or one past the end puts it at the end
     */
    public void add(String item, int index) {
        this.addItem(item, index);
    }

    /**
     * Inserts a line at that position.
     *
     * @deprecated it is from the 1.0 naming. Use {@link #add(String, int)}.
     */
    @Deprecated
    public synchronized void addItem(String item, int index) {
        if (index < -1 || index >= this.items.size()) {
            index = -1;
        }
        if (item == null) {
            item = "";
        }
        if (index == -1) {
            this.items.addElement(item);
        } else {
            this.items.insertElementAt(item, index);
            this.shiftSelection(index, 1);
        }
    }

    /**
     * Changes what the line at that position says.
     *
     * <p>Removing it and putting it back **loses the selection** of that line, and it is what the
     * JDK does.
     *
     * @throws ArrayIndexOutOfBoundsException if there is no such position
     */
    public synchronized void replaceItem(String newValue, int index) {
        this.remove(index);
        this.add(newValue, index);
    }

    /** Empties the list. */
    public void removeAll() {
        synchronized (this) {
            this.items.removeAllElements();
            this.selected = new int[0];
        }
    }

    /**
     * Empties the list.
     *
     * @deprecated it is from the 1.0 naming. Use {@link #removeAll}.
     */
    @Deprecated
    public synchronized void clear() {
        this.removeAll();
    }

    /**
     * Removes the first line that says that.
     *
     * @throws IllegalArgumentException if there is none that says that
     */
    public synchronized void remove(String item) {
        int i = this.items.indexOf(item);
        if (i < 0) {
            throw new IllegalArgumentException("item " + item + " not found in list");
        }
        this.remove(i);
    }

    /**
     * Removes the line at that position.
     *
     * @throws ArrayIndexOutOfBoundsException if there is no such position
     */
    public void remove(int position) {
        this.delItem(position);
    }

    /**
     * Removes the line at that position.
     *
     * @deprecated it is from the 1.0 naming. Use {@link #remove(int)}.
     */
    @Deprecated
    public void delItem(int position) {
        this.delItems(position, position);
    }

    /**
     * Which line is selected.
     *
     * @return the position, or -1 if there is none or there is more than one
     */
    public synchronized int getSelectedIndex() {
        if (this.selected.length != 1) {
            return -1;
        }
        return this.selected[0];
    }

    /** Which lines are selected. */
    public synchronized int[] getSelectedIndexes() {
        int[] r = new int[this.selected.length];
        System.arraycopy(this.selected, 0, r, 0, this.selected.length);
        return r;
    }

    /**
     * What the selected line says.
     *
     * @return the text, or `null` if there is none or there is more than one
     */
    public synchronized String getSelectedItem() {
        int i = this.getSelectedIndex();
        return i < 0 ? null : this.getItem(i);
    }

    /** What the selected lines say. */
    public synchronized String[] getSelectedItems() {
        String[] r = new String[this.selected.length];
        for (int i = 0; i < this.selected.length; i++) {
            r[i] = this.getItem(this.selected[i]);
        }
        return r;
    }

    /** The same as {@link #getSelectedItems}, as {@link ItemSelectable} asks for. */
    public Object[] getSelectedObjects() {
        return this.getSelectedItems();
    }

    /**
     * Selects that line.
     *
     * <p>In single mode it **replaces** the selection; in multiple mode it adds to it. A position
     * that does not exist is ignored: it is what the JDK does, because the list may have changed
     * between the position being worked out and being used.
     */
    public void select(int index) {
        synchronized (this) {
            if (index < 0 || index >= this.items.size()) {
                return;
            }
            if (this.isIndexSelected(index)) {
                return;
            }
            if (!this.multipleMode) {
                this.selected = new int[1];
                this.selected[0] = index;
                return;
            }
            int[] fresh = new int[this.selected.length + 1];
            int j = 0;
            boolean placed = false;
            for (int i = 0; i < this.selected.length; i++) {
                if (!placed && this.selected[i] > index) {
                    fresh[j] = index;
                    j = j + 1;
                    placed = true;
                }
                fresh[j] = this.selected[i];
                j = j + 1;
            }
            if (!placed) {
                fresh[j] = index;
            }
            this.selected = fresh;
        }
    }

    /** Deselects that line; if it was not selected nothing happens. */
    public synchronized void deselect(int index) {
        if (!this.isIndexSelected(index)) {
            return;
        }
        int[] fresh = new int[this.selected.length - 1];
        int j = 0;
        for (int i = 0; i < this.selected.length; i++) {
            if (this.selected[i] != index) {
                fresh[j] = this.selected[i];
                j = j + 1;
            }
        }
        this.selected = fresh;
    }

    /** Whether that line is selected. */
    public boolean isIndexSelected(int index) {
        int[] sel = this.selected;
        for (int i = 0; i < sel.length; i++) {
            if (sel[i] == index) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether that line is selected.
     *
     * @deprecated it is from the 1.0 naming. Use {@link #isIndexSelected}.
     */
    @Deprecated
    public boolean isSelected(int index) {
        return this.isIndexSelected(index);
    }

    /** How many lines it shows at once. */
    public int getRows() {
        return this.rows;
    }

    /** Whether it takes more than one selected. */
    public boolean isMultipleMode() {
        return this.multipleMode;
    }

    /**
     * Whether it takes more than one selected.
     *
     * @deprecated it is from the 1.0 naming. Use {@link #isMultipleMode}.
     */
    @Deprecated
    public boolean allowsMultipleSelections() {
        return this.multipleMode;
    }

    /**
     * Changes the selection mode.
     *
     * <p>On switching to single with several selected, it keeps the **last** one, which is the one
     * that was marked as the current one in the interface.
     */
    public void setMultipleMode(boolean b) {
        this.setMultipleSelections(b);
    }

    /**
     * Changes the selection mode.
     *
     * @deprecated it is from the 1.0 naming. Use {@link #setMultipleMode}.
     */
    @Deprecated
    public synchronized void setMultipleSelections(boolean b) {
        if (b == this.multipleMode) {
            return;
        }
        this.multipleMode = b;
        if (!b && this.selected.length > 1) {
            int last = this.selected[this.selected.length - 1];
            this.selected = new int[1];
            this.selected[0] = last;
        }
    }

    /**
     * Which line was asked to be kept in view.
     *
     * @return the position, or -1 if nobody asked
     */
    public int getVisibleIndex() {
        return this.visibleIndex;
    }

    /**
     * Asks for that line to be kept in view.
     *
     * <p>Without a screen there is nothing to scroll, but the request is noted and
     * {@link #getVisibleIndex} reports it, which is the only observable thing about this method.
     */
    public synchronized void makeVisible(int index) {
        this.visibleIndex = index;
    }

    /** What a list of that many lines would need. */
    public Dimension getPreferredSize(int rows) {
        return this.measure(rows);
    }

    /**
     * What a list of that many lines would need.
     *
     * @deprecated it is from the 1.0 naming. Use {@link #getPreferredSize(int)}.
     */
    @Deprecated
    public Dimension preferredSize(int rows) {
        return this.getPreferredSize(rows);
    }

    public Dimension getPreferredSize() {
        return this.rows > 0 ? this.getPreferredSize(this.rows) : super.getPreferredSize();
    }

    /**
     * What it needs.
     *
     * @deprecated it is from the 1.0 naming. Use {@link #getPreferredSize()}.
     */
    @Deprecated
    public Dimension preferredSize() {
        return this.getPreferredSize();
    }

    /** The minimum a list of that many lines would need. */
    public Dimension getMinimumSize(int rows) {
        return this.measure(rows);
    }

    /**
     * The minimum for that many lines.
     *
     * @deprecated it is from the 1.0 naming. Use {@link #getMinimumSize(int)}.
     */
    @Deprecated
    public Dimension minimumSize(int rows) {
        return this.getMinimumSize(rows);
    }

    public Dimension getMinimumSize() {
        return this.rows > 0 ? this.getMinimumSize(this.rows) : super.getMinimumSize();
    }

    /**
     * The minimum it needs.
     *
     * @deprecated it is from the 1.0 naming. Use {@link #getMinimumSize()}.
     */
    @Deprecated
    public Dimension minimumSize() {
        return this.getMinimumSize();
    }

    /**
     * How much room those lines take.
     *
     * <p>Without a screen there is no measured typography, so the measure comes from the size
     * already set. It is the same thing {@link Component#getPreferredSize} does and for the same
     * reason: inventing a line height would be inventing a metric that does not exist.
     */
    private Dimension measure(int rows) {
        return this.getSize();
    }

    /** Adds a selection listener; `null` does nothing. */
    public synchronized void addItemListener(ItemListener l) {
        if (l == null) {
            return;
        }
        this.itemListener = AWTEventMulticaster.add(this.itemListener, l);
        this.enableEvents(AWTEvent.ITEM_EVENT_MASK);
    }

    /** Removes a selection listener. */
    public synchronized void removeItemListener(ItemListener l) {
        if (l == null) {
            return;
        }
        this.itemListener = AWTEventMulticaster.remove(this.itemListener, l);
    }

    /** The selection listeners. */
    public synchronized ItemListener[] getItemListeners() {
        return AWTEventMulticaster.getListeners(this.itemListener, ItemListener.class);
    }

    /** Adds an action listener; `null` does nothing. */
    public synchronized void addActionListener(ActionListener l) {
        if (l == null) {
            return;
        }
        this.actionListener = AWTEventMulticaster.add(this.actionListener, l);
        this.enableEvents(AWTEvent.ACTION_EVENT_MASK);
    }

    /** Removes an action listener. */
    public synchronized void removeActionListener(ActionListener l) {
        if (l == null) {
            return;
        }
        this.actionListener = AWTEventMulticaster.remove(this.actionListener, l);
    }

    /** The action listeners. */
    public synchronized ActionListener[] getActionListeners() {
        return AWTEventMulticaster.getListeners(this.actionListener, ActionListener.class);
    }

    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        if (listenerType == ActionListener.class) {
            return AWTEventMulticaster.getListeners(this.actionListener, listenerType);
        }
        if (listenerType == ItemListener.class) {
            return AWTEventMulticaster.getListeners(this.itemListener, listenerType);
        }
        return super.getListeners(listenerType);
    }

    protected void processEvent(AWTEvent e) {
        if (e instanceof ActionEvent) {
            this.processActionEvent((ActionEvent) e);
            return;
        }
        if (e instanceof ItemEvent) {
            this.processItemEvent((ItemEvent) e);
            return;
        }
        super.processEvent(e);
    }

    /** Tells the selection listeners. */
    protected void processItemEvent(ItemEvent e) {
        ItemListener l = this.itemListener;
        if (l != null) {
            l.itemStateChanged(e);
        }
    }

    /** Tells the action listeners. */
    protected void processActionEvent(ActionEvent e) {
        ActionListener l = this.actionListener;
        if (l != null) {
            l.actionPerformed(e);
        }
    }

    protected String paramString() {
        return super.paramString() + ",selected=" + this.getSelectedItem();
    }

    /**
     * Removes a stretch of lines, from end to end inclusive.
     *
     * @deprecated it is for the internal use of the windowing system. Use {@link #remove(int)}.
     */
    @Deprecated
    public synchronized void delItems(int start, int end) {
        for (int i = end; i >= start; i--) {
            this.items.removeElementAt(i);
        }
        this.shiftSelection(start, -(end - start + 1));
    }

    /**
     * Shifts the selected positions when something is put in or taken out in the middle.
     *
     * <p>The ones that fall inside the removed stretch are lost; the ones after it are shifted.
     * Without this the selection would be left pointing at lines other than the ones the user
     * chose, which is worse than losing it.
     */
    private void shiftSelection(int from, int delta) {
        int[] sel = this.selected;
        int[] tmp = new int[sel.length];
        int j = 0;
        for (int i = 0; i < sel.length; i++) {
            if (sel[i] < from) {
                tmp[j] = sel[i];
                j = j + 1;
            } else if (delta > 0) {
                tmp[j] = sel[i] + delta;
                j = j + 1;
            } else if (sel[i] >= from - delta) {
                tmp[j] = sel[i] + delta;
                j = j + 1;
            }
        }
        int[] fresh = new int[j];
        System.arraycopy(tmp, 0, fresh, 0, j);
        this.selected = fresh;
    }

    /** The accessibility information of this list. */
    public AccessibleContext getAccessibleContext() {
        if (this.accessibleContext == null) {
            this.accessibleContext = new AccessibleAWTList();
        }
        return this.accessibleContext;
    }

    /**
     * The accessibility of a list.
     *
     * <p>It reports `MULTISELECTABLE` when that applies and knows how to operate the selection. The
     * lines are not components, so {@link #getAccessibleSelection(int)} returns `null`: lying with
     * a wrapper object would be worse than saying there is none.
     */
    protected class AccessibleAWTList extends AccessibleAWTComponent
            implements AccessibleSelection {

        /** For the subclasses. */
        protected AccessibleAWTList() {
        }

        public AccessibleSelection getAccessibleSelection() {
            return this;
        }

        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.LIST;
        }

        public AccessibleStateSet getAccessibleStateSet() {
            AccessibleStateSet s = super.getAccessibleStateSet();
            if (List.this.isMultipleMode()) {
                s.add(AccessibleState.MULTISELECTABLE);
            }
            return s;
        }

        /** How many lines it has. */
        public int getAccessibleChildrenCount() {
            return List.this.getItemCount();
        }

        /**
         * The line at that position.
         *
         * @return `null` always: the lines are strings, not components
         */
        public Accessible getAccessibleChild(int i) {
            return null;
        }

        /** How many are selected. */
        public int getAccessibleSelectionCount() {
            return List.this.getSelectedIndexes().length;
        }

        /**
         * What is selected.
         *
         * @return `null` always, for the same reason as {@link #getAccessibleChild}
         */
        public Accessible getAccessibleSelection(int i) {
            return null;
        }

        /** Whether that line is selected. */
        public boolean isAccessibleChildSelected(int i) {
            return List.this.isIndexSelected(i);
        }

        /** Selects that line. */
        public void addAccessibleSelection(int i) {
            List.this.select(i);
        }

        /** Deselects it. */
        public void removeAccessibleSelection(int i) {
            List.this.deselect(i);
        }

        /** Deselects everything. */
        public void clearAccessibleSelection() {
            int[] sel = List.this.getSelectedIndexes();
            for (int i = 0; i < sel.length; i++) {
                List.this.deselect(sel[i]);
            }
        }

        /** Selects everything, if the list takes it; in single mode it does nothing. */
        public void selectAllAccessibleSelection() {
            if (!List.this.isMultipleMode()) {
                return;
            }
            int n = List.this.getItemCount();
            for (int i = 0; i < n; i++) {
                List.this.select(i);
            }
        }
    }
}
