package java.awt;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.EventListener;
import java.util.Vector;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleAction;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;

/**
 * A drop-down list: it shows one line and unfolds the rest when pressed.
 *
 * <p>There is always **exactly one** selected while there is anything in the list, and there is no
 * going back to a state with no selection. Adding the first entry selects it by itself.
 *
 * <p>As in {@link Checkbox}, changing the selection from a program with {@link #select} fires no
 * events: those belong to the user's interaction.
 */
public class Choice extends Component implements ItemSelectable, Accessible {

    private static final long serialVersionUID = -4075310674757313071L;

    private static int choiceCounter = 0;

    /** The entries. */
    Vector<String> pItems = new Vector<String>();

    /** Which one is selected, or -1 if the list is empty. */
    int selectedIndex = -1;

    /** The listeners, chained. */
    transient ItemListener itemListener;

    /** An empty list. */
    public Choice() throws HeadlessException {
    }

    String constructComponentName() {
        synchronized (Choice.class) {
            String n = "choice" + choiceCounter;
            choiceCounter = choiceCounter + 1;
            return n;
        }
    }

    /** Declares it showable. */
    public void addNotify() {
        super.addNotify();
    }

    /** How many entries it has. */
    public int getItemCount() {
        return this.pItems.size();
    }

    /**
     * How many entries it has.
     *
     * @deprecated it is from the 1.0 naming. Use {@link #getItemCount}.
     */
    @Deprecated
    public int countItems() {
        return this.getItemCount();
    }

    /**
     * The entry at that position.
     *
     * @throws ArrayIndexOutOfBoundsException if there is no such position
     */
    public String getItem(int index) {
        return this.getItemImpl(index);
    }

    final String getItemImpl(int index) {
        return this.pItems.elementAt(index);
    }

    /**
     * Adds an entry at the end.
     *
     * <p>The first one ends up selected: a drop-down list cannot be left with no selection.
     *
     * @throws NullPointerException if the entry is `null`
     */
    public void add(String item) {
        synchronized (this) {
            this.insertAt(item, this.pItems.size());
        }
    }

    /**
     * Adds an entry at the end.
     *
     * @deprecated it is from the 1.0 naming. Use {@link #add(String)}.
     */
    @Deprecated
    public void addItem(String item) {
        this.add(item);
    }

    /**
     * Inserts an entry at that position.
     *
     * <p>If the inserted one lands at the position of the selected one or before it, **the
     * selection moves to the first one**. It sounds arbitrary and it is, but it is what AWT does
     * and changing it would be lying: the caller who inserted above the selection finds the first
     * one selected, not the one they had shifted by one place.
     *
     * @throws IllegalArgumentException if the position is negative
     */
    public void insert(String item, int index) {
        synchronized (this) {
            if (index < 0) {
                throw new IllegalArgumentException("index less than zero.");
            }
            int i = Math.min(index, this.pItems.size());
            this.insertAt(item, i);
            if (this.selectedIndex < 0 || this.selectedIndex >= i) {
                this.select(0);
            }
        }
    }

    /** Puts the entry in and selects the first one if it was the only one. */
    private void insertAt(String item, int index) {
        if (item == null) {
            throw new NullPointerException("cannot add null item to Choice");
        }
        this.pItems.insertElementAt(item, index);
        if (this.selectedIndex < 0) {
            this.select(0);
        }
    }

    /**
     * Removes the first entry that says that.
     *
     * @throws IllegalArgumentException if there is none that says that
     */
    public void remove(String item) {
        synchronized (this) {
            int i = this.pItems.indexOf(item);
            if (i < 0) {
                throw new IllegalArgumentException("item " + item + " not found in choice");
            }
            this.remove(i);
        }
    }

    /**
     * Removes the entry at that position.
     *
     * <p>Removing the selected one moves the selection to the first one left; if none is left, the
     * list ends up with no selection, which is the only case where that can happen.
     *
     * @throws IndexOutOfBoundsException if there is no such position
     */
    public void remove(int position) {
        synchronized (this) {
            this.pItems.removeElementAt(position);
            if (this.pItems.isEmpty()) {
                this.selectedIndex = -1;
            } else if (this.selectedIndex == position) {
                this.select(0);
            } else if (this.selectedIndex > position) {
                this.select(this.selectedIndex - 1);
            }
        }
    }

    /** Empties the list. */
    public void removeAll() {
        synchronized (this) {
            this.pItems.removeAllElements();
            this.selectedIndex = -1;
        }
    }

    /**
     * What the selected entry says.
     *
     * @return the text, or `null` if the list is empty
     */
    public synchronized String getSelectedItem() {
        if (this.selectedIndex < 0) {
            return null;
        }
        return this.getItem(this.selectedIndex);
    }

    /**
     * What is selected.
     *
     * @return an array of one element, or `null` if the list is empty
     */
    public synchronized Object[] getSelectedObjects() {
        if (this.selectedIndex < 0) {
            return null;
        }
        Object[] items = new Object[1];
        items[0] = this.getItem(this.selectedIndex);
        return items;
    }

    /**
     * Which position is selected.
     *
     * @return the position, or -1 if the list is empty
     */
    public int getSelectedIndex() {
        return this.selectedIndex;
    }

    /**
     * Selects that position.
     *
     * @throws IllegalArgumentException if there is no such position
     */
    public synchronized void select(int pos) {
        if (pos >= this.pItems.size() || pos < 0) {
            throw new IllegalArgumentException("illegal Choice item position: " + pos);
        }
        if (!this.pItems.isEmpty()) {
            this.selectedIndex = pos;
        }
    }

    /**
     * Selects the first entry that says that.
     *
     * <p>If there is none nothing happens, and that is right: the previous selection is still
     * valid.
     */
    public synchronized void select(String str) {
        int i = this.pItems.indexOf(str);
        if (i >= 0) {
            this.select(i);
        }
    }

    /** Adds a listener; `null` does nothing. */
    public synchronized void addItemListener(ItemListener l) {
        if (l == null) {
            return;
        }
        this.itemListener = AWTEventMulticaster.add(this.itemListener, l);
        this.enableEvents(AWTEvent.ITEM_EVENT_MASK);
    }

    /** Removes a listener. */
    public synchronized void removeItemListener(ItemListener l) {
        if (l == null) {
            return;
        }
        this.itemListener = AWTEventMulticaster.remove(this.itemListener, l);
    }

    /** The listeners that are set. */
    public synchronized ItemListener[] getItemListeners() {
        return AWTEventMulticaster.getListeners(this.itemListener, ItemListener.class);
    }

    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        if (listenerType == ItemListener.class) {
            return AWTEventMulticaster.getListeners(this.itemListener, listenerType);
        }
        return super.getListeners(listenerType);
    }

    protected void processEvent(AWTEvent e) {
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

    protected String paramString() {
        return super.paramString() + ",current=" + this.getSelectedItem();
    }

    /** The accessibility information of this list. */
    public AccessibleContext getAccessibleContext() {
        if (this.accessibleContext == null) {
            this.accessibleContext = new AccessibleAWTChoice();
        }
        return this.accessibleContext;
    }

    /**
     * The accessibility of a drop-down list.
     *
     * <p>It implements {@link AccessibleAction} but reports **zero** actions, which is what the JDK
     * does: the action would be unfolding the list, and that is done by the system's widget.
     * Declaring one that cannot be run would be worse than declaring none.
     *
     * <p>It does **not** implement {@link javax.accessibility.AccessibleSelection}, and that is not
     * an oversight: the JDK does not do it either, so {@code getAccessibleSelection()} returns
     * `null`. Adding it would be more useful and would be diverging.
     */
    protected class AccessibleAWTChoice extends AccessibleAWTComponent
            implements AccessibleAction {

        /** For the subclasses. */
        protected AccessibleAWTChoice() {
        }

        public AccessibleAction getAccessibleAction() {
            return this;
        }

        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.COMBO_BOX;
        }

        /** None: see the class note. */
        public int getAccessibleActionCount() {
            return 0;
        }

        /**
         * What that action is called.
         *
         * @return `null` always: there is none
         */
        public String getAccessibleActionDescription(int i) {
            return null;
        }

        /**
         * Runs that action.
         *
         * @return `false` always, for the same reason
         */
        public boolean doAccessibleAction(int i) {
            return false;
        }

    }
}
