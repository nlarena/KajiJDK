package java.awt.event;

import java.awt.AWTEvent;

/**
 * An action was carried out: a button was pressed, an option chosen, Enter given in a field.
 *
 * <p>It is AWT's highest-level event and therefore the most used one. It does not say which key or
 * which button: it says that **what the component wanted to happen happened**, no matter how it was
 * arrived at. The same event comes out of a click, of the space bar or of a keyboard shortcut.
 *
 * <p>The "command" is a string identifying **which** action it was, and it serves so that a single
 * listener can attend to several components without comparing references.
 */
public class ActionEvent extends AWTEvent {

    private static final long serialVersionUID = -7671078796273832149L;

    /** The family's first identifier. */
    public static final int ACTION_FIRST = 1001;

    /** The family's last identifier. */
    public static final int ACTION_LAST = 1001;

    /** The action was carried out. */
    public static final int ACTION_PERFORMED = 1001;

    /** Alt was down. */
    public static final int ALT_MASK = 8;

    /** Control was down. */
    public static final int CTRL_MASK = 2;

    /** Meta was down. */
    public static final int META_MASK = 4;

    /** Shift was down. */
    public static final int SHIFT_MASK = 1;

    private final String actionCommand;
    private final long when;
    private final int modifiers;

    /**
     * With the source, the identifier and the command.
     *
     * @throws IllegalArgumentException if the source is `null`
     */
    public ActionEvent(Object source, int id, String command) {
        this(source, id, command, 0, 0);
    }

    /**
     * Like the previous one, with the modifiers.
     *
     * @throws IllegalArgumentException if the source is `null`
     */
    public ActionEvent(Object source, int id, String command, int modifiers) {
        this(source, id, command, 0, modifiers);
    }

    /**
     * With everything given.
     *
     * @throws IllegalArgumentException if the source is `null`
     */
    public ActionEvent(Object source, int id, String command, long when, int modifiers) {
        super(source, id);
        this.actionCommand = command;
        this.when = when;
        this.modifiers = modifiers;
    }

    /** Which action it was. */
    public String getActionCommand() {
        return this.actionCommand;
    }

    /** When it happened. */
    public long getWhen() {
        return this.when;
    }

    /** Which modifiers were down. */
    public int getModifiers() {
        return this.modifiers;
    }

    public String paramString() {
        String type = this.id == ACTION_PERFORMED ? "ACTION_PERFORMED" : "unknown type";
        return type + ",cmd=" + this.actionCommand + ",when=" + this.when + ",modifiers="
                + java.awt.event.InputEvent.getModifiersExText(this.modifiers);
    }
}
