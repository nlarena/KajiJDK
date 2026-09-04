package java.awt.font;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.Set;

/**
 * Reemplaza los dígitos latinos por los de otra escritura.
 *
 * <p>El número doce se escribe `12` en todas las escrituras del mundo: mismas cifras, mismo orden,
 * mismo valor posicional. Lo que cambia son los **dibujos** de las diez cifras. Esta clase hace
 * exactamente esa sustitución, carácter por carácter, sin tocar nada más.
 *
 * <p>Y ahí está el punto: es una operación de **presentación**, no de contenido. El texto sigue
 * diciendo doce; sólo se ve distinto. Por eso se aplica al dibujar y no al guardar.
 *
 * <p>Hay dos modos y la diferencia importa. El **fijo** convierte todos los dígitos a la escritura
 * elegida. El **contextual** mira el texto que rodea a cada dígito y usa la escritura de lo que vino
 * antes, que es lo que hace falta en un documento que mezcla idiomas: los números de la parte en
 * árabe salen en arábigo-índico y los de la parte en inglés quedan latinos, sin marcarlos a mano.
 *
 * <p>El contexto se sigue con los caracteres de **dirección fuerte** —letras, no puntuación ni
 * espacios—, porque son los que identifican una escritura sin ambigüedad. Un dígito que aparezca
 * antes de cualquier letra usa el contexto por omisión que se declaró al pedir el conversor.
 *
 * <p>El etíope es la excepción que confirma la regla: su sistema de cifras **no tiene cero**, así
 * que el `0` se deja como está en vez de convertirse en algo que no existe.
 */
public final class NumericShaper implements Serializable {

    private static final long serialVersionUID = -8022764705923730308L;

    /** Los dígitos latinos de siempre. */
    public static final int EUROPEAN = 1 << 0;

    /** Los dígitos arábigo-índicos. */
    public static final int ARABIC = 1 << 1;

    /** Los arábigo-índicos orientales, de Persia y Urdu. */
    public static final int EASTERN_ARABIC = 1 << 2;

    /** Devanagari. */
    public static final int DEVANAGARI = 1 << 3;

    /** Bengalí. */
    public static final int BENGALI = 1 << 4;

    /** Gurmukhi. */
    public static final int GURMUKHI = 1 << 5;

    /** Guyaratí. */
    public static final int GUJARATI = 1 << 6;

    /** Oriya. */
    public static final int ORIYA = 1 << 7;

    /** Tamil. */
    public static final int TAMIL = 1 << 8;

    /** Telugu. */
    public static final int TELUGU = 1 << 9;

    /** Canarés. */
    public static final int KANNADA = 1 << 10;

    /** Malayalam. */
    public static final int MALAYALAM = 1 << 11;

    /** Tailandés. */
    public static final int THAI = 1 << 12;

    /** Lao. */
    public static final int LAO = 1 << 13;

    /** Tibetano. */
    public static final int TIBETAN = 1 << 14;

    /** Birmano. */
    public static final int MYANMAR = 1 << 15;

    /** Etíope, que **no tiene cero**. */
    public static final int ETHIOPIC = 1 << 16;

    /** Jemer. */
    public static final int KHMER = 1 << 17;

    /** Mongol. */
    public static final int MONGOLIAN = 1 << 18;

    /** Todas las escrituras que las constantes de arriba pueden nombrar. */
    public static final int ALL_RANGES = 0x7FFFF;

    /** El dígito cero de cada escritura, en el orden de las constantes. */
    private static final char[] BASES = {
        '\u0030',
        '\u0660',
        '\u06F0',
        '\u0966',
        '\u09E6',
        '\u0A66',
        '\u0AE6',
        '\u0B66',
        '\u0BE6',
        '\u0C66',
        '\u0CE6',
        '\u0D66',
        '\u0E50',
        '\u0ED0',
        '\u0F20',
        '\u1040',
        '\u1369',
        '\u17E0',
        '\u1810'
    };

    /** Dónde empieza el bloque de cada escritura, para reconocer el contexto. */
    private static final char[] CTX_LO = { '\u0000', '\u0600', '\u0600', '\u0900', '\u0980', '\u0A00', '\u0A80', '\u0B00', '\u0B80', '\u0C00', '\u0C80', '\u0D00', '\u0E00', '\u0E80', '\u0F00', '\u1000', '\u1200', '\u1780', '\u1800' };

    /** Dónde termina. */
    private static final char[] CTX_HI = { '\u0300', '\u0780', '\u0780', '\u0980', '\u0A00', '\u0A80', '\u0B00', '\u0B80', '\u0C00', '\u0C80', '\u0D00', '\u0D80', '\u0E80', '\u0F00', '\u1000', '\u10A0', '\u1380', '\u1800', '\u18B0' };

    /** La posición de la escritura etíope, que es la que no tiene cero. */
    private static final int ETHIOPIC_KEY = 16;

    /** La posición de la escritura latina. */
    private static final int EUROPEAN_KEY = 0;

    /**
     * Las escrituras que se pueden pedir, como enumeración.
     *
     * <p>Es la forma moderna de nombrar lo mismo que las constantes `int` de arriba, y admite
     * bastantes más escrituras: las constantes son bits de una máscara y se quedaron sin lugar.
     */
    public static enum Range {

        /** Los dígitos latinos de siempre. */
        EUROPEAN('\u0030', '\u0000', '\u0300'),

        /** Los dígitos arábigo-índicos. */
        ARABIC('\u0660', '\u0600', '\u0780'),

        /** Los arábigo-índicos orientales, de Persia y Urdu. */
        EASTERN_ARABIC('\u06F0', '\u0600', '\u0780'),

        /** Devanagari. */
        DEVANAGARI('\u0966', '\u0900', '\u0980'),

        /** Bengalí. */
        BENGALI('\u09E6', '\u0980', '\u0A00'),

        /** Gurmukhi. */
        GURMUKHI('\u0A66', '\u0A00', '\u0A80'),

        /** Guyaratí. */
        GUJARATI('\u0AE6', '\u0A80', '\u0B00'),

        /** Oriya. */
        ORIYA('\u0B66', '\u0B00', '\u0B80'),

        /** Tamil. */
        TAMIL('\u0BE6', '\u0B80', '\u0C00'),

        /** Telugu. */
        TELUGU('\u0C66', '\u0C00', '\u0C80'),

        /** Canarés. */
        KANNADA('\u0CE6', '\u0C80', '\u0D00'),

        /** Malayalam. */
        MALAYALAM('\u0D66', '\u0D00', '\u0D80'),

        /** Tailandés. */
        THAI('\u0E50', '\u0E00', '\u0E80'),

        /** Lao. */
        LAO('\u0ED0', '\u0E80', '\u0F00'),

        /** Tibetano. */
        TIBETAN('\u0F20', '\u0F00', '\u1000'),

        /** Birmano. */
        MYANMAR('\u1040', '\u1000', '\u10A0'),

        /** Etíope, que **no tiene cero**. */
        ETHIOPIC('\u1369', '\u1200', '\u1380'),

        /** Jemer. */
        KHMER('\u17E0', '\u1780', '\u1800'),

        /** Mongol. */
        MONGOLIAN('\u1810', '\u1800', '\u18B0'),

        /** N’Ko. */
        NKO('\u07C0', '\u07C0', '\u0800'),

        /** Birmano, variante shan. */
        MYANMAR_SHAN('\u1090', '\u1000', '\u10A0'),

        /** Limbu. */
        LIMBU('\u1946', '\u1900', '\u1950'),

        /** Tai lue nuevo. */
        NEW_TAI_LUE('\u19D0', '\u1980', '\u19E0'),

        /** Balinés. */
        BALINESE('\u1B50', '\u1B00', '\u1B80'),

        /** Sundanés. */
        SUNDANESE('\u1BB0', '\u1B80', '\u1BC0'),

        /** Lepcha. */
        LEPCHA('\u1C40', '\u1C00', '\u1C50'),

        /** Ol chiki. */
        OL_CHIKI('\u1C50', '\u1C50', '\u1C80'),

        /** Vai. */
        VAI('\uA620', '\uA500', '\uA640'),

        /** Saurashtra. */
        SAURASHTRA('\uA8D0', '\uA880', '\uA8E0'),

        /** Kayah li. */
        KAYAH_LI('\uA900', '\uA900', '\uA930'),

        /** Cham. */
        CHAM('\uAA50', '\uAA00', '\uAA60'),

        /** Tai tham, dígitos hora. */
        TAI_THAM_HORA('\u1A80', '\u1A20', '\u1AB0'),

        /** Tai tham, dígitos tham. */
        TAI_THAM_THAM('\u1A90', '\u1A20', '\u1AB0'),

        /** Javanés. */
        JAVANESE('\uA9D0', '\uA980', '\uA9E0'),

        /** Meetei mayek. */
        MEETEI_MAYEK('\uABF0', '\uABC0', '\uAC00'),

        /** Cingalés. */
        SINHALA('\u0DE6', '\u0D80', '\u0E00'),

        /** Birmano, variante tai laing. */
        MYANMAR_TAI_LAING('\uA9F0', '\uA9E0', '\uAA00');

        private final char base;
        private final char start;
        private final char end;

        /** Con el dígito cero de la escritura y el bloque en el que vive. */
        private Range(char base, char start, char end) {
            this.base = base;
            this.start = start;
            this.end = end;
        }

        /** El dígito cero de esta escritura. */
        char getNumericBase() {
            return this.base;
        }

        /** Si ese carácter pertenece a esta escritura. */
        boolean contiene(char c) {
            return c >= this.start && c < this.end;
        }

        /** Si esta escritura no tiene cifra para el cero. */
        boolean sinCero() {
            return this == ETHIOPIC;
        }
    }

    /** La máscara de escrituras, si se pidió con constantes `int`. */
    private final int mask;

    /** El conjunto de escrituras, si se pidió con `Range`. */
    private final Set<Range> rangeSet;

    /** Qué escritura usar antes de encontrar contexto. */
    private final int key;

    /** Lo mismo, si se pidió con `Range`. */
    private final Range shapingRange;

    /** Si mira el texto de alrededor. */
    private final boolean contextual;

    /** El constructor común; se llega por las fábricas. */
    private NumericShaper(int mask, Set<Range> rangeSet, int key, Range shapingRange,
            boolean contextual) {
        this.mask = mask;
        this.rangeSet = rangeSet;
        this.key = key;
        this.shapingRange = shapingRange;
        this.contextual = contextual;
    }

    /** La posición de la única escritura de la máscara, o -1 si no hay exactamente una. */
    private static int unicaClave(int singleRange) {
        int key = -1;
        for (int i = 0; i < BASES.length; i++) {
            if ((singleRange & (1 << i)) != 0) {
                if (key >= 0) {
                    return -1;
                }
                key = i;
            }
        }
        return key;
    }

    /**
     * Un conversor fijo a esa escritura.
     *
     * @throws IllegalArgumentException si no se nombra exactamente una escritura
     */
    public static NumericShaper getShaper(int singleRange) {
        int key = unicaClave(singleRange);
        if (key < 0) {
            throw new IllegalArgumentException("invalid shaper: " + Integer.toHexString(singleRange));
        }
        return new NumericShaper(singleRange, null, key, null, false);
    }

    /**
     * Un conversor fijo a esa escritura.
     *
     * @throws NullPointerException si la escritura es `null`
     */
    public static NumericShaper getShaper(Range singleRange) {
        if (singleRange == null) {
            throw new NullPointerException();
        }
        Set<Range> uno = EnumSet.of(singleRange);
        return new NumericShaper(0, uno, 0, singleRange, false);
    }

    /**
     * Un conversor contextual entre esas escrituras, con el latino como contexto inicial.
     *
     * @throws IllegalArgumentException si no se nombra ninguna escritura conocida
     */
    public static NumericShaper getContextualShaper(int ranges) {
        int r = ranges & ALL_RANGES;
        return new NumericShaper(r, null, EUROPEAN_KEY, null, true);
    }

    /**
     * Un conversor contextual con el contexto inicial dado.
     *
     * @throws IllegalArgumentException si el contexto inicial no nombra exactamente una escritura
     */
    public static NumericShaper getContextualShaper(int ranges, int defaultContext) {
        int key = unicaClave(defaultContext);
        if (key < 0) {
            throw new IllegalArgumentException("invalid shaper: "
                    + Integer.toHexString(defaultContext));
        }
        return new NumericShaper(ranges & ALL_RANGES, null, key, null, true);
    }

    /**
     * Un conversor contextual entre esas escrituras, con el latino como contexto inicial.
     *
     * @throws NullPointerException si el conjunto es `null`
     */
    public static NumericShaper getContextualShaper(Set<Range> ranges) {
        Set<Range> copia = EnumSet.noneOf(Range.class);
        copia.addAll(ranges);
        return new NumericShaper(0, copia, 0, Range.EUROPEAN, true);
    }

    /**
     * Un conversor contextual con el contexto inicial dado.
     *
     * @throws NullPointerException si el conjunto o el contexto son `null`
     */
    public static NumericShaper getContextualShaper(Set<Range> ranges, Range defaultContext) {
        if (defaultContext == null) {
            throw new NullPointerException();
        }
        Set<Range> copia = EnumSet.noneOf(Range.class);
        copia.addAll(ranges);
        return new NumericShaper(0, copia, 0, defaultContext, true);
    }

    /** Si mira el texto de alrededor para decidir. */
    public boolean isContextual() {
        return this.contextual;
    }

    /**
     * Las escrituras, como máscara de constantes `int`.
     *
     * <p>Devuelve 0 si el conversor se armó con `Range`: hay escrituras que no tienen constante, y
     * dar una máscara incompleta sería peor que decir que no hay.
     */
    public int getRanges() {
        return this.mask;
    }

    /** Las escrituras, como conjunto. */
    public Set<Range> getRangeSet() {
        if (this.rangeSet != null) {
            Set<Range> copia = EnumSet.noneOf(Range.class);
            copia.addAll(this.rangeSet);
            return copia;
        }
        Set<Range> copia = EnumSet.noneOf(Range.class);
        Range[] todos = Range.values();
        for (int i = 0; i < BASES.length; i++) {
            if ((this.mask & (1 << i)) != 0) {
                copia.add(todos[i]);
            }
        }
        return copia;
    }

    /**
     * Convierte los dígitos del tramo, en el lugar.
     *
     * @throws NullPointerException si el texto es `null`
     * @throws IndexOutOfBoundsException si el tramo se sale del arreglo
     */
    public void shape(char[] text, int start, int count) {
        comprobar(text, start, count);
        if (this.contextual) {
            this.contextualmente(text, start, count, this.key, this.shapingRange);
        } else if (this.rangeSet != null) {
            convertir(text, start, count, this.shapingRange.getNumericBase(),
                    this.shapingRange.sinCero());
        } else {
            convertir(text, start, count, BASES[this.key], this.key == ETHIOPIC_KEY);
        }
    }

    /**
     * Como el anterior, con otro contexto inicial.
     *
     * @throws IllegalArgumentException si el contexto no nombra exactamente una escritura
     * @throws NullPointerException si el texto es `null`
     */
    public void shape(char[] text, int start, int count, int context) {
        comprobar(text, start, count);
        int key = unicaClave(context);
        if (key < 0) {
            throw new IllegalArgumentException("invalid context");
        }
        if (this.contextual) {
            this.contextualmente(text, start, count, key, null);
        } else {
            this.shape(text, start, count);
        }
    }

    /**
     * Como el anterior, con el contexto inicial dado como `Range`.
     *
     * @throws NullPointerException si el texto o el contexto son `null`
     */
    public void shape(char[] text, int start, int count, Range context) {
        comprobar(text, start, count);
        if (context == null) {
            throw new NullPointerException();
        }
        if (this.contextual) {
            this.contextualmente(text, start, count, 0, context);
        } else {
            this.shape(text, start, count);
        }
    }

    /**
     * Comprueba el tramo.
     *
     * @throws NullPointerException si el texto es `null`
     * @throws IndexOutOfBoundsException si el tramo se sale
     */
    private static void comprobar(char[] text, int start, int count) {
        if (text == null) {
            throw new NullPointerException("text is null");
        }
        if (start < 0 || count < 0 || start + count > text.length || start + count < 0) {
            throw new IndexOutOfBoundsException("bad start or count");
        }
    }

    /** Reemplaza los dígitos latinos del tramo por los de esa escritura. */
    private static void convertir(char[] text, int start, int count, char base, boolean sinCero) {
        char menor = sinCero ? '1' : '0';
        char corrimiento = (char) (base - '0');
        for (int i = start; i < start + count; i++) {
            char c = text[i];
            if (c >= menor && c <= '9') {
                text[i] = (char) (c + corrimiento);
            }
        }
    }

    /**
     * Recorre el texto siguiendo el contexto y convierte cada dígito según lo que vino antes.
     *
     * <p>El contexto sólo cambia con caracteres de **dirección fuerte**: los espacios y la
     * puntuación no pertenecen a ninguna escritura, y dejar que la corten haría que el número de
     * "12 árabes" se escribiera distinto que el de "12árabes".
     */
    private void contextualmente(char[] text, int start, int count, int ctxKey, Range ctxRange) {
        char base;
        boolean sinCero;
        if (this.rangeSet != null) {
            Range actual = ctxRange == null ? this.shapingRange : ctxRange;
            actual = this.admitida(actual);
            base = actual.getNumericBase();
            sinCero = actual.sinCero();
        } else {
            int k = ctxKey;
            if ((this.mask & (1 << k)) == 0) {
                k = EUROPEAN_KEY;
            }
            base = BASES[k];
            sinCero = k == ETHIOPIC_KEY;
        }
        for (int i = start; i < start + count; i++) {
            char c = text[i];
            char menor = sinCero ? '1' : '0';
            if (c >= menor && c <= '9') {
                text[i] = (char) (c + (char) (base - '0'));
                continue;
            }
            if (!esDireccionFuerte(c)) {
                continue;
            }
            if (this.rangeSet != null) {
                Range nuevo = this.rangoDe(c);
                if (nuevo != null) {
                    base = nuevo.getNumericBase();
                    sinCero = nuevo.sinCero();
                }
            } else {
                int nuevo = claveDe(c);
                if (nuevo >= 0) {
                    if ((this.mask & (1 << nuevo)) == 0) {
                        nuevo = EUROPEAN_KEY;
                    }
                    base = BASES[nuevo];
                    sinCero = nuevo == ETHIOPIC_KEY;
                }
            }
        }
    }

    /** Ese rango si el conversor lo admite; el latino si no. */
    private Range admitida(Range r) {
        if (this.rangeSet.contains(r)) {
            return r;
        }
        return Range.EUROPEAN;
    }

    /** El rango admitido al que pertenece ese carácter, o `null` si a ninguno. */
    private Range rangoDe(char c) {
        java.util.Iterator<Range> it = this.rangeSet.iterator();
        while (it.hasNext()) {
            Range r = it.next();
            if (r != Range.EUROPEAN && r.contiene(c)) {
                return r;
            }
        }
        if (c < '\u0300') {
            return Range.EUROPEAN;
        }
        return null;
    }

    /** La posición de la escritura a la que pertenece ese carácter, o -1. */
    private static int claveDe(char c) {
        for (int i = 1; i < CTX_LO.length; i++) {
            if (c >= CTX_LO[i] && c < CTX_HI[i]) {
                return i;
            }
        }
        if (c < CTX_HI[0]) {
            return EUROPEAN_KEY;
        }
        return -1;
    }

    /** Si el carácter identifica una escritura por sí solo. */
    private static boolean esDireccionFuerte(char c) {
        byte d = Character.getDirectionality(c);
        return d == Character.DIRECTIONALITY_LEFT_TO_RIGHT
                || d == Character.DIRECTIONALITY_RIGHT_TO_LEFT
                || d == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC;
    }

    public int hashCode() {
        int h = this.mask;
        if (this.rangeSet != null) {
            h = h ^ this.rangeSet.hashCode();
        }
        if (this.contextual) {
            h = h ^ 1;
        }
        return h;
    }

    /** Igualdad por escrituras, contexto inicial y modo. */
    public boolean equals(Object o) {
        if (o == null || o.getClass() != this.getClass()) {
            return false;
        }
        NumericShaper that = (NumericShaper) o;
        if (this.contextual != that.contextual || this.mask != that.mask
                || this.key != that.key) {
            return false;
        }
        if (this.rangeSet == null) {
            return that.rangeSet == null;
        }
        return this.rangeSet.equals(that.rangeSet) && this.shapingRange == that.shapingRange;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append("[contextual:").append(this.contextual);
        if (this.contextual) {
            sb.append(", context:");
            if (this.rangeSet != null) {
                sb.append(this.shapingRange);
            } else {
                sb.append(Integer.toHexString(1 << this.key));
            }
        }
        sb.append(", range(s): ");
        if (this.rangeSet != null) {
            sb.append(this.rangeSet);
        } else {
            sb.append(Integer.toHexString(this.mask));
        }
        sb.append(']');
        return sb.toString();
    }
}
