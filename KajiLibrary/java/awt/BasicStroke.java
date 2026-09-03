package java.awt;

/**
 * KajiLibrary's java.awt.BasicStroke -- como se dibuja una linea: ancho, puntas, esquinas y guiones.
 *
 * <p>Es un valor inmutable y nada mas: no dibuja, describe. Quien dibuja es el {@code Graphics2D} que
 * la recibe, y por eso esta clase se puede escribir entera sin ningun sistema de ventanas -- salvo un
 * metodo, ver abajo.
 *
 * <h2>Las cuatro decisiones que describe</h2>
 *
 * <ul>
 *   <li><b>ancho</b>: en unidades del espacio del usuario, no en pixeles. Un ancho de 0 es legal y
 *       significa "la linea mas fina que el dispositivo pueda dibujar".
 *   <li><b>punta</b> ({@code CAP_*}): que pasa en los extremos de un tramo abierto.
 *   <li><b>esquina</b> ({@code JOIN_*}): que pasa donde dos tramos se encuentran.
 *   <li><b>guiones</b>: el patron de trazo y relleno, mas la fase, que es cuanto del patron ya se
 *       consumio al empezar.
 * </ul>
 *
 * <h2>Las validaciones, que no son las que uno espera</h2>
 *
 * <p>Las cuatro salieron de preguntarle al JDK 25, y en las cuatro lo intuitivo es lo contrario:
 *
 * <ol>
 *   <li>Un ancho de <b>cero es valido</b>; solo se rechaza el negativo.
 *   <li>El limite de inglete se comprueba <b>solo si la esquina es {@code JOIN_MITER}</b>. Con
 *       {@code JOIN_ROUND} un valor menor que 1 pasa sin protestar, porque ahi no se usa.
 *   <li>Un patron de guiones con <b>algun</b> cero es valido; solo se rechaza el que los tiene
 *       <b>todos</b> en cero, que seria un patron que no avanza nunca.
 *   <li>Los constructores de cero y de un argumento usan {@code CAP_SQUARE}, no {@code CAP_BUTT}.
 * </ol>
 *
 * <h2>El unico metodo que no esta implementado</h2>
 *
 * <p>{@code createStrokedShape(Shape)} lanza {@code UnsupportedOperationException}. Su trabajo es
 * calcular el contorno de la figura que resulta de recorrer un camino con este pincel: ensanchar
 * cada tramo, cerrar las puntas, resolver las esquinas -- con el corte del inglete cuando se pasa
 * del limite -- y aplicar los guiones a lo largo de curvas de Bezier. Es geometria computacional de
 * verdad, y una version aproximada seria peor que la excepcion: devolveria un contorno que se ve
 * casi bien y que no coincide con el que dibuja cualquier otra implementacion, sin avisar.
 *
 * <p>Se declara igual, y no se deja la clase afuera, porque {@code Stroke} lo exige y porque el uso
 * normal de {@code BasicStroke} no pasa por ahi: se construye, se le pasa a un {@code Graphics2D} y
 * es el rasterizador el que la interpreta. Todo lo que la clase promete como <b>valor</b> --las
 * validaciones, las copias, la igualdad, el hash-- es exacto.
 */
public class BasicStroke implements Stroke {

    /** Las esquinas se prolongan hasta que los dos bordes se cruzan. */
    public static final int JOIN_MITER = 0;

    /** Las esquinas se redondean con un arco. */
    public static final int JOIN_ROUND = 1;

    /** Las esquinas se cortan con un segmento recto. */
    public static final int JOIN_BEVEL = 2;

    /** La linea termina justo en el punto final, sin sobresalir. */
    public static final int CAP_BUTT = 0;

    /** La linea termina en un semicirculo que sobresale medio ancho. */
    public static final int CAP_ROUND = 1;

    /** La linea termina en un cuadrado que sobresale medio ancho. */
    public static final int CAP_SQUARE = 2;

    // De paquete y no privados: es como los declara el JDK, y hay codigo del propio java.awt que los
    // lee sin pasar por los accesores.
    float width;
    int join;
    int cap;
    float miterlimit;
    float[] dash;
    float dash_phase;

    /**
     * @param width      el ancho; 0 significa la linea mas fina posible
     * @param cap        una de las {@code CAP_*}
     * @param join       una de las {@code JOIN_*}
     * @param miterlimit el limite de inglete; solo se usa --y solo se valida-- con {@code JOIN_MITER}
     * @param dash       el patron de guiones, o null para linea continua
     * @param dash_phase cuanto del patron ya se consumio al empezar
     * @throws IllegalArgumentException si algun argumento esta fuera de rango; ver la nota de la
     *     clase, porque los limites no son los que uno supone
     */
    public BasicStroke(float width, int cap, int join, float miterlimit,
            float[] dash, float dash_phase) {
        if (width < 0.0f) {
            throw new IllegalArgumentException("negative width");
        }
        if (cap != CAP_BUTT && cap != CAP_ROUND && cap != CAP_SQUARE) {
            throw new IllegalArgumentException("illegal end cap value");
        }
        if (join == JOIN_MITER) {
            // Solo aca: con las otras dos esquinas el limite no se usa para nada.
            if (miterlimit < 1.0f) {
                throw new IllegalArgumentException("miter limit < 1");
            }
        } else if (join != JOIN_ROUND && join != JOIN_BEVEL) {
            throw new IllegalArgumentException("illegal line join value");
        }
        if (dash != null) {
            if (dash_phase < 0.0f) {
                throw new IllegalArgumentException("negative dash phase");
            }
            boolean anyPositive = false;
            int i = 0;
            while (i < dash.length) {
                if (dash[i] < 0.0f) {
                    throw new IllegalArgumentException("negative dash length");
                }
                if (dash[i] > 0.0f) {
                    anyPositive = true;
                }
                i = i + 1;
            }
            // Un patron que no avanza nunca dejaria al dibujante en un bucle infinito. Que **algun**
            // tramo sea cero, en cambio, es legitimo: es como se piden puntos con CAP_ROUND.
            if (!anyPositive) {
                throw new IllegalArgumentException("dash lengths all zero");
            }
        }
        this.width = width;
        this.cap = cap;
        this.join = join;
        this.miterlimit = miterlimit;
        if (dash != null) {
            this.dash = copy(dash);
        }
        this.dash_phase = dash_phase;
    }

    /** Linea continua, con las puntas y esquinas dadas. */
    public BasicStroke(float width, int cap, int join, float miterlimit) {
        this(width, cap, join, miterlimit, null, 0.0f);
    }

    /** Idem, con el limite de inglete por omision. */
    public BasicStroke(float width, int cap, int join) {
        this(width, cap, join, 10.0f, null, 0.0f);
    }

    /** Solo el ancho. Las puntas quedan en {@code CAP_SQUARE} y las esquinas en {@code JOIN_MITER}. */
    public BasicStroke(float width) {
        this(width, CAP_SQUARE, JOIN_MITER, 10.0f, null, 0.0f);
    }

    /** El pincel por omision: ancho 1, {@code CAP_SQUARE}, {@code JOIN_MITER}, limite 10. */
    public BasicStroke() {
        this(1.0f, CAP_SQUARE, JOIN_MITER, 10.0f, null, 0.0f);
    }

    public float getLineWidth() {
        return this.width;
    }

    public int getEndCap() {
        return this.cap;
    }

    public int getLineJoin() {
        return this.join;
    }

    public float getMiterLimit() {
        return this.miterlimit;
    }

    /** El patron de guiones, o null si la linea es continua. Copia. */
    public float[] getDashArray() {
        if (this.dash == null) {
            return null;
        }
        return copy(this.dash);
    }

    public float getDashPhase() {
        return this.dash_phase;
    }

    /**
     * No esta implementado: ver la nota de la clase.
     *
     * @throws UnsupportedOperationException siempre
     */
    public Shape createStrokedShape(Shape p) {
        throw new UnsupportedOperationException("createStrokedShape necesita un motor de geometria "
            + "que esta biblioteca no tiene; un contorno aproximado no coincidiria con el de "
            + "ninguna otra implementacion");
    }

    private static float[] copy(float[] a) {
        float[] c = new float[a.length];
        System.arraycopy(a, 0, c, 0, a.length);
        return c;
    }

    /**
     * El hash del JDK, bit a bit.
     *
     * <p>Se mezcla la representacion en bits de los flotantes y no su valor, y la fase y los guiones
     * entran solo si hay patron. Vale reproducirlo exactamente: un {@code BasicStroke} es una clave
     * razonable en una cache de pinceles, y un hash distinto no rompe nada pero hace que dos
     * bibliotecas no compartan esa cache.
     */
    @Override
    public int hashCode() {
        int hash = Float.floatToIntBits(this.width);
        hash = hash * 31 + this.join;
        hash = hash * 31 + this.cap;
        hash = hash * 31 + Float.floatToIntBits(this.miterlimit);
        if (this.dash != null) {
            hash = hash * 31 + Float.floatToIntBits(this.dash_phase);
            int i = 0;
            while (i < this.dash.length) {
                hash = hash * 31 + Float.floatToIntBits(this.dash[i]);
                i = i + 1;
            }
        }
        return hash;
    }

    /** Dos pinceles son el mismo si dibujarian igual. */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BasicStroke)) {
            return false;
        }
        BasicStroke other = (BasicStroke) obj;
        if (this.width != other.width || this.join != other.join || this.cap != other.cap
                || this.miterlimit != other.miterlimit) {
            return false;
        }
        if (this.dash == null) {
            return other.dash == null;
        }
        if (other.dash == null || this.dash_phase != other.dash_phase
                || this.dash.length != other.dash.length) {
            return false;
        }
        int i = 0;
        while (i < this.dash.length) {
            if (this.dash[i] != other.dash[i]) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }
}
