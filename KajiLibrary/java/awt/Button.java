package java.awt;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventListener;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleAction;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;

/**
 * A button: it is pressed and something happens.
 *
 * <p>Pressing it fires an {@link ActionEvent} with a **command**, which is the string that
 * identifies what has to be done. If nobody set one, the command is the button's caption, and there
 * is the classic trap: translating the interface into another language changes the caption and,
 * with it, the command, so the `if` that compared against "OK" stops working. That is why setting
 * the command by hand with {@link #setActionCommand} is better.
 */
public class Button extends Component implements Accessible {

    private static final long serialVersionUID = -8774683716313001058L;

    private static int buttonCounter = 0;

    /** The caption. */
    String label;

    /** The command it sends when pressed, or `null` to use the caption. */
    String actionCommand;

    /** The listeners, chained. */
    transient ActionListener actionListener;

    /** A button without a caption. */
    public Button() throws HeadlessException {
        this("");
    }

    /** A button with that caption. */
    public Button(String label) throws HeadlessException {
        this.label = label;
    }

    String constructComponentName() {
        synchronized (Button.class) {
            String n = "button" + buttonCounter;
            buttonCounter = buttonCounter + 1;
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

    /**
     * Sets the command it sends when pressed.
     *
     * @param command the command, or `null` to go back to using the caption
     */
    public void setActionCommand(String command) {
        this.actionCommand = command;
    }

    /**
     * The command it sends when pressed.
     *
     * @return the command, or the caption if none was set
     */
    public String getActionCommand() {
        return this.actionCommand == null ? this.label : this.actionCommand;
    }

    /** Adds a listener; `null` does nothing. */
    public synchronized void addActionListener(ActionListener l) {
        if (l == null) {
            return;
        }
        this.actionListener = AWTEventMulticaster.add(this.actionListener, l);
        this.enableEvents(AWTEvent.ACTION_EVENT_MASK);
    }

    /** Removes a listener. */
    public synchronized void removeActionListener(ActionListener l) {
        if (l == null) {
            return;
        }
        this.actionListener = AWTEventMulticaster.remove(this.actionListener, l);
    }

    /** The listeners that are set. */
    public synchronized ActionListener[] getActionListeners() {
        return AWTEventMulticaster.getListeners(this.actionListener, ActionListener.class);
    }

    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        if (listenerType == ActionListener.class) {
            return AWTEventMulticaster.getListeners(this.actionListener, listenerType);
        }
        return super.getListeners(listenerType);
    }

    protected void processEvent(AWTEvent e) {
        if (e instanceof ActionEvent) {
            this.processActionEvent((ActionEvent) e);
            return;
        }
        super.processEvent(e);
    }

    /** Tells the action listeners. */
    protected void processActionEvent(ActionEvent e) {
        ActionListener l = this.actionListener;
        if (l != null) {
            l.actionPerformed(e);
        }
    }

    protected String paramString() {
        return super.paramString() + ",label=" + this.label;
    }

    /** The accessibility information of this button. */
    public AccessibleContext getAccessibleContext() {
        if (this.accessibleContext == null) {
            this.accessibleContext = new AccessibleAWTButton();
        }
        return this.accessibleContext;
    }

    /**
     * The accessibility of a button.
     *
     * <p>It offers **one** action, pressing it, and running that fires the same event as a click.
     * That is what lets a screen reader activate the button without a mouse.
     */
    protected class AccessibleAWTButton extends AccessibleAWTComponent
            implements AccessibleAction {

        /** For the subclasses. */
        protected AccessibleAWTButton() {
        }

        public String getAccessibleName() {
            if (Button.this.getLabel() == null) {
                return super.getAccessibleName();
            }
            return Button.this.getLabel();
        }

        public AccessibleAction getAccessibleAction() {
            return this;
        }

        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.PUSH_BUTTON;
        }

        /** Just one: pressing it. */
        public int getAccessibleActionCount() {
            return 1;
        }

        /**
         * What that action is called.
         *
         * @return "click" for 0, `null` for any other
         */
        public String getAccessibleActionDescription(int i) {
            if (i == 0) {
                return "click";
            }
            return null;
        }

        /**
         * Presses the button.
         *
         * @return `true` if the action existed
         */
        public boolean doAccessibleAction(int i) {
            if (i != 0) {
                return false;
            }
            Button.this.processActionEvent(new ActionEvent(Button.this,
                    ActionEvent.ACTION_PERFORMED, Button.this.getActionCommand()));
            return true;
        }
    }
}
