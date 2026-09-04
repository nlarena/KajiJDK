package javax.swing;

/**
 * Las constantes de posicion y orientacion que comparten los componentes de Swing.
 *
 * <p>Es una interfaz sin metodos, y un componente la <em>implementa</em> para poder escribir
 * {@code CENTER} a secas en vez de {@code SwingConstants.CENTER}. Es un uso de las interfaces que
 * hoy se considera de mal gusto —mete constantes en la API publica de cada clase— y que el JDK
 * conserva por compatibilidad: esta aca porque {@code JLabel} y todos los demas la llevan en su
 * firma.
 *
 * <p>Los puntos cardinales y {@link #LEADING}/{@link #TRAILING} son dos vocabularios sobre lo mismo.
 * La diferencia importa: {@code LEFT} es siempre la izquierda, y {@code LEADING} es el lado por el
 * que <em>empieza</em> el texto, que en un idioma que se lee de derecha a izquierda es la derecha.
 */
public interface SwingConstants {

    /** El centro, en las dos direcciones. */
    public static final int CENTER = 0;

    /** Arriba. */
    public static final int TOP = 1;

    /** A la izquierda, sin importar la orientacion. */
    public static final int LEFT = 2;

    /** Abajo. */
    public static final int BOTTOM = 3;

    /** A la derecha, sin importar la orientacion. */
    public static final int RIGHT = 4;

    /** Norte: arriba y al centro. */
    public static final int NORTH = 1;

    /** Noreste. */
    public static final int NORTH_EAST = 2;

    /** Este. */
    public static final int EAST = 3;

    /** Sureste. */
    public static final int SOUTH_EAST = 4;

    /** Sur. */
    public static final int SOUTH = 5;

    /** Suroeste. */
    public static final int SOUTH_WEST = 6;

    /** Oeste. */
    public static final int WEST = 7;

    /** Noroeste. */
    public static final int NORTH_WEST = 8;

    /** Horizontal. */
    public static final int HORIZONTAL = 0;

    /** Vertical. */
    public static final int VERTICAL = 1;

    /** El lado por el que empieza el texto, segun la orientacion del componente. */
    public static final int LEADING = 10;

    /** El lado por el que termina el texto, segun la orientacion del componente. */
    public static final int TRAILING = 11;

    /** El siguiente, en una secuencia. */
    public static final int NEXT = 12;

    /** El anterior, en una secuencia. */
    public static final int PREVIOUS = 13;
}
