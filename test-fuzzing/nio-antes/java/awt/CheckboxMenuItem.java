package java.awt;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.EventListener;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleAction;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;
import javax.accessibility.AccessibleValue;

/**
 * A menu entry with a tick: it is turned on and off instead of running something.
 *
 * <p>It is what the "show this or not" options use: show the toolbar, wrap the text to the width,
 * and so on. Unlike a plain {@link MenuItem}, which fires a {@link java.awt.event.ActionEvent} and
 * forgets, this one keeps state and reports with an {@link ItemEvent}.
 *
 * <p>Its constructors declare {@link HeadlessException} like the JDK's and never throw it; see
 * {@link MenuComponent}.
 */
public class CheckboxMenuItem extends MenuItem implements ItemSelectable, Accessible {

    private static final long serialVersionUID = 6190621106981774043L;

    private static int checkboxMenuItemCounter = 0;

    /** Whether it is ticked. */
    private boolean state;

    /** The listeners, chained. */
    private transient ItemListener itemListener;

    /** An entry without a caption and unticked. */
    public CheckboxMenuItem() throws HeadlessException {
        this("", false);
    }

    /** An entry with that caption, unticked. */
    public CheckboxMenuItem(String label) throws HeadlessException {
        this(label, false);
    }

    /** An entry with that caption and that state. */
    public CheckboxMenuItem(String label, boolean state) throws HeadlessException {
        super(label);
        this.state = state;
    }

    String constructComponentName() {
        synchronized (CheckboxMenuItem.class) {
            String n = "chkmenuitem" + checkboxMenuItemCounter;
            checkboxMenuItemCounter = checkboxMenuItemCounter + 1;
            return n;
        }
    }

    /** Declares it showable. */
    public void addNotify() {
        super.addNotify();
    }

    /** Whether it is ticked. */
    public boolean getState() {
        return this.state;
    }

    /** Ticks it or unticks it; it fires no event. */
    public synchronized void setState(boolean b) {
        this.state = b;
    }

    /**
     * What is selected.
     *
     * @return an array with the caption if it is ticked, or `null` if not
     */
    public synchronized Object[] getSelectedObjects() {
        if (!this.state) {
            return null;
        }
        Object[] items = new Object[1];
        items[0] = this.getLabel();
        return items;
    }

    /** Adds a listener; `null` does nothing. */
    public synchronized void addItemListener(ItemListener l) {
        if (l == null) {
            return;
        }
        this.itemListener = AWTEventMulticaster.add(this.itemListener, l);
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

    /**
     * Handles the user choosing the entry.
     *
     * <p>Here the state is flipped **and** reported, because this comes from a user action, which
     * is exactly what {@link #setState} is not.
     */
    void doMenuEvent(long when, int modifiers) {
        this.setState(!this.state);
        this.processItemEvent(new ItemEvent(this, ItemEvent.ITEM_STATE_CHANGED, this.getLabel(),
                this.state ? ItemEvent.SELECTED : ItemEvent.DESELECTED));
    }

    public String paramString() {
        return super.paramString() + ",state=" + this.state;
    }

    /** The accessibility information of this entry. */
    public AccessibleContext getAccessibleContext() {
        if (this.accessibleContext == null) {
            this.accessibleContext = new AccessibleAWTCheckboxMenuItem();
        }
        return this.accessibleContext;
    }

    /** The accessibility of a menu entry with a tick. */
    protected class AccessibleAWTCheckboxMenuItem extends AccessibleAWTMenuItem
            implements AccessibleAction, AccessibleValue {

        /** For the subclasses. */
        protected AccessibleAWTCheckboxMenuItem() {
        }

        public AccessibleAction getAccessibleAction() {
            return this;
        }

        public AccessibleValue getAccessibleValue() {
            return this;
        }

        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.CHECK_BOX;
        }

        public AccessibleStateSet getAccessibleStateSet() {
            AccessibleStateSet s = super.getAccessibleStateSet();
            if (CheckboxMenuItem.this.getState()) {
                s.add(AccessibleState.CHECKED);
            }
            return s;
        }

        /** Just one: flipping it. */
        public int getAccessibleActionCount() {
            return 1;
        }

        public String getAccessibleActionDescription(int i) {
            if (i == 0) {
                return "toggle";
            }
            return null;
        }

        public boolean doAccessibleAction(int i) {
            if (i != 0) {
                return false;
            }
            CheckboxMenuItem.this.doMenuEvent(0, 0);
            return true;
        }

        /** 1 if it is ticked, 0 if not. */
        public Number getCurrentAccessibleValue() {
            return Integer.valueOf(CheckboxMenuItem.this.getState() ? 1 : 0);
        }

        public boolean setCurrentAccessibleValue(Number n) {
            if (n == null) {
                return false;
            }
            CheckboxMenuItem.this.setState(n.intValue() != 0);
            return true;
        }

        public Number getMinimumAccessibleValue() {
            return Integer.valueOf(0);
        }

        public Number getMaximumAccessibleValue() {
            return Integer.valueOf(1);
        }
    }
}
