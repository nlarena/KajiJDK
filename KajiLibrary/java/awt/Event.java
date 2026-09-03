package java.awt;

/**
 * El evento del AWT de 1.0: una sola clase con campos publicos para todo, anterior al modelo de
 * escuchas.
 *
 * <p>Esta obsoleta desde 1.1 y sin embargo se puede escribir entera, que es exactamente por lo que
 * esta aca: no menciona ni un solo tipo del sistema de ventanas. El objetivo del evento es un
 * {@code Object} --no un {@code Component}-- porque en 1.0 el objetivo se comparaba con
 * {@code ==} y nadie se molesto en tiparlo. Toda la clase son sesenta constantes, diez campos y
 * cuatro metodos de aritmetica de bits.
 *
 * <p>Los codigos de tecla mezclan dos convenciones y conviene no "ordenarlos": ENTER, BACK_SPACE,
 * TAB, ESCAPE y DELETE son el valor ASCII del caracter (10, 8, 9, 27, 127), mientras que las teclas
 * que no producen caracter --las flechas, las de funcion, PAUSE-- se numeran desde 1000. Por eso
 * HOME vale 1000 y no 1; el que las de accion arranquen tan arriba es lo que permite que un mismo
 * campo {@code key} sirva para las dos clases de tecla sin ambiguedad.
 *
 * <p>Los identificadores de evento tambien se agrupan por centenas --2xx ventana, 4xx teclado, 5xx
 * mouse, 6xx scroll, 7xx lista, 1xxx varios-- y ahi si hay una colision heredada que no se puede
 * arreglar: {@code ACTION_EVENT} vale 1001, igual que {@code PGUP}. No se pisan porque uno va en
 * {@code id} y el otro en {@code key}, pero explica por que las dos tablas no se pueden unificar.
 */
public class Event implements java.io.Serializable {

    private static final long serialVersionUID = 5488922509400504703L;

    // --- modificadores: mascara de bits, se combinan con OR ---

    public static final int SHIFT_MASK = 1 << 0;

    public static final int CTRL_MASK = 1 << 1;

    public static final int META_MASK = 1 << 2;

    public static final int ALT_MASK = 1 << 3;

    // --- teclas de accion: no producen caracter, se numeran desde 1000 ---

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

    // --- teclas con caracter: el valor es el ASCII, no un codigo inventado ---

    public static final int ENTER = '\n';

    public static final int BACK_SPACE = '\b';

    public static final int TAB = '\t';

    public static final int ESCAPE = 27;

    public static final int DELETE = 127;

    // --- identificadores de evento, agrupados por centenas ---

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

    /** A quien le paso. Es Object y no Component porque en 1.0 solo se comparaba con {@code ==}. */
    public Object target;

    public long when;

    public int id;

    public int x;

    public int y;

    public int key;

    public int modifiers;

    public int clickCount;

    public Object arg;

    /** El siguiente de la cola. En 1.0 los eventos se encadenaban a mano. */
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
                // Estos ya son la conclusion de otro evento --el click que ya se proceso-- asi que
                // nacen consumidos: reenviarlos al peer los duplicaria.
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
                // Los demas no se pueden consumir.
                break;
        }
    }

    boolean isConsumed() {
        return consumed;
    }

    /**
     * Los campos que valen cero o null no se imprimen. No es solo por brevedad: en 1.0 un evento se
     * reutilizaba para todo, asi que la mitad de los campos siempre estaban en cero y listarlos
     * enterraba los dos o tres que importaban.
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
