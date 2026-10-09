package java.awt;

/**
 * KajiLibrary's java.awt.SystemColor -- the desktop's colours, by name instead of by value.
 *
 * <p>The twenty-six constants are not fixed colours but <b>roles</b>: {@code window} is "the
 * background of a window", not "white". A program that draws with {@code SystemColor.window} and
 * {@code SystemColor.windowText} looks right in a light theme and in a dark one without changing a
 * line; one that writes white and black by hand breaks in the second.
 *
 * <h2>The values are live, and that is why the class is odd</h2>
 *
 * <p>Each constant is a <b>unique object, mutable inside</b>: when the user changes the theme, the
 * same object starts returning another colour. That is why {@code SystemColor.window} can be kept
 * in a field and stay correct, and why {@link #toString()} prints the index and not the value --
 * today's value says nothing about tomorrow's.
 *
 * <p>From that also comes the surprising signature: the constructor is private. This note added
 * that the constants are compared by identity, not by RGB; {@code equals} is inherited from {@link
 * Color} and compares {@code getRGB()}, so two different roles that have the same colour today are
 * equal.
 *
 * <h2>What it returns here</h2>
 *
 * <p><b>The default values</b>: the same ones the JDK hands out when it runs with no desktop
 * ({@code java.awt.headless}). It is not a choice of this library but the only possible one --
 * there is no window system to ask --, and it is exactly what the JDK does in that situation. When
 * there is a toolkit, the table would be updated and the same objects would start answering the
 * theme's colours, without anything already written having to change.
 */
public final class SystemColor extends Color implements java.io.Serializable {

    private static final long serialVersionUID = 4503142729533789064L;

    /** The background of the desktop. */
    public static final int DESKTOP = 0;

    /** The background of the active window's title bar. */
    public static final int ACTIVE_CAPTION = 1;

    /** The text of the active window's title bar. */
    public static final int ACTIVE_CAPTION_TEXT = 2;

    /** The border of the active window's title bar. */
    public static final int ACTIVE_CAPTION_BORDER = 3;

    /** The background of an inactive window's title bar. */
    public static final int INACTIVE_CAPTION = 4;

    /** The text of an inactive window's title bar. */
    public static final int INACTIVE_CAPTION_TEXT = 5;

    /** The border of an inactive window's title bar. */
    public static final int INACTIVE_CAPTION_BORDER = 6;

    /** The background of a window. */
    public static final int WINDOW = 7;

    /** The border of a window. */
    public static final int WINDOW_BORDER = 8;

    /** The text of a window. */
    public static final int WINDOW_TEXT = 9;

    /** The background of a menu. */
    public static final int MENU = 10;

    /** The text of a menu. */
    public static final int MENU_TEXT = 11;

    /** The background of a text field. */
    public static final int TEXT = 12;

    /** The text of a text field. */
    public static final int TEXT_TEXT = 13;

    /** The background of selected text. */
    public static final int TEXT_HIGHLIGHT = 14;

    /** Selected text. */
    public static final int TEXT_HIGHLIGHT_TEXT = 15;

    /** Disabled text. */
    public static final int TEXT_INACTIVE_TEXT = 16;

    /** The background of a control. */
    public static final int CONTROL = 17;

    /** The text of a control. */
    public static final int CONTROL_TEXT = 18;

    /** A control's highlight, on the lit side. */
    public static final int CONTROL_HIGHLIGHT = 19;

    /** A control's light highlight. */
    public static final int CONTROL_LT_HIGHLIGHT = 20;

    /** A control's shadow. */
    public static final int CONTROL_SHADOW = 21;

    /** A control's dark shadow. */
    public static final int CONTROL_DK_SHADOW = 22;

    /** The background of a scroll bar's track. */
    public static final int SCROLLBAR = 23;

    /** The background of a tooltip. */
    public static final int INFO = 24;

    /** The text of a tooltip. */
    public static final int INFO_TEXT = 25;

    /** How many roles there are. It is the length of the table, not a colour. */
    public static final int NUM_COLORS = 26;

    // The live table. It is `static` and mutable on purpose: when there is a toolkit and the theme
    // changes, it would be updated here and the twenty-six constants would start answering the new
    // colours without anyone having to ask for them again. The start-up values are the ones the JDK
    // uses with no desktop; see the class note.
    private static int[] systemColors = {
        0xFF005C5C,  // desktop
        0xFF000080,  // activeCaption
        0xFFFFFFFF,  // activeCaptionText
        0xFFC0C0C0,  // activeCaptionBorder
        0xFF808080,  // inactiveCaption
        0xFFC0C0C0,  // inactiveCaptionText
        0xFFC0C0C0,  // inactiveCaptionBorder
        0xFFFFFFFF,  // window
        0xFF000000,  // windowBorder
        0xFF000000,  // windowText
        0xFFC0C0C0,  // menu
        0xFF000000,  // menuText
        0xFFC0C0C0,  // text
        0xFF000000,  // textText
        0xFF000080,  // textHighlight
        0xFFFFFFFF,  // textHighlightText
        0xFF808080,  // textInactiveText
        0xFFC0C0C0,  // control
        0xFF000000,  // controlText
        0xFFFFFFFF,  // controlHighlight
        0xFFE0E0E0,  // controlLtHighlight
        0xFF808080,  // controlShadow
        0xFF000000,  // controlDkShadow
        0xFFE0E0E0,  // scrollbar
        0xFFE0E000,  // info
        0xFF000000,  // infoText
    };

    /** The background of the desktop. */
    public static final SystemColor desktop = new SystemColor((byte) DESKTOP);

    /** The background of the active window's title bar. */
    public static final SystemColor activeCaption = new SystemColor((byte) ACTIVE_CAPTION);

    /** The text of the active window's title bar. */
    public static final SystemColor activeCaptionText = new SystemColor((byte) ACTIVE_CAPTION_TEXT);

    /** The border of the active window's title bar. */
    public static final SystemColor activeCaptionBorder =
        new SystemColor((byte) ACTIVE_CAPTION_BORDER);

    /** The background of an inactive window's title bar. */
    public static final SystemColor inactiveCaption = new SystemColor((byte) INACTIVE_CAPTION);

    /** The text of an inactive window's title bar. */
    public static final SystemColor inactiveCaptionText =
        new SystemColor((byte) INACTIVE_CAPTION_TEXT);

    /** The border of an inactive window's title bar. */
    public static final SystemColor inactiveCaptionBorder =
        new SystemColor((byte) INACTIVE_CAPTION_BORDER);

    /** The background of a window. */
    public static final SystemColor window = new SystemColor((byte) WINDOW);

    /** The border of a window. */
    public static final SystemColor windowBorder = new SystemColor((byte) WINDOW_BORDER);

    /** The text of a window. */
    public static final SystemColor windowText = new SystemColor((byte) WINDOW_TEXT);

    /** The background of a menu. */
    public static final SystemColor menu = new SystemColor((byte) MENU);

    /** The text of a menu. */
    public static final SystemColor menuText = new SystemColor((byte) MENU_TEXT);

    /** The background of a text field. */
    public static final SystemColor text = new SystemColor((byte) TEXT);

    /** The text of a text field. */
    public static final SystemColor textText = new SystemColor((byte) TEXT_TEXT);

    /** The background of selected text. */
    public static final SystemColor textHighlight = new SystemColor((byte) TEXT_HIGHLIGHT);

    /** Selected text. */
    public static final SystemColor textHighlightText = new SystemColor((byte) TEXT_HIGHLIGHT_TEXT);

    /** Disabled text. */
    public static final SystemColor textInactiveText = new SystemColor((byte) TEXT_INACTIVE_TEXT);

    /** The background of a control. */
    public static final SystemColor control = new SystemColor((byte) CONTROL);

    /** The text of a control. */
    public static final SystemColor controlText = new SystemColor((byte) CONTROL_TEXT);

    /** A control's highlight, on the lit side. */
    public static final SystemColor controlHighlight = new SystemColor((byte) CONTROL_HIGHLIGHT);

    /** A control's light highlight. */
    public static final SystemColor controlLtHighlight =
        new SystemColor((byte) CONTROL_LT_HIGHLIGHT);

    /** A control's shadow. */
    public static final SystemColor controlShadow = new SystemColor((byte) CONTROL_SHADOW);

    /** A control's dark shadow. */
    public static final SystemColor controlDkShadow = new SystemColor((byte) CONTROL_DK_SHADOW);

    /** The background of a scroll bar's track. */
    public static final SystemColor scrollbar = new SystemColor((byte) SCROLLBAR);

    /** The background of a tooltip. */
    public static final SystemColor info = new SystemColor((byte) INFO);

    /** The text of a tooltip. */
    public static final SystemColor infoText = new SystemColor((byte) INFO_TEXT);

    // Which of the twenty-six roles it is. It is the only thing the object keeps: the colour is
    // looked up in the table every time, which is what makes it live.
    private final transient int index;

    private SystemColor(byte index) {
        // The super is built with the start-up value; `getRGB` reads it again from the table, so
        // this number is only the initial state of the inherited Color.
        super(systemColors[index]);
        this.index = index;
    }

    /**
     * Today's colour for this role.
     *
     * <p>It is read from the table on every call, not from the state inherited from {@link Color}:
     * that is what keeps the same object correct after a theme change.
     */
    @Override
    public int getRGB() {
        return systemColors[this.index];
    }

    /**
     * The index of the role, not the colour.
     *
     * <p>Printing the value would be misleading: it changes with the theme, and whoever reads the
     * text would think that number identifies the object. The index does identify it.
     */
    @Override
    public String toString() {
        return getClass().getName() + "[i=" + this.index + "]";
    }
}
