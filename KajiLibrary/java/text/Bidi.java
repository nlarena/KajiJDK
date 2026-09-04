package java.text;

/**
 * El orden de dibujado de un texto que mezcla escrituras de izquierda a derecha y de derecha a
 * izquierda.
 *
 * <p>Un {@code String} guarda el texto en orden LÓGICO —el orden en que se lee— y eso no alcanza
 * para dibujarlo: en "el archivo ‏שלום‎ está acá", la palabra hebrea se dibuja al revés y el resto
 * no, y dónde empieza y termina cada tramo no se decide carácter por carácter. Esta clase corre el
 * algoritmo de Unicode (UAX #9) y contesta lo único que hace falta: qué NIVEL tiene cada carácter.
 * Nivel par es izquierda a derecha, impar es derecha a izquierda, y el número dice cuánto está
 * anidado.
 *
 * <p>De ahí sale todo el resto de la API. Las CORRIDAS son los tramos de nivel constante, que son
 * las unidades que un dibujante puede tratar como una sola pieza. {@link #reorderVisually} las
 * ordena para pintar. Y {@link #createLineBidi} existe porque cortar en líneas cambia el resultado:
 * el espacio final de una línea vuelve a la dirección del párrafo, cosa que en el medio del texto no
 * pasaría.
 *
 * @implNote El algoritmo está completo salvo la regla N0 (pares de corchetes, agregada en Unicode
 *           6.3), que necesita la tabla {@code BidiBrackets.txt}. Sin ella los corchetes se
 *           resuelven como neutros comunes, que es lo que hacía el algoritmo antes de 6.3; la
 *           diferencia aparece sólo cuando un par de corchetes encierra texto de dirección contraria
 *           a la de alrededor. Ver {@code AlgoritmoBidi} para el detalle y para por qué no se
 *           transcribió media tabla.
 */
public final class Bidi {

    /** Base de izquierda a derecha, sin mirar el texto. */
    public static final int DIRECTION_LEFT_TO_RIGHT = 0;

    /** Base de derecha a izquierda, sin mirar el texto. */
    public static final int DIRECTION_RIGHT_TO_LEFT = 1;

    /** La base sale del primer carácter fuerte; si no hay ninguno, izquierda a derecha. */
    public static final int DIRECTION_DEFAULT_LEFT_TO_RIGHT = -2;

    /** La base sale del primer carácter fuerte; si no hay ninguno, derecha a izquierda. */
    public static final int DIRECTION_DEFAULT_RIGHT_TO_LEFT = -1;

    private final byte[] niveles;
    private final byte nivelBase;
    private final int largo;

    public Bidi(String paragraph, int flags) {
        this(Bidi.aChars(paragraph), 0, null, 0, Bidi.largoDe(paragraph), flags);
    }

    /**
     * Corre el algoritmo sobre el texto de un iterador con atributos.
     *
     * <p>Los atributos que mira son los dos que el JDK define para esto: {@code RUN_DIRECTION} para
     * la dirección base y {@code BIDI_EMBEDDING} para los encajes impuestos. Los dos los define
     * {@code java.awt.font.TextAttribute}, que no es parte de {@code java.base} y no existe acá; sin
     * esas claves no hay forma de leerlos, así que este constructor usa el texto y la dirección
     * deducida del primer fuerte. Es el mismo resultado que da el JDK cuando el iterador no trae
     * esos atributos, que es el caso de todo iterador construido dentro de {@code java.base}.
     */
    public Bidi(AttributedCharacterIterator paragraph) {
        this(Bidi.textoDe(paragraph), 0, null, 0, Bidi.textoDe(paragraph).length,
                Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);
    }

    /**
     * Corre el algoritmo con encajes impuestos por el llamador.
     *
     * @param embeddings uno por carácter: positivo es un encaje (el texto conserva su dirección
     *                   propia dentro de ese nivel) y negativo una anulación (la dirección del nivel
     *                   manda). Cero deja decidir al algoritmo. {@code null} equivale a todo cero.
     */
    public Bidi(char[] text, int textStart, byte[] embeddings, int embStart, int paragraphLength,
                int flags) {
        if (text == null) {
            throw new NullPointerException();
        }
        if (textStart < 0 || paragraphLength < 0 || textStart + paragraphLength > text.length) {
            throw new IllegalArgumentException("Invalid text range");
        }
        if (embeddings != null && (embStart < 0 || embStart + paragraphLength > embeddings.length)) {
            throw new IllegalArgumentException("Invalid embeddings range");
        }
        char[] trozo = new char[paragraphLength];
        for (int i = 0; i < paragraphLength; i = i + 1) {
            trozo[i] = text[textStart + i];
        }
        byte[] enc = null;
        if (embeddings != null) {
            enc = new byte[paragraphLength];
            boolean alguno = false;
            for (int i = 0; i < paragraphLength; i = i + 1) {
                enc[i] = embeddings[embStart + i];
                if (enc[i] != 0) {
                    alguno = true;
                }
            }
            if (!alguno) {
                enc = null;
            }
        }
        int base;
        if (flags == Bidi.DIRECTION_LEFT_TO_RIGHT) {
            base = 0;
        } else if (flags == Bidi.DIRECTION_RIGHT_TO_LEFT) {
            base = 1;
        } else if (flags == Bidi.DIRECTION_DEFAULT_RIGHT_TO_LEFT) {
            base = -1;
        } else {
            base = -2;
        }
        AlgoritmoBidi alg = new AlgoritmoBidi(trozo, enc, base);
        byte[] lv = alg.niveles();
        byte nb = alg.nivelParrafo();
        Bidi.aplanarSiUniforme(lv, nb);
        this.niveles = lv;
        this.nivelBase = nb;
        this.largo = paragraphLength;
    }

    /**
     * Cuando TODO el texto va en la dirección de la base, los niveles se aplanan al nivel base.
     *
     * <p>Es lo que hace el JDK, y no es cosmético: un texto que resolvió a niveles 3-3-3-1 con base
     * 1 se dibuja exactamente igual que uno de niveles 1-1-1-1 —el orden visual no cambia si nunca
     * hay un cambio de dirección— y el JDK informa una sola corrida en lugar de dos. Reportar los
     * niveles internos ahí daría un {@code getRunCount()} distinto del suyo sin que ningún
     * dibujante viera la diferencia.
     *
     * <p>Alcanza con mirar la paridad: si algún nivel tiene paridad distinta de la base, hay un
     * cambio de dirección y los niveles se conservan tal cual.
     */
    private static void aplanarSiUniforme(byte[] niveles, byte nivelBase) {
        int paridad = nivelBase & 1;
        for (int i = 0; i < niveles.length; i = i + 1) {
            if ((niveles[i] & 1) != paridad) {
                return;
            }
        }
        for (int i = 0; i < niveles.length; i = i + 1) {
            niveles[i] = nivelBase;
        }
    }

    // Constructor interno para createLineBidi: los niveles ya están resueltos y sólo hay que
    // recortar y volver a aplicar L1 sobre el nuevo final de línea.
    private Bidi(byte[] niveles, byte nivelBase) {
        this.niveles = niveles;
        this.nivelBase = nivelBase;
        this.largo = niveles.length;
    }

    private static char[] aChars(String s) {
        if (s == null) {
            throw new NullPointerException();
        }
        return s.toCharArray();
    }

    private static int largoDe(String s) {
        if (s == null) {
            throw new NullPointerException();
        }
        return s.length();
    }

    private static char[] textoDe(AttributedCharacterIterator it) {
        if (it == null) {
            throw new NullPointerException();
        }
        int desde = it.getBeginIndex();
        int hasta = it.getEndIndex();
        char[] out = new char[hasta - desde];
        for (int i = desde; i < hasta; i = i + 1) {
            it.setIndex(i);
            out[i - desde] = it.current();
        }
        return out;
    }

    /**
     * El Bidi de una línea recortada de este párrafo.
     *
     * <p>No se vuelve a correr el algoritmo: los niveles del párrafo ya son los correctos y
     * recalcularlos sobre el trozo daría OTRO resultado, porque el trozo no ve el contexto. Lo único
     * que cambia es L1 —el espacio del final de la línea vuelve a la dirección del párrafo—, y eso
     * sí depende de dónde se cortó.
     */
    public Bidi createLineBidi(int lineStart, int lineLimit) {
        if (lineStart < 0 || lineLimit < lineStart || lineLimit > this.largo) {
            throw new IllegalArgumentException("Invalid line range");
        }
        int m = lineLimit - lineStart;
        byte[] sub = new byte[m];
        for (int i = 0; i < m; i = i + 1) {
            sub[i] = this.niveles[lineStart + i];
        }
        int i = m - 1;
        while (i >= 0 && sub[i] == this.nivelBase) {
            i = i - 1;
        }
        return new Bidi(sub, this.nivelBase);
    }

    public boolean isMixed() {
        return !this.isLeftToRight() && !this.isRightToLeft();
    }

    public boolean isLeftToRight() {
        if ((this.nivelBase & 1) != 0) {
            return false;
        }
        for (int i = 0; i < this.largo; i = i + 1) {
            if ((this.niveles[i] & 1) != 0) {
                return false;
            }
        }
        return true;
    }

    public boolean isRightToLeft() {
        if ((this.nivelBase & 1) == 0) {
            return false;
        }
        for (int i = 0; i < this.largo; i = i + 1) {
            if ((this.niveles[i] & 1) == 0) {
                return false;
            }
        }
        return true;
    }

    public int getLength() {
        return this.largo;
    }

    public boolean baseIsLeftToRight() {
        return (this.nivelBase & 1) == 0;
    }

    public int getBaseLevel() {
        return this.nivelBase;
    }

    public int getLevelAt(int offset) {
        // Fuera de rango devuelve el nivel base en lugar de reventar: es lo que hace el JDK, y le
        // ahorra al que dibuja una comprobación en el borde de cada línea.
        if (offset < 0 || offset >= this.largo) {
            return this.nivelBase;
        }
        return this.niveles[offset];
    }

    /** Cuántos tramos de nivel constante hay. Cada uno se dibuja como una pieza. */
    public int getRunCount() {
        if (this.largo == 0) {
            return 0;
        }
        int c = 1;
        for (int i = 1; i < this.largo; i = i + 1) {
            if (this.niveles[i] != this.niveles[i - 1]) {
                c = c + 1;
            }
        }
        return c;
    }

    public int getRunLevel(int run) {
        return this.niveles[this.inicioDeCorrida(run)];
    }

    public int getRunStart(int run) {
        return this.inicioDeCorrida(run);
    }

    public int getRunLimit(int run) {
        int i = this.inicioDeCorrida(run) + 1;
        while (i < this.largo && this.niveles[i] == this.niveles[i - 1]) {
            i = i + 1;
        }
        return i;
    }

    private int inicioDeCorrida(int run) {
        if (run < 0) {
            throw new IllegalArgumentException("Invalid run index " + run);
        }
        int c = 0;
        for (int i = 0; i < this.largo; i = i + 1) {
            if (i == 0 || this.niveles[i] != this.niveles[i - 1]) {
                if (c == run) {
                    return i;
                }
                c = c + 1;
            }
        }
        throw new IllegalArgumentException("Invalid run index " + run);
    }

    /**
     * Si el texto necesita el algoritmo, o si alcanza con dibujarlo de izquierda a derecha.
     *
     * <p>Existe para poder saltear el algoritmo entero en el caso normal, que es el más común: un
     * texto sin nada de derecha a izquierda y sin controles se dibuja en orden lógico y no hace
     * falta calcular ni un nivel.
     */
    public static boolean requiresBidi(char[] text, int start, int limit) {
        if (text == null) {
            throw new NullPointerException();
        }
        if (start < 0 || limit > text.length || start > limit) {
            throw new IllegalArgumentException("Invalid range");
        }
        for (int i = start; i < limit; i = i + 1) {
            byte t = Character.getDirectionality(text[i]);
            if (t == AlgoritmoBidi.R || t == AlgoritmoBidi.AL || t == AlgoritmoBidi.AN
                    || t == AlgoritmoBidi.RLE || t == AlgoritmoBidi.RLO
                    || t == AlgoritmoBidi.LRE || t == AlgoritmoBidi.LRO
                    || t == AlgoritmoBidi.PDF || t == AlgoritmoBidi.LRI
                    || t == AlgoritmoBidi.RLI || t == AlgoritmoBidi.FSI
                    || t == AlgoritmoBidi.PDI) {
                return true;
            }
        }
        return false;
    }

    /**
     * Reordena los objetos al orden visual (regla L2).
     *
     * <p>La regla es corta y no obvia: de mayor a menor nivel, se DA VUELTA cada tramo contiguo de
     * nivel mayor o igual. Hacerlo de una sola pasada no funciona — un tramo de nivel 2 dentro de
     * uno de nivel 1 tiene que darse vuelta dos veces, y la segunda lo devuelve a su orden interno.
     */
    public static void reorderVisually(byte[] levels, int levelStart, Object[] objects,
                                       int objectStart, int count) {
        if (levels == null || objects == null) {
            throw new NullPointerException();
        }
        if (count < 0 || levelStart < 0 || objectStart < 0
                || levelStart + count > levels.length || objectStart + count > objects.length) {
            throw new IllegalArgumentException("Invalid range");
        }
        if (count == 0) {
            return;
        }
        byte maximo = 0;
        byte minImpar = (byte) (AlgoritmoBidi.MAX_DEPTH + 1);
        for (int i = 0; i < count; i = i + 1) {
            byte l = levels[levelStart + i];
            if (l > maximo) {
                maximo = l;
            }
            if ((l & 1) != 0 && l < minImpar) {
                minImpar = l;
            }
        }
        for (byte nivel = maximo; nivel >= minImpar; nivel = (byte) (nivel - 1)) {
            int i = 0;
            while (i < count) {
                if (levels[levelStart + i] < nivel) {
                    i = i + 1;
                    continue;
                }
                int fin = i;
                while (fin < count && levels[levelStart + fin] >= nivel) {
                    fin = fin + 1;
                }
                int a = objectStart + i;
                int b = objectStart + fin - 1;
                while (a < b) {
                    Object tmp = objects[a];
                    objects[a] = objects[b];
                    objects[b] = tmp;
                    a = a + 1;
                    b = b - 1;
                }
                i = fin;
            }
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("java.text.Bidi[direction: ");
        if (this.isMixed()) {
            sb.append("mixed");
        } else if (this.isLeftToRight()) {
            sb.append("ltr");
        } else {
            sb.append("rtl");
        }
        sb.append(" baselevel: ");
        sb.append(Integer.toString(this.nivelBase));
        sb.append(" length: ");
        sb.append(Integer.toString(this.largo));
        sb.append(" runs:");
        int c = this.getRunCount();
        for (int i = 0; i < c; i = i + 1) {
            sb.append(" ");
            sb.append(Integer.toString(this.getRunStart(i)));
            sb.append("-");
            sb.append(Integer.toString(this.getRunLimit(i)));
            sb.append("(");
            sb.append(Integer.toString(this.getRunLevel(i)));
            sb.append(")");
        }
        sb.append("]");
        return sb.toString();
    }
}
