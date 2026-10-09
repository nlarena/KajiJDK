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
 * A box with two states: ticked or not.
 *
 * <p>On its own it is a check box. Put into a {@link CheckboxGroup} it turns into a radio button,
 * because the group takes care of unticking its sisters. It is the same widget doing two different
 * jobs, which is one of the oddities of AWT.
 *
 * <p>Changing the state with {@link #setState} fires **no** event. The events belong to the user's
 * interaction, not to the program: if a `setState` reported, a listener that answers by setting
 * another box would build a cascade.
 */
public class Checkbox extends Component implements ItemSelectable, Accessible {

    private static final long serialVersionUID = 7270714317450821763L;

    private static int checkboxCounter = 0;

    /** The caption. */
    String label;

    /** Whether it is ticked. */
    boolean state;

    /** The group it belongs to, or `null` if it is a loose box. */
    CheckboxGroup group;

    /** The listeners, chained. */
    transient ItemListener itemListener;

    /**
     * Changes the state without going through the group.
     *
     * <p>It is what {@link CheckboxGroup} uses to untick the sister: if it called {@link #setState}
     * it would come back into the group and keep going round.
     */
    void setStateInternal(boolean state) {
        this.state = state;
    }

    /** A box without a caption, unticked and with no group. */
    public Checkbox() throws HeadlessException {
        this("", false, null);
    }

    /** A box with that caption, unticked. */
    public Checkbox(String label) throws HeadlessException {
        this(label, false, null);
    }

    /** A box with that caption and that state. */
    public Checkbox(String label, boolean state) throws HeadlessException {
        this(label, state, null);
    }

    /** A box with that caption and that state, inside that group. */
    public Checkbox(String label, boolean state, CheckboxGroup group) throws HeadlessException {
        this.label = label;
        this.state = state;
        this.group = group;
        if (state && group != null) {
            group.setSelectedCheckbox(this);
        }
    }

    /**
     * The same, with the last two arguments the other way round.
     *
     * <p>Both constructors exist because nobody remembers which one goes first.
     */
    public Checkbox(String label, CheckboxGroup group, boolean state) throws HeadlessException {
        this(label, state, group);
    }

    String constructComponentName() {
        synchronized (Checkbox.class) {
            String n = "checkbox" + checkboxCounter;
            checkboxCounter = checkboxCounter + 1;
            return n;
        }
    }

    /** Declares it showable. */
    public void addNotify() {
        super.addNotify();
    }

    /**
     * The caption.
     *
     * @return the caption, or `null` if it has none
     */
    public String getLabel() {
        return this.label;
    }

    /** Changes the caption. */
    public void setLabel(String label) {
        boolean changed;
        synchronized (this) {
            changed = label != this.label && (this.label == null || !this.label.equals(label));
            if (changed) {
                this.label = label;
            }
        }
        if (changed) {
            this.invalidate();
        }
    }

    /** Whether it is ticked. */
    public boolean getState() {
        return this.state;
    }

    /**
     * Ticks it or unticks it.
     *
     * <p>Unticking the **ticked box of a group does nothing**: the group cannot be left empty from
     * here, just as it cannot be left empty from the interface. The only way to empty it is {@link
     * CheckboxGroup#setSelectedCheckbox CheckboxGroup.setSelectedCheckbox(null)}, which is the
     * group's method and not the box's.
     */
    public void setState(boolean state) {
        CheckboxGroup g = this.group;
        if (g != null) {
            if (state) {
                g.setSelectedCheckbox(this);
                return;
            }
            if (g.getSelectedCheckbox() == this) {
                state = true;
            }
        }
        this.setStateInternal(state);
    }

    /**
     * What is selected.
     *
     * @return an array with the caption if it is ticked, or `null` if not
     */
    public Object[] getSelectedObjects() {
        if (!this.state) {
            return null;
        }
        Object[] items = new Object[1];
        items[0] = this.label;
        return items;
    }

    /**
     * The group it belongs to.
     *
     * @return the group, or `null` if it is a loose box
     */
    public CheckboxGroup getCheckboxGroup() {
        return this.group;
    }

    /**
     * Moves it to another group.
     *
     * <p>On entering a group the box is **unticked**, unless the group had nothing ticked.
     * Otherwise, entering would break the group's only rule.
     */
    public void setCheckboxGroup(CheckboxGroup g) {
        CheckboxGroup previous;
        synchronized (this) {
            previous = this.group;
            if (previous == g) {
                return;
            }
            this.group = g;
        }
        if (previous != null && previous.getSelectedCheckbox() == this) {
            previous.setSelectedCheckbox(null);
        }
        if (g != null) {
            if (g.getSelectedCheckbox() != null) {
                this.state = false;
            } else if (this.state) {
                g.setSelectedCheckbox(this);
            }
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
        String s = super.paramString() + ",label=" + this.label + ",state=" + this.state;
        if (this.group != null) {
            s = s + ",group=" + this.group;
        }
        return s;
    }

    /** The accessibility information of this box. */
    public AccessibleContext getAccessibleContext() {
        if (this.accessibleContext == null) {
            this.accessibleContext = new AccessibleAWTCheckbox();
        }
        return this.accessibleContext;
    }

    /**
     * The accessibility of a box.
     *
     * <p>It reports the `CHECKED` state and offers the action of ticking it, which is what a screen
     * reader needs to operate it. The accessible value is 1 ticked and 0 unticked.
     */
    protected class AccessibleAWTCheckbox extends AccessibleAWTComponent
            implements AccessibleAction, AccessibleValue {

        /** For the subclasses. */
        protected AccessibleAWTCheckbox() {
        }

        public AccessibleAction getAccessibleAction() {
            return this;
        }

        public AccessibleValue getAccessibleValue() {
            return this;
        }

        public String getAccessibleName() {
            if (Checkbox.this.getLabel() == null) {
                return super.getAccessibleName();
            }
            return Checkbox.this.getLabel();
        }

        /**
         * A check box, group or no group.
         *
         * <p>It would be tempting to report `RADIO_BUTTON` when it is in a group, because that is
         * what it looks like. The JDK reports `CHECK_BOX` always, and it was checked: changing it
         * would make a screen reader announce it differently than with real AWT.
         */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.CHECK_BOX;
        }

        public AccessibleStateSet getAccessibleStateSet() {
            AccessibleStateSet s = super.getAccessibleStateSet();
            if (Checkbox.this.getState()) {
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
            Checkbox.this.setState(!Checkbox.this.getState());
            return true;
        }

        /** 1 if it is ticked, 0 if not. */
        public Number getCurrentAccessibleValue() {
            return Integer.valueOf(Checkbox.this.getState() ? 1 : 0);
        }

        /**
         * Ticks it if the value is not zero.
         *
         * @return `true` if the value was not `null`
         */
        public boolean setCurrentAccessibleValue(Number n) {
            if (n == null) {
                return false;
            }
            Checkbox.this.setState(n.intValue() != 0);
            return true;
        }

        /** Zero: unticked. */
        public Number getMinimumAccessibleValue() {
            return Integer.valueOf(0);
        }

        /** One: ticked. */
        public Number getMaximumAccessibleValue() {
            return Integer.valueOf(1);
        }
    }
}
