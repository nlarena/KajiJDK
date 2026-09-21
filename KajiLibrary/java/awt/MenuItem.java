package java.awt;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventListener;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;

/**
 * A menu option: a line that can be chosen.
 *
 * <p>When it is chosen it fires an {@link ActionEvent}, just like a button, and for the same
 * reason: for whoever listens, "the user asked to save" is the same whether it comes from the menu,
 * from the toolbar button or from the keyboard shortcut. That is the point of the action being the
 * highest-level event of AWT.
 *
 * <p>The **command** identifies which action it is, and if none is given the label is used. That
 * last part is a known trap: translating the interface changes the label and with it the command,
 * and the code that compared against the English text stops working. That is why setting it
 * explicitly is better.
 *
 * <p>Its constructors declare {@link HeadlessException} like the JDK's and never throw it; see
 * {@link MenuComponent}.
 */
public class MenuItem extends MenuComponent implements Accessible {

    private static final long serialVersionUID = -21757335363267194L;

    private String label;
    private boolean enabled = true;
    private MenuShortcut shortcut;
    private String actionCommand;
    private transient ActionListener actionListener;

    /** Which families of events it asked to receive. */
    long eventMask;

    /** An option without a label. */
    public MenuItem() throws HeadlessException {
        this("", null);
    }

    /** With that label. */
    public MenuItem(String label) throws HeadlessException {
        this(label, null);
    }

    /** With a label and a keyboard shortcut. */
    public MenuItem(String label, MenuShortcut s) throws HeadlessException {
        this.label = label;
        this.shortcut = s;
    }

    /** Notifies that it can be shown. */
    public void addNotify() {
    }

    /** The text of the option. */
    public String getLabel() {
        return this.label;
    }

    /** Changes the text. */
    public synchronized void setLabel(String label) {
        this.label = label;
    }

    /** Whether it can be chosen. */
    public boolean isEnabled() {
        return this.enabled;
    }

    /** Enables or disables it. */
    public synchronized void setEnabled(boolean b) {
        this.enabled = b;
    }

    /**
     * Enables it.
     *
     * @deprecated it is from the 1.0 model. Use {@link #setEnabled}.
     */
    @Deprecated
    public synchronized void enable() {
        this.setEnabled(true);
    }

    /**
     * Enables or disables it.
     *
     * @deprecated it is from the 1.0 model. Use {@link #setEnabled}.
     */
    @Deprecated
    public void enable(boolean b) {
        this.setEnabled(b);
    }

    /**
     * Disables it.
     *
     * @deprecated it is from the 1.0 model. Use {@link #setEnabled}.
     */
    @Deprecated
    public synchronized void disable() {
        this.setEnabled(false);
    }

    /** The keyboard shortcut, or `null`. */
    public MenuShortcut getShortcut() {
        return this.shortcut;
    }

    /** Gives it a keyboard shortcut. */
    public void setShortcut(MenuShortcut s) {
        this.shortcut = s;
    }

    /** Takes its shortcut away. */
    public void deleteShortcut() {
        this.shortcut = null;
    }

    /**
     * Asks to receive those families of events.
     *
     * <p>It is the counterpart of registering a listener: without the mask on, the event is not
     * delivered even if there is someone listening.
     */
    protected final void enableEvents(long eventsToEnable) {
        this.eventMask = this.eventMask | eventsToEnable;
    }

    /** Stops receiving them. */
    protected final void disableEvents(long eventsToDisable) {
        this.eventMask = this.eventMask & ~eventsToDisable;
    }

    /** Changes which action this option identifies. */
    public void setActionCommand(String command) {
        this.actionCommand = command;
    }

    /**
     * Which action it identifies.
     *
     * <p>If none was given, the label — with the trap that it changes when translating.
     */
    public String getActionCommand() {
        if (this.actionCommand == null) {
            return this.label;
        }
        return this.actionCommand;
    }

    /** Adds someone to tell when it is chosen; a `null` is ignored. */
    public synchronized void addActionListener(ActionListener l) {
        if (l == null) {
            return;
        }
        this.actionListener = AWTEventMulticaster.add(this.actionListener, l);
        this.enableEvents(AWTEvent.ACTION_EVENT_MASK);
    }

    /** Removes that listener. */
    public synchronized void removeActionListener(ActionListener l) {
        if (l == null) {
            return;
        }
        this.actionListener = AWTEventMulticaster.remove(this.actionListener, l);
    }

    /** The registered listeners. */
    public synchronized ActionListener[] getActionListeners() {
        return AWTEventMulticaster.getListeners(this.actionListener, ActionListener.class);
    }

    /**
     * The listeners of that class.
     *
     * <p>The {@code T extends EventListener} bound is what keeps the question well posed: a class
     * that is not a listener one cannot be passed without raw types.
     *
     * @throws NullPointerException if the class is `null`
     */
    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        EventListener l = null;
        if (listenerType == ActionListener.class) {
            l = this.actionListener;
        }
        return AWTEventMulticaster.getListeners(l, listenerType);
    }

    /** Dispatches the event to the method that corresponds. */
    protected void processEvent(AWTEvent e) {
        if (e instanceof ActionEvent) {
            this.processActionEvent((ActionEvent) e);
        }
    }

    /** Tells the action listeners. */
    protected void processActionEvent(ActionEvent e) {
        ActionListener listener = this.actionListener;
        if (listener != null) {
            listener.actionPerformed(e);
        }
    }

    public String paramString() {
        String s = ",label=" + this.label;
        if (this.shortcut != null) {
            s = s + ",shortcut=" + this.shortcut;
        }
        return super.paramString() + s;
    }

    /** The accessibility information of this option. */
    public AccessibleContext getAccessibleContext() {
        if (this.accessibleContext == null) {
            this.accessibleContext = new AccessibleAWTMenuItem();
        }
        return this.accessibleContext;
    }

    /** The accessibility of a menu option. */
    protected class AccessibleAWTMenuItem extends AccessibleAWTMenuComponent {

        /** For the subclasses. */
        protected AccessibleAWTMenuItem() {
        }

        /** The label. */
        public String getAccessibleName() {
            return MenuItem.this.getLabel();
        }

        /** It is a menu option. */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.MENU_ITEM;
        }

        /** Enabled or not, which is the only thing that can be known without a screen. */
        public AccessibleStateSet getAccessibleStateSet() {
            AccessibleStateSet s = new AccessibleStateSet();
            if (MenuItem.this.isEnabled()) {
                s.add(AccessibleState.ENABLED);
            }
            return s;
        }
    }
}
