package javax.swing;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.util.Enumeration;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;

import javax.swing.plaf.ButtonUI;
import javax.swing.plaf.basic.BasicToggleButtonUI;

/**
 * A button with two states: each click selects it or deselects it.
 *
 * <p>The whole difference with {@link JButton} is in the model: {@link ToggleButtonModel}
 * changes the selection on releasing, and consults the {@link ButtonGroup} if there is one,
 * which is what makes radio buttons exclude one another. {@link JCheckBox} and
 * {@link JRadioButton} inherit from here and only change the look and feel.
 *
 * <p>The focus, inside a group, goes to the selected one: walking a group of radio buttons with
 * Tab stops at the one that is ticked, not at the first. It is
 * {@link #requestFocus(FocusEvent.Cause)} redirecting the request when the cause is a walk or
 * an activation.
 */
public class JToggleButton extends AbstractButton implements Accessible {

    private static final String uiClassID = "ToggleButtonUI";

    public JToggleButton() {
        this(null, null, false);
    }

    public JToggleButton(Icon icon) {
        this(null, icon, false);
    }

    public JToggleButton(Icon icon, boolean selected) {
        this(null, icon, selected);
    }

    public JToggleButton(String text) {
        this(text, null, false);
    }

    public JToggleButton(String text, boolean selected) {
        this(text, null, selected);
    }

    public JToggleButton(Action a) {
        this();
        setAction(a);
    }

    public JToggleButton(String text, Icon icon) {
        this(text, icon, false);
    }

    public JToggleButton(String text, Icon icon, boolean selected) {
        setModel(new ToggleButtonModel());
        model.setSelected(selected);
        init(text, icon);
    }

    /** It installs the basic look and feel; see {@code JButton#updateUI}. */
    public void updateUI() {
        setUI((ButtonUI) BasicToggleButtonUI.createUI(this));
    }

    public String getUIClassID() {
        return uiClassID;
    }

    /**
     * Yes: a button with state follows its action's selection, and the action follows the button's.
     */
    boolean shouldUpdateSelectedStateFromAction() {
        return true;
    }

    /**
     * Who the focus asked for with that cause goes to: to the group's selected one if the cause
     * is a walk or an activation, and to this button in any other case.
     */
    private JToggleButton groupSelection(FocusEvent.Cause cause) {
        boolean walk = cause == FocusEvent.Cause.ACTIVATION
                || cause == FocusEvent.Cause.TRAVERSAL
                || cause == FocusEvent.Cause.TRAVERSAL_UP
                || cause == FocusEvent.Cause.TRAVERSAL_DOWN
                || cause == FocusEvent.Cause.TRAVERSAL_FORWARD
                || cause == FocusEvent.Cause.TRAVERSAL_BACKWARD;
        if (!walk) {
            return this;
        }
        ButtonGroup group = getModel().getGroup();
        if (group == null) {
            return this;
        }
        ButtonModel selection = group.getSelection();
        if (selection == null || selection == getModel()) {
            return this;
        }
        Enumeration<AbstractButton> members = group.getElements();
        while (members.hasMoreElements()) {
            AbstractButton member = members.nextElement();
            if (member instanceof JToggleButton && member.getModel() == selection) {
                return (JToggleButton) member;
            }
        }
        return this;
    }

    public void requestFocus(FocusEvent.Cause cause) {
        groupSelection(cause).requestFocusNoRedirect(cause);
    }

    private void requestFocusNoRedirect(FocusEvent.Cause cause) {
        super.requestFocus(cause);
    }

    public boolean requestFocusInWindow(FocusEvent.Cause cause) {
        return groupSelection(cause).requestFocusInWindowNoRedirect(cause);
    }

    private boolean requestFocusInWindowNoRedirect(FocusEvent.Cause cause) {
        return super.requestFocusInWindow(cause);
    }

    protected String paramString() {
        return super.paramString();
    }

    /** With no accessibility context: there is no assistive technology that reads it on this VM. */
    public AccessibleContext getAccessibleContext() {
        return null;
    }

    /**
     * The model of a button with state: releasing changes the selection, and the group rules.
     *
     * <p>{@link #setSelected} goes through the group first, if there is one: it is the group that
     * decides what is selected and what is deselected, and this model takes whatever the group
     * says. With no group, it behaves like {@link DefaultButtonModel} save for
     * {@link #setPressed}, which on releasing while armed inverts the selection before firing the
     * action.
     */
    public static class ToggleButtonModel extends DefaultButtonModel {

        public ToggleButtonModel() {
        }

        public boolean isSelected() {
            return (stateMask & SELECTED) != 0;
        }

        public void setSelected(boolean b) {
            ButtonGroup group = getGroup();
            if (group != null) {
                group.setSelected(this, b);
                b = group.isSelected(this);
            }
            if (isSelected() == b) {
                return;
            }
            if (b) {
                stateMask = stateMask | SELECTED;
            } else {
                stateMask = stateMask & ~SELECTED;
            }
            fireStateChanged();
            fireItemStateChanged(new ItemEvent(this, ItemEvent.ITEM_STATE_CHANGED, this,
                    isSelected() ? ItemEvent.SELECTED : ItemEvent.DESELECTED));
        }

        public void setPressed(boolean b) {
            if (isPressed() == b || !isEnabled()) {
                return;
            }
            if (!b && isArmed()) {
                setSelected(!isSelected());
            }
            if (b) {
                stateMask = stateMask | PRESSED;
            } else {
                stateMask = stateMask & ~PRESSED;
            }
            fireStateChanged();
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
        }
    }
}
