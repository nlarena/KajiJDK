package java.awt;

/**
 * Como se coloca un componente dentro de un {@code GridBagLayout}: celda, expansion, relleno,
 * anclaje y margenes.
 *
 * <p>Es una bolsa de campos publicos sin comportamiento, asi que se puede escribir entera aunque el
 * {@code GridBagLayout} que la consume no exista: la clase no toca ningun componente, solo describe
 * una intencion.
 *
 * <p>Los valores de los anclajes no son consecutivos por casualidad. Los absolutos --CENTER,
 * NORTH...-- van del 10 al 18; los relativos al sentido de lectura --PAGE_START, LINE_END...-- del
 * 19 al 26; y los relativos a la linea de base saltan a multiplos de 256. El salto no es estetico:
 * permite que el layout distinga las tres familias con una comparacion de rango en vez de un
 * switch de treinta casos, y deja hueco para agregar valores sin renumerar nada.
 */
public class GridBagConstraints implements Cloneable, java.io.Serializable {

    private static final long serialVersionUID = -1000070633030801713L;

    /** Poner esto a continuacion del anterior, sin decir en que fila o columna cae. */
    public static final int RELATIVE = -1;

    /** Este componente es el ultimo de su fila o columna. */
    public static final int REMAINDER = 0;

    /** No agrandar el componente aunque sobre lugar en la celda. */
    public static final int NONE = 0;

    public static final int BOTH = 1;

    public static final int HORIZONTAL = 2;

    public static final int VERTICAL = 3;

    // --- anclajes absolutos: la celda como puntos cardinales ---

    public static final int CENTER = 10;

    public static final int NORTH = 11;

    public static final int NORTHEAST = 12;

    public static final int EAST = 13;

    public static final int SOUTHEAST = 14;

    public static final int SOUTH = 15;

    public static final int SOUTHWEST = 16;

    public static final int WEST = 17;

    public static final int NORTHWEST = 18;

    // --- anclajes relativos al sentido de lectura: en arabe "LINE_START" es la derecha ---

    public static final int PAGE_START = 19;

    public static final int PAGE_END = 20;

    public static final int LINE_START = 21;

    public static final int LINE_END = 22;

    public static final int FIRST_LINE_START = 23;

    public static final int FIRST_LINE_END = 24;

    public static final int LAST_LINE_START = 25;

    public static final int LAST_LINE_END = 26;

    // --- anclajes relativos a la linea de base del texto ---

    public static final int BASELINE = 0x100;

    public static final int BASELINE_LEADING = 0x200;

    public static final int BASELINE_TRAILING = 0x300;

    public static final int ABOVE_BASELINE = 0x400;

    public static final int ABOVE_BASELINE_LEADING = 0x500;

    public static final int ABOVE_BASELINE_TRAILING = 0x600;

    public static final int BELOW_BASELINE = 0x700;

    public static final int BELOW_BASELINE_LEADING = 0x800;

    public static final int BELOW_BASELINE_TRAILING = 0x900;

    public int gridx;

    public int gridy;

    public int gridwidth;

    public int gridheight;

    public double weightx;

    public double weighty;

    public int anchor;

    public int fill;

    public Insets insets;

    public int ipadx;

    public int ipady;

    /** Los valores por defecto: una celda a continuacion de la anterior, centrada y sin estirar. */
    public GridBagConstraints() {
        gridx = RELATIVE;
        gridy = RELATIVE;
        gridwidth = 1;
        gridheight = 1;

        weightx = 0;
        weighty = 0;
        anchor = CENTER;
        fill = NONE;

        insets = new Insets(0, 0, 0, 0);
        ipadx = 0;
        ipady = 0;
    }

    public GridBagConstraints(int gridx, int gridy, int gridwidth, int gridheight,
            double weightx, double weighty, int anchor, int fill, Insets insets,
            int ipadx, int ipady) {
        this.gridx = gridx;
        this.gridy = gridy;
        this.gridwidth = gridwidth;
        this.gridheight = gridheight;
        this.fill = fill;
        this.ipadx = ipadx;
        this.ipady = ipady;
        this.insets = insets;
        this.anchor = anchor;
        this.weightx = weightx;
        this.weighty = weighty;
    }

    /**
     * Copia. Los Insets se clonan aparte: son un objeto mutable y si se compartieran, cambiar el
     * margen de la copia cambiaria el del original, que es justo lo que nadie espera de un clone.
     */
    public Object clone() {
        try {
            GridBagConstraints c = (GridBagConstraints) super.clone();
            c.insets = (Insets) insets.clone();
            return c;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }
}
