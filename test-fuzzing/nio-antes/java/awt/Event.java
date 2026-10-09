package java.awt;

/**
 * The event of AWT 1.0: one single class with public fields for everything, older than the listener
 * model.
 *
 * <p>It has been obsolete since 1.1 and can nevertheless be written whole, which is exactly why it
 * is here: it does not mention a single type of the windowing system. The target of the event is an
 * {@code Object} --not a {@code Component}-- because in 1.0 the target was compared with {@code ==}
 * and nobody bothered to type it. The whole class is sixty-four public constants, ten fields and
 * three methods of bit arithmetic.
 *
 * <p>The key codes mix two conventions and it is better not to "tidy them up": ENTER, BACK_SPACE,
 * TAB, ESCAPE and DELETE are the ASCII value of the character (10, 8, 9, 27, 127), while the keys
 * that produce no character --the arrows, the function ones, PAUSE-- are numbered from 1000 up.
 * That is why HOME is 1000 and not 1; that the action keys start so high is what lets one same
 * {@code key} field serve both kinds of key without ambiguity.
 *
 * <p>The event identifiers are also grouped by hundreds --2xx window, 4xx keyboard, 5xx mouse, 6xx
 * scroll, 7xx list, 1xxx miscellaneous-- and there there is an inherited collision that cannot be
 * fixed: {@code ACTION_EVENT} is 1001, the same as {@code END}. They do not step on each other
 * because one goes in {@code id} and the other in {@code key}, but it explains why the two tables
 * cannot be unified.
 */
public class Event implements java.io.Serializable {

    private static final long serialVersionUID = 5488922509400504703L;

    // --- modifiers: a bit mask, they combine with OR ---

    public static final int SHIFT_MASK = 1 << 0;

    public static final int CTRL_MASK = 1 << 1;

    public static final int META_MASK = 1 << 2;

    public static final int ALT_MASK = 1 << 3;

    // --- action keys: they produce no character, numbered from 1000 up ---

    public static final int HOME = 1000;

    public static final int END = 1001;

    public static final int PGUP = 1002;

    public static final int PGDN = 1003;

    public static final int UP = 1004;

    public static final int DOWN = 1005;

    public static final int LEFT = 1006;

    public static final int RIGHT = 1007;

    public static final int F1 = 1008;

    public static final int F2 = 1009;

    public static final int F3 = 1010;

    public static final int F4 = 1011;

    public static final int F5 = 1012;

    public static final int F6 = 1013;

    public static final int F7 = 1014;

    public static final int F8 = 1015;

    public static final int F9 = 1016;

    public static final int F10 = 1017;

    public static final int F11 = 1018;

    public static final int F12 = 1019;

    public static final int PRINT_SCREEN = 1020;

    public static final int SCROLL_LOCK = 1021;

    public static final int CAPS_LOCK = 1022;

    public static final int NUM_LOCK = 1023;

    public static final int PAUSE = 1024;

    public static final int INSERT = 1025;

    // --- keys with a character: the value is the ASCII one, not an invented code ---

    public static final int ENTER = '\n';

    public static final int BACK_SPACE = '\b';

    public static final int TAB = '\t';

    public static final int ESCAPE = 27;

    public static final int DELETE = 127;

    // --- event identifiers, grouped by hundreds ---

    private static final int WINDOW_EVENT = 200;

    public static final int WINDOW_DESTROY = 1 + WINDOW_EVENT;

    public static final int WINDOW_EXPOSE = 2 + WINDOW_EVENT;

    public static final int WINDOW_ICONIFY = 3 + WINDOW_EVENT;

    public static final int WINDOW_DEICONIFY = 4 + WINDOW_EVENT;

    public static final int WINDOW_MOVED = 5 + WINDOW_EVENT;

    private static final int KEY_EVENT = 400;

    public static final int KEY_PRESS = 1 + KEY_EVENT;

    public static final int KEY_RELEASE = 2 + KEY_EVENT;

    public static final int KEY_ACTION = 3 + KEY_EVENT;

    public static final int KEY_ACTION_RELEASE = 4 + KEY_EVENT;

    private static final int MOUSE_EVENT = 500;

    public static final int MOUSE_DOWN = 1 + MOUSE_EVENT;

    public static final int MOUSE_UP = 2 + MOUSE_EVENT;

    public static final int MOUSE_MOVE = 3 + MOUSE_EVENT;

    public static final int MOUSE_ENTER = 4 + MOUSE_EVENT;

    public static final int MOUSE_EXIT = 5 + MOUSE_EVENT;

    public static final int MOUSE_DRAG = 6 + MOUSE_EVENT;

    private static final int SCROLL_EVENT = 600;

    public static final int SCROLL_LINE_UP = 1 + SCROLL_EVENT;

    public static final int SCROLL_LINE_DOWN = 2 + SCROLL_EVENT;

    public static final int SCROLL_PAGE_UP = 3 + SCROLL_EVENT;

    public static final int SCROLL_PAGE_DOWN = 4 + SCROLL_EVENT;

    public static final int SCROLL_ABSOLUTE = 5 + SCROLL_EVENT;

    public static final int SCROLL_BEGIN = 6 + SCROLL_EVENT;

    public static final int SCROLL_END = 7 + SCROLL_EVENT;

    private static final int LIST_EVENT = 700;

    public static final int LIST_SELECT = 1 + LIST_EVENT;

    public static final int LIST_DESELECT = 2 + LIST_EVENT;

    private static final int MISC_EVENT = 1000;

    public static final int ACTION_EVENT = 1 + MISC_EVENT;

    public static final int LOAD_FILE = 2 + MISC_EVENT;

    public static final int SAVE_FILE = 3 + MISC_EVENT;

    public static final int GOT_FOCUS = 4 + MISC_EVENT;

    public static final int LOST_FOCUS = 5 + MISC_EVENT;

    /**
     * Who it happened to. It is Object and not Component because in 1.0 it was only compared with
     * {@code ==}.
     */
    public Object target;

    public long when;

    public int id;

    public int x;

    public int y;

    public int key;

    public int modifiers;

    public int clickCount;

    public Object arg;

    /** The next one in the queue. In 1.0 events were chained by hand. */
    public Event evt;

    private boolean consumed;

    public Event(Object target, long when, int id, int x, int y, int key, int modifiers,
            Object arg) {
        this.target = target;
        this.when = when;
        this.id = id;
        this.x = x;
        this.y = y;
        this.key = key;
        this.modifiers = modifiers;
        this.arg = arg;
        this.clickCount = 0;
        switch (id) {
            case ACTION_EVENT:
            case WINDOW_DESTROY:
            case WINDOW_ICONIFY:
            case WINDOW_DEICONIFY:
            case WINDOW_MOVED:
            case SCROLL_LINE_UP:
            case SCROLL_LINE_DOWN:
            case SCROLL_PAGE_UP:
            case SCROLL_PAGE_DOWN:
            case SCROLL_ABSOLUTE:
            case SCROLL_BEGIN:
            case SCROLL_END:
            case LIST_SELECT:
            case LIST_DESELECT:
                // These are already the conclusion of another event --the click that was processed
                // already-- so they are born consumed: sending them back to the peer would
                // duplicate them.
                consumed = true;
                break;
            default:
                break;
        }
    }

    public Event(Object target, long when, int id, int x, int y, int key, int modifiers) {
        this(target, when, id, x, y, key, modifiers, null);
    }

    public Event(Object target, int id, Object arg) {
        this(target, 0, id, 0, 0, 0, 0, arg);
    }

    public void translate(int dx, int dy) {
        this.x += dx;
        this.y += dy;
    }

    public boolean shiftDown() {
        return (modifiers & SHIFT_MASK) != 0;
    }

    public boolean controlDown() {
        return (modifiers & CTRL_MASK) != 0;
    }

    public boolean metaDown() {
        return (modifiers & META_MASK) != 0;
    }

    void consume() {
        switch (id) {
            case KEY_PRESS:
            case KEY_RELEASE:
            case KEY_ACTION:
            case KEY_ACTION_RELEASE:
                consumed = true;
                break;
            default:
                // The rest cannot be consumed.
                break;
        }
    }

    boolean isConsumed() {
        return consumed;
    }

    /**
     * The fields that are zero or null are not printed. It is not only for brevity: in 1.0 one
     * event was reused for everything, so half the fields were always at zero and listing them
     * buried the two or three that mattered.
     */
    protected String paramString() {
        String str = "id=" + id + ",x=" + x + ",y=" + y;
        if (key != 0) {
            str += ",key=" + key;
        }
        if (shiftDown()) {
            str += ",shift";
        }
        if (controlDown()) {
            str += ",control";
        }
        if (metaDown()) {
            str += ",meta";
        }
        if (target != null) {
            str += ",target=" + target;
        }
        if (arg != null) {
            str += ",arg=" + arg;
        }
        return str;
    }

    public String toString() {
        return getClass().getName() + "[" + paramString() + "]";
    }
}
