package java.awt;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventListener;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;

/**
 * One line to type in.
 *
 * <p>It fires an {@link ActionEvent} when the user presses Enter, which is what makes it useful for
 * a search box or a dialog: the field itself reports that the typing is done. Without a windowing
 * system nobody presses anything, so that event only arrives if something posts it.
 *
 * <p>It has a password mode: {@link #setEchoChar} makes it always show the same character instead
 * of what was typed. Mind that **the text is stored all the same**, in the clear, and {@link
 * #getText} returns it as it is: the echo is cosmetic, it is not encryption.
 */
public class TextField extends TextComponent {

    private static final long serialVersionUID = -2966288784432217853L;

    private static int textFieldCounter = 0;

    /** How many letters wide it asks to be. */
    int columns;

    /** What it shows instead of what was typed, or 0 if it shows what was typed. */
    char echoChar;

    /** The action listeners, chained. */
    transient ActionListener actionListener;

    /** An empty field. */
    public TextField() throws HeadlessException {
        this("", 0);
    }

    /** A field with that text, as wide as the text. */
    public TextField(String text) throws HeadlessException {
        this(text, text == null ? 0 : text.length());
    }

    /** An empty field of that width. */
    public TextField(int columns) throws HeadlessException {
        this("", columns);
    }

    /**
     * A field with that text and that width.
     *
     * <p>A negative width is taken as zero instead of breaking: the width is a layout suggestion,
     * not an invariant.
     */
    public TextField(String text, int columns) throws HeadlessException {
        super(text);
        this.columns = Math.max(0, columns);
    }

    String constructComponentName() {
        synchronized (TextField.class) {
            String n = "textfield" + textFieldCounter;
            textFieldCounter = textFieldCounter + 1;
            return n;
        }
    }

    /** Declares it showable. */
    public void addNotify() {
        super.addNotify();
    }

    /**
     * What it shows instead of what was typed.
     *
     * @return the echo character, or 0 if it shows what was typed
     */
    public char getEchoChar() {
        return this.echoChar;
    }

    /**
     * Makes it always show that character.
     *
     * @param c the character, or 0 to go back to showing what is typed
     */
    public void setEchoChar(char c) {
        this.setEchoCharacter(c);
    }

    /**
     * Makes it always show that character.
     *
     * @deprecated it is from the 1.0 naming. Use {@link #setEchoChar}.
     */
    @Deprecated
    public synchronized void setEchoCharacter(char c) {
        this.echoChar = c;
    }

    /**
     * Changes the text.
     *
     * <p>Against what this note used to say, it does not send the caret to the start: it delegates
     * to {@link TextComponent#setText}, which only clamps the selection to the new text. The JDK
     * also replaces the line separators with spaces here —a field is one line— and this one does
     * not.
     */
    public void setText(String t) {
        super.setText(t);
    }

    /** Whether it is in echo mode. */
    public boolean echoCharIsSet() {
        return this.echoChar != 0;
    }

    /** How many letters wide it asks to be. */
    public int getColumns() {
        return this.columns;
    }

    /**
     * Changes the width it asks for.
     *
     * @throws IllegalArgumentException if it is negative
     */
    public void setColumns(int columns) {
        synchronized (this) {
            if (columns < 0) {
                throw new IllegalArgumentException("columns less than zero.");
            }
            this.columns = columns;
        }
    }

    /**
     * What a field of that width would need.
     *
     * <p>It answers the current size and ignores the number of columns: working out what a given
     * number of letters measures needs the font measured on a screen.
     */
    public Dimension getPreferredSize(int columns) {
        return this.getSize();
    }

    /**
     * What a field of that width would need.
     *
     * @deprecated it is from the 1.0 naming. Use {@link #getPreferredSize(int)}.
     */
    @Deprecated
    public Dimension preferredSize(int columns) {
        return this.getPreferredSize(columns);
    }

    public Dimension getPreferredSize() {
        return this.columns > 0 ? this.getPreferredSize(this.columns) : super.getPreferredSize();
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

    /**
     * The minimum for a field of that width.
     *
     * <p>Like {@link #getPreferredSize(int)}, it answers the current size and ignores the columns.
     */
    public Dimension getMinimumSize(int columns) {
        return this.getSize();
    }

    /**
     * The minimum for that width.
     *
     * @deprecated it is from the 1.0 naming. Use {@link #getMinimumSize(int)}.
     */
    @Deprecated
    public Dimension minimumSize(int columns) {
        return this.getMinimumSize(columns);
    }

    public Dimension getMinimumSize() {
        return this.columns > 0 ? this.getMinimumSize(this.columns) : super.getMinimumSize();
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
        String s = super.paramString();
        if (this.echoChar != 0) {
            s = s + ",echo=" + this.echoChar;
        }
        return s;
    }

    /** The accessibility information of this field. */
    public AccessibleContext getAccessibleContext() {
        if (this.accessibleContext == null) {
            this.accessibleContext = new AccessibleAWTTextField();
        }
        return this.accessibleContext;
    }

    /**
     * The accessibility of a text field.
     *
     * <p>The only thing it adds is the `SINGLE_LINE` state, which already comes from
     * {@link TextComponent.AccessibleAWTTextComponent}: it is one line by definition.
     */
    protected class AccessibleAWTTextField extends AccessibleAWTTextComponent {

        /** For the subclasses. */
        protected AccessibleAWTTextField() {
        }

        public AccessibleStateSet getAccessibleStateSet() {
            AccessibleStateSet s = super.getAccessibleStateSet();
            s.add(AccessibleState.SINGLE_LINE);
            return s;
        }
    }
}
