package java.awt.event;

import java.awt.Component;

/**
 * The root of the input events: keyboard and mouse.
 *
 * <p>What it adds is **when** it happened and **which modifiers** were down, which is what the two
 * have in common and nobody else needs.
 *
 * <p>The modifiers are there twice and it is worth understanding why. The old masks
 * ({@code SHIFT_MASK} and company) mixed the state of the keys and that of the mouse into a single
 * number in a way that made it impossible to tell "button 1 is down" from "Alt is down" in some
 * combinations. The new ones ({@code SHIFT_DOWN_MASK}) use bits that do not collide and say exactly
 * what was down at the moment of the event. The old ones are kept because they have been in the API
 * since 1.0.
 *
 * <p>{@link #consume} is overridden here to become public: in an input event, consuming is something
 * the application does on purpose so that the system does not give a key or a click its default
 * treatment.
 */
public abstract class InputEvent extends ComponentEvent {

    private static final long serialVersionUID = -2482525981698309786L;

    /** Alt was down. */
    public static final int ALT_DOWN_MASK = 512;

    /** AltGr was down. */
    public static final int ALT_GRAPH_DOWN_MASK = 8192;

    /** AltGr, in the old encoding. */
    public static final int ALT_GRAPH_MASK = 32;

    /** Alt, in the old encoding. */
    public static final int ALT_MASK = 8;

    /** Button 1 was down. */
    public static final int BUTTON1_DOWN_MASK = 1024;

    /** Button 1, in the old encoding. */
    public static final int BUTTON1_MASK = 16;

    /** Button 2 was down. */
    public static final int BUTTON2_DOWN_MASK = 2048;

    /** Button 2, in the old encoding. */
    public static final int BUTTON2_MASK = 8;

    /** Button 3 was down. */
    public static final int BUTTON3_DOWN_MASK = 4096;

    /** Button 3, in the old encoding. */
    public static final int BUTTON3_MASK = 4;

    /** Control was down. */
    public static final int CTRL_DOWN_MASK = 128;

    /** Control, in the old encoding. */
    public static final int CTRL_MASK = 2;

    /** Meta was down. */
    public static final int META_DOWN_MASK = 256;

    /** Meta, in the old encoding. */
    public static final int META_MASK = 4;

    /** Shift was down. */
    public static final int SHIFT_DOWN_MASK = 64;

    /** Shift, in the old encoding. */
    public static final int SHIFT_MASK = 1;

    /** When it happened, in milliseconds since the epoch. */
    long when;

    /** What was down. */
    int modifiers;

    /** With the component, the identifier, the moment and the modifiers. */
    InputEvent(Component source, int id, long when, int modifiers) {
        super(source, id);
        this.when = when;
        this.modifiers = modifiers;
    }

    /**
     * The mask of button number `button`.
     *
     * @throws IllegalArgumentException if the number is not positive
     */
    public static int getMaskForButton(int button) {
        if (button <= 0) {
            throw new IllegalArgumentException("button doesn\'t exist " + button);
        }
        if (button == 1) {
            return BUTTON1_DOWN_MASK;
        }
        if (button == 2) {
            return BUTTON2_DOWN_MASK;
        }
        if (button == 3) {
            return BUTTON3_DOWN_MASK;
        }
        // The buttons from the fourth on carry on in the high bits, one per button.
        return 1 << (button + 9);
    }

    /**
     * Whether Shift was down.
     *
     * <p>It looks at the **new** mask and not the old one: it is the only one that says
     * unambiguously what was down, and it is what the modern JDK does. An event built with the old
     * masks answers `false` here, and that is right — those masks did not tell keys from buttons.
     */
    public boolean isShiftDown() {
        return (this.modifiers & SHIFT_DOWN_MASK) != 0;
    }

    /** Whether Control was down. */
    public boolean isControlDown() {
        return (this.modifiers & CTRL_DOWN_MASK) != 0;
    }

    /** Whether Meta was down. */
    public boolean isMetaDown() {
        return (this.modifiers & META_DOWN_MASK) != 0;
    }

    /** Whether Alt was down. */
    public boolean isAltDown() {
        return (this.modifiers & ALT_DOWN_MASK) != 0;
    }

    /** Whether AltGr was down. */
    public boolean isAltGraphDown() {
        return (this.modifiers & ALT_GRAPH_DOWN_MASK) != 0;
    }

    /** When it happened. */
    public long getWhen() {
        return this.when;
    }

    /**
     * The modifiers in the old encoding.
     *
     * @deprecated it mixes keys with buttons ambiguously. Use {@link #getModifiersEx}.
     */
    @Deprecated
    public int getModifiers() {
        return this.modifiers & (JDK_1_3_MODIFIERS | HIGH_MODIFIERS);
    }

    /** The modifiers in the new encoding. */
    public int getModifiersEx() {
        return this.modifiers & ~JDK_1_3_MODIFIERS;
    }

    /** The bits the old encoding used. */
    static final int JDK_1_3_MODIFIERS = SHIFT_DOWN_MASK - 1;

    /** The bits reserved for the buttons from the fourth on. */
    static final int HIGH_MODIFIERS = ~((1 << 14) - 1);

    /** Marks that someone took charge. */
    public void consume() {
        this.consumed = true;
    }

    /** Whether someone already took charge. */
    public boolean isConsumed() {
        return this.consumed;
    }

    /** The modifiers written out for a person, such as "Ctrl+Shift". */
    public static String getModifiersExText(int modifiers) {
        StringBuilder sb = new StringBuilder();
        if ((modifiers & META_DOWN_MASK) != 0) {
            sb.append("Meta+");
        }
        if ((modifiers & CTRL_DOWN_MASK) != 0) {
            sb.append("Ctrl+");
        }
        if ((modifiers & ALT_DOWN_MASK) != 0) {
            sb.append("Alt+");
        }
        if ((modifiers & SHIFT_DOWN_MASK) != 0) {
            sb.append("Shift+");
        }
        if ((modifiers & ALT_GRAPH_DOWN_MASK) != 0) {
            sb.append("Alt Graph+");
        }
        for (int b = 1; b <= 3; b++) {
            if ((modifiers & getMaskForButton(b)) != 0) {
                sb.append("Button").append(b).append("+");
            }
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }
}
