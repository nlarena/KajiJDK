package java.awt;

/**
 * La forma del puntero del mouse: la flecha, la manito, el reloj, las ocho flechas de redimension.
 *
 * <p>Un Cursor no es un dibujo sino un pedido: guarda un tipo y un nombre, y quien lo pinta es el
 * sistema. Por eso la clase se puede escribir sin sistema de ventanas: lo que hay es la tabla de
 * los catorce tipos predefinidos y el cache que los comparte.
 *
 * <p>{@code CUSTOM_CURSOR} vale -1 y no 14 a proposito: no es un tipo mas de la serie sino la
 * marca de "esto no es ninguno de los predefinidos", y estar fuera del rango 0..13 es justo lo que
 * hace que la validacion de tipo lo rechace sin un caso especial.
 *
 * <h2>Lo que falta y por que</h2>
 *
 * <p>{@code getSystemCustomCursor(String)} <b>no esta</b>. Busca el cursor en un archivo de
 * propiedades del escritorio a traves del {@code Toolkit}, y {@code Toolkit} no existe en
 * KajiLibrary. Devolver null --que la firma permite, porque significa "ese cursor no esta
 * definido"-- seria peor que no tener el metodo: diria que se busco y no se encontro cuando en
 * realidad no se busco nada.
 *
 * <p>Los nombres de los predefinidos son los que el JDK usa cuando no encuentra el paquete de
 * recursos traducido, que es el caso aca: no hay traduccion que buscar, asi que el valor por
 * defecto es la respuesta correcta y no un relleno.
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

    /** Fuera de la serie: -1 no es un tipo, es "ninguno de los de arriba". */
    public static final int CUSTOM_CURSOR = -1;

    /**
     * El cache de los predefinidos. Es {@code protected} porque estaba asi en 1.1 y quedo; se
     * llena a medida que se piden, no de entrada, para no fabricar catorce objetos que nadie use.
     */
    protected static Cursor[] predefined = new Cursor[14];

    /**
     * Clave del paquete de recursos y nombre por defecto de cada tipo. Sin Toolkit se usa siempre
     * la segunda columna, que es lo que el JDK devuelve cuando no hay traduccion.
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
     * Para las subclases que representan un cursor hecho a medida. No valida el tipo porque el
     * suyo es CUSTOM_CURSOR, que esta fuera del rango a proposito.
     */
    protected Cursor(String name) {
        this.type = Cursor.CUSTOM_CURSOR;
        this.name = name;
    }

    /**
     * Los predefinidos se comparten: dos llamadas con el mismo tipo devuelven el mismo objeto. Un
     * cursor es inmutable, asi que no hay nada que se pueda romper compartiendolo, y en cambio
     * evita fabricar uno por cada componente de una ventana.
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
}
