package javax.swing;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.Serializable;
import java.util.EventListener;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

/**
 * The usual button model: five bits in an integer and three lists of listeners.
 *
 * <p>The bits go in {@link #stateMask} so that the whole state is read in one go and so that
 * adding one does not change the class's shape. Each change of a bit fires
 * {@link #fireStateChanged}, which is what makes the button repaint itself; besides, releasing
 * while armed fires the action, and selecting or deselecting fires the item event.
 *
 * <p>The {@code ActionEvent}'s modifiers come from the event that is being dispatched -- the
 * mouse or keyboard one that caused the click --, through {@link EventQueue#getCurrentEvent}. A
 * {@code doClick} from a program has no event under way and the modifiers are zero.
 */
public class DefaultButtonModel implements ButtonModel, Serializable {

    /** The state bits; see the constants. */
    protected int stateMask = 0;

    protected String actionCommand = null;

    protected ButtonGroup group = null;

    protected int mnemonic = 0;

    /** The change event, created once: it carries nothing but the source. */
    protected transient ChangeEvent changeEvent = null;

    protected EventListenerList listenerList = new EventListenerList();

    private boolean menuItem = false;

    public static final int ARMED = 1;
    public static final int SELECTED = 1 << 1;
    public static final int PRESSED = 1 << 2;
    public static final int ENABLED = 1 << 3;
    public static final int ROLLOVER = 1 << 4;

    /** An enabled model at rest. */
    public DefaultButtonModel() {
        stateMask = 0;
        setEnabled(true);
    }

    public void setActionCommand(String actionCommand) {
        this.actionCommand = actionCommand;
    }

    public String getActionCommand() {
        return actionCommand;
    }

    public boolean isArmed() {
        return (stateMask & ARMED) != 0;
    }

    public boolean isSelected() {
        return (stateMask & SELECTED) != 0;
    }

    public boolean isEnabled() {
        return (stateMask & ENABLED) != 0;
    }

    public boolean isPressed() {
        return (stateMask & PRESSED) != 0;
    }

    public boolean isRollover() {
        return (stateMask & ROLLOVER) != 0;
    }

    /**
     * It arms or disarms; a disabled model does not arm.
     *
     * <p>The JDK lets a disabled menu item arm itself when the look and feel asks for it
     * ({@code MenuItem.disabledAreNavigable}), so that the keyboard can pass through it. With no
     * {@code UIManager} to say so, the rule is the same for all: disabled does not arm.
     */
    public void setArmed(boolean b) {
        if (isArmed() == b || !isEnabled()) {
            return;
        }
        if (b) {
            stateMask = stateMask | ARMED;
        } else {
            stateMask = stateMask & ~ARMED;
        }
        fireStateChanged();
    }

    /** It enables or disables; disabling also disarms and releases. */
    public void setEnabled(boolean b) {
        if (isEnabled() == b) {
            return;
        }
        if (b) {
            stateMask = stateMask | ENABLED;
        } else {
            stateMask = stateMask & ~ENABLED;
            stateMask = stateMask & ~ARMED;
            stateMask = stateMask & ~PRESSED;
        }
        fireStateChanged();
    }

    /** It selects or deselects, giving notice to the item and change listeners. */
    public void setSelected(boolean b) {
        if (isSelected() == b) {
            return;
        }
        if (b) {
            stateMask = stateMask | SELECTED;
        } else {
            stateMask = stateMask & ~SELECTED;
        }
        fireItemStateChanged(new ItemEvent(this, ItemEvent.ITEM_STATE_CHANGED, this,
                b ? ItemEvent.SELECTED : ItemEvent.DESELECTED));
        fireStateChanged();
    }

    /**
     * It presses or releases; releasing while armed fires the action.
     *
     * <p>It is the heart of the click: the action does not come out on pressing but on releasing,
     * and only if the model is still armed, which is what is lost on taking the mouse off the
     * button.
     */
    public void setPressed(boolean b) {
        if (isPressed() == b || !isEnabled()) {
            return;
        }
        if (b) {
            stateMask = stateMask | PRESSED;
        } else {
            stateMask = stateMask & ~PRESSED;
        }
        if (!isPressed() && isArmed()) {
            int modifiers = 0;
            AWTEvent current = EventQueue.getCurrentEvent();
            if (current instanceof InputEvent) {
                modifiers = ((InputEvent) current).getModifiers();
            } else if (current instanceof ActionEvent) {
                modifiers = ((ActionEvent) current).getModifiers();
            }
            fireActionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
                    getActionCommand(), EventQueue.getMostRecentEventTime(), modifiers));
        }
        fireStateChanged();
    }

    /** The cursor came in or went out; a disabled model does not learn about it. */
    public void setRollover(boolean b) {
        if (isRollover() == b || !isEnabled()) {
            return;
        }
        if (b) {
            stateMask = stateMask | ROLLOVER;
        } else {
            stateMask = stateMask & ~ROLLOVER;
        }
        fireStateChanged();
    }

    public void setMnemonic(int key) {
        mnemonic = key;
        fireStateChanged();
    }

    public int getMnemonic() {
        return mnemonic;
    }

    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }

    public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }

    public ChangeListener[] getChangeListeners() {
        return listenerList.getListeners(ChangeListener.class);
    }

    /** It gives notice that some bit changed; the event is created the first time and reused. */
    protected void fireStateChanged() {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == ChangeListener.class) {
                if (changeEvent == null) {
                    changeEvent = new ChangeEvent(this);
                }
                ((ChangeListener) listeners[i + 1]).stateChanged(changeEvent);
            }
        }
    }

    public void addActionListener(ActionListener l) {
        listenerList.add(ActionListener.class, l);
    }

    public void removeActionListener(ActionListener l) {
        listenerList.remove(ActionListener.class, l);
    }

    public ActionListener[] getActionListeners() {
        return listenerList.getListeners(ActionListener.class);
    }

    protected void fireActionPerformed(ActionEvent e) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == ActionListener.class) {
                ((ActionListener) listeners[i + 1]).actionPerformed(e);
            }
        }
    }

    public void addItemListener(ItemListener l) {
        listenerList.add(ItemListener.class, l);
    }

    public void removeItemListener(ItemListener l) {
        listenerList.remove(ItemListener.class, l);
    }

    public ItemListener[] getItemListeners() {
        return listenerList.getListeners(ItemListener.class);
    }

    protected void fireItemStateChanged(ItemEvent e) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == ItemListener.class) {
                ((ItemListener) listeners[i + 1]).itemStateChanged(e);
            }
        }
    }

    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        return listenerList.getListeners(listenerType);
    }

    /** None: a model does not know what object it represents; the button knows that. */
    public Object[] getSelectedObjects() {
        return null;
    }

    public void setGroup(ButtonGroup group) {
        this.group = group;
    }

    public ButtonGroup getGroup() {
        return group;
    }

    /** Whether this model belongs to a menu item; see {@link #setArmed}. */
    public boolean isMenuItem() {
        return menuItem;
    }

    public void setMenuItem(boolean menuItem) {
        this.menuItem = menuItem;
    }
}
