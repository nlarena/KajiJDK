package java.awt;

/**
 * The shape of the mouse pointer: the arrow, the little hand, the clock, the eight resize arrows.
 *
 * <p>A Cursor is not a drawing but a request: it keeps a type and a name, and the one that paints
 * it is the system. That is why the class can be written without a windowing system: what there is
 * here is the table of the fourteen predefined types and the cache that shares them.
 *
 * <p>{@code CUSTOM_CURSOR} is -1 and not 14 on purpose: it is not one more type of the series but
 * the mark meaning "this is none of the predefined ones", and being outside the range 0..13 is
 * exactly what makes the type check reject it with no special case.
 *
 * <h2>What is missing and why</h2>
 *
 * <p>{@code getSystemCustomCursor(String)} is here, and it **throws**. It looks the cursor up in a
 * file of desktop descriptors, and this library ships none. Returning null would say "it was looked
 * up and was not there", which is not true; the {@code AWTException} the method itself declares
 * says "it could not be found", which is.
 *
 * <p>The names of the predefined ones are those the JDK uses when it does not find the translated
 * resource bundle, which is the case here: there is no translation to look for, so the default
 * value is the right answer and not filler.
 */
public class Cursor implements java.io.Serializable {

    private static final long serialVersionUID = 8028237497568985504L;

    public static final int DEFAULT_CURSOR = 0;

    public static final int CROSSHAIR_CURSOR = 1;

    public static final int TEXT_CURSOR = 2;

    public static final int WAIT_CURSOR = 3;

    public static final int SW_RESIZE_CURSOR = 4;

    public static final int SE_RESIZE_CURSOR = 5;

    public static final int NW_RESIZE_CURSOR = 6;

    public static final int NE_RESIZE_CURSOR = 7;

    public static final int N_RESIZE_CURSOR = 8;

    public static final int S_RESIZE_CURSOR = 9;

    public static final int W_RESIZE_CURSOR = 10;

    public static final int E_RESIZE_CURSOR = 11;

    public static final int HAND_CURSOR = 12;

    public static final int MOVE_CURSOR = 13;

    /** Outside the series: -1 is not a type, it is "none of the above". */
    public static final int CUSTOM_CURSOR = -1;

    /**
     * The cache of the predefined ones. It is {@code protected} because it was like that in 1.1 and
     * stayed; it fills up as they are asked for, not up front, so as not to build fourteen objects
     * nobody uses.
     */
    protected static Cursor[] predefined = new Cursor[14];

    /**
     * Resource bundle key and default name of each type. Without a Toolkit the second column is
     * always used, which is what the JDK returns when there is no translation.
     */
    static final String[][] cursorProperties = {
        {"AWT.DefaultCursor", "Default Cursor"},
        {"AWT.CrosshairCursor", "Crosshair Cursor"},
        {"AWT.TextCursor", "Text Cursor"},
        {"AWT.WaitCursor", "Wait Cursor"},
        {"AWT.SWResizeCursor", "Southwest Resize Cursor"},
        {"AWT.SEResizeCursor", "Southeast Resize Cursor"},
        {"AWT.NWResizeCursor", "Northwest Resize Cursor"},
        {"AWT.NEResizeCursor", "Northeast Resize Cursor"},
        {"AWT.NResizeCursor", "North Resize Cursor"},
        {"AWT.SResizeCursor", "South Resize Cursor"},
        {"AWT.WResizeCursor", "West Resize Cursor"},
        {"AWT.EResizeCursor", "East Resize Cursor"},
        {"AWT.HandCursor", "Hand Cursor"},
        {"AWT.MoveCursor", "Move Cursor"},
    };

    int type = DEFAULT_CURSOR;

    protected String name;

    public Cursor(int type) {
        if (type < Cursor.DEFAULT_CURSOR || type > Cursor.MOVE_CURSOR) {
            throw new IllegalArgumentException("illegal cursor type");
        }
        this.type = type;
        this.name = cursorProperties[type][1];
    }

    /**
     * For the subclasses that stand for a custom cursor. It does not validate the type because
     * theirs is CUSTOM_CURSOR, which is outside the range on purpose.
     */
    protected Cursor(String name) {
        this.type = Cursor.CUSTOM_CURSOR;
        this.name = name;
    }

    /**
     * The predefined ones are shared: two calls with the same type return the same object. A cursor
     * is immutable, so there is nothing that sharing it can break, and it avoids building one per
     * component of a window.
     */
    public static Cursor getPredefinedCursor(int type) {
        if (type < Cursor.DEFAULT_CURSOR || type > Cursor.MOVE_CURSOR) {
            throw new IllegalArgumentException("illegal cursor type");
        }
        Cursor c = predefined[type];
        if (c == null) {
            c = new Cursor(type);
            predefined[type] = c;
        }
        return c;
    }

    public static Cursor getDefaultCursor() {
        return getPredefinedCursor(Cursor.DEFAULT_CURSOR);
    }

    public int getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return getClass().getName() + "[" + getName() + "]";
    }

    /**
     * A cursor defined by the desktop, looked up by name.
     *
     * @throws AWTException always: there are no descriptors to look in. The predefined cursors are
     *     there, in {@link #getPredefinedCursor}.
     */
    public static Cursor getSystemCustomCursor(String name) throws AWTException {
        throw new AWTException("System cursor not found: " + name
                + " (this library ships no desktop cursor descriptors)");
    }
}
