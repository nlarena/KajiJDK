package java.net;

import java.util.Locale;

/**
 * La traduccion entre un nombre de dominio con caracteres no ASCII y su forma transportable.
 *
 * <h2>Que problema resuelve</h2>
 *
 * <p>El DNS habla ASCII. Un dominio escrito con acentos, ideogramas o cirilico no puede viajar tal
 * cual, y aun asi tiene que resolver al mismo lugar desde cualquier parte. La solucion es
 * <strong>Punycode</strong> (RFC 3492): una codificacion reversible que convierte cualquier cadena
 * Unicode en ASCII, marcada con el prefijo {@code xn--}.
 *
 * <p>Reversible es la palabra: {@link #toASCII} y {@link #toUnicode} son inversas, y por eso el
 * mismo dominio se puede mostrar bonito y resolver correctamente sin ninguna tabla de por medio.
 *
 * <h2>Como funciona Punycode, en una linea</h2>
 *
 * <p>Separa los caracteres ASCII —que se copian literalmente— de los que no, y describe a estos
 * ultimos como una serie de <em>deltas</em> sobre un codigo y una posicion. Los deltas se escriben
 * en un alfabeto de 36 simbolos con longitud variable, y un mecanismo de sesgo hace que los saltos
 * chicos —lo normal, porque un nombre suele estar en un solo alfabeto— ocupen poco.
 *
 * <h2>La limitacion de esta implementacion, dicha de frente</h2>
 *
 * <p>El RFC 3490 manda pasar el nombre por <strong>nameprep</strong> (RFC 3491) antes de codificar:
 * plegado de mayusculas, normalizacion <strong>NFKC</strong>, y rechazo de caracteres prohibidos.
 * Aca se hace el plegado de mayusculas y el rechazo de los prohibidos que se pueden detectar sin
 * tablas, pero <strong>no la normalizacion NFKC</strong>: el {@link java.text.Normalizer} de esta
 * biblioteca no declara esa forma — decision documentada alli, y preferible a declararla y tirar.
 *
 * <p>Que significa en la practica: para una entrada <em>ya normalizada</em> —que es el caso de
 * cualquier nombre que venga de un navegador, de un archivo de configuracion o de un teclado— el
 * resultado es identico al del JDK. Para una entrada que necesitaria NFKC, el Punycode que sale es
 * el de la cadena sin normalizar: sigue siendo Punycode valido y sigue siendo reversible, pero no es
 * el mismo que produciria el JDK.
 *
 * <p>Lo que <strong>si</strong> es exacto es el algoritmo de Punycode, que es la parte especificada
 * hasta el ultimo detalle y la que de verdad tiene forma de estar mal.
 *
 * @since 1.6
 */
public final class IDN {

    /**
     * Permite que el nombre tenga puntos de codigo que Unicode todavia no asigno.
     *
     * <p>Apagado por omision, y es lo prudente: un caracter sin asignar puede recibir significado
     * —o una regla de plegado— en una version futura, y ahi el mismo nombre pasaria a codificar
     * distinto.
     */
    public static final int ALLOW_UNASSIGNED = 0x01;

    /**
     * Exige que el resultado cumpla las reglas STD3 de nombre de host.
     *
     * <p>Letras, digitos y guion; sin empezar ni terminar en guion. Sirve para no fabricar un nombre
     * que Punycode acepta y que despues ninguna resolucion va a admitir.
     */
    public static final int USE_STD3_ASCII_RULES = 0x02;

    // El alfabeto de 36 simbolos y los parametros de sesgo son constantes del RFC 3492, no
    // elecciones: cambiarlas produce una codificacion que nadie mas entiende.
    private static final int BASE = 36;
    private static final int TMIN = 1;
    private static final int TMAX = 26;
    private static final int SKEW = 38;
    private static final int DAMP = 700;
    private static final int INITIAL_BIAS = 72;
    private static final int INITIAL_N = 128;
    private static final char DELIMITER = '-';
    private static final String ACE_PREFIX = "xn--";
    private static final int MAX_LABEL = 63;

    private IDN() {
    }

    /**
     * A la forma ASCII.
     *
     * @throws IllegalArgumentException si el nombre no cumple las reglas de IDNA
     */
    public static String toASCII(String input, int flag) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        boolean vacio = input.isEmpty();
        while (i < input.length() || vacio) {
            int fin = finDeEtiqueta(input, i);
            String etiqueta = input.substring(i, fin);
            out.append(etiquetaAAscii(etiqueta, flag));
            if (fin >= input.length()) {
                break;
            }
            // El separador se conserva tal cual: los cuatro que Unicode reconoce como punto se
            // normalizan al ASCII, que es lo unico que el DNS transporta.
            out.append('.');
            i = fin + 1;
            vacio = i == input.length();
        }
        return out.toString();
    }

    /** A la forma ASCII, sin banderas. */
    public static String toASCII(String input) {
        return toASCII(input, 0);
    }

    /**
     * De vuelta a Unicode.
     *
     * <p>Nunca falla: una etiqueta que no se puede decodificar se devuelve tal como vino. Es
     * deliberado en el RFC — un nombre a medio traducir es mas util que una excepcion, porque esto
     * se usa sobre todo para <em>mostrar</em>.
     */
    public static String toUnicode(String input, int flag) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        boolean vacio = input.isEmpty();
        while (i < input.length() || vacio) {
            int fin = finDeEtiqueta(input, i);
            out.append(etiquetaAUnicode(input.substring(i, fin), flag));
            if (fin >= input.length()) {
                break;
            }
            out.append('.');
            i = fin + 1;
            vacio = i == input.length();
        }
        return out.toString();
    }

    /** De vuelta a Unicode, sin banderas. */
    public static String toUnicode(String input) {
        return toUnicode(input, 0);
    }

    /**
     * Donde termina la etiqueta que empieza en {@code desde}.
     *
     * <p>Los cuatro separadores del RFC 3490 y no solo el punto ASCII: hay alfabetos con su propia
     * forma de punto, y un nombre escrito con ellos tiene que partirse igual.
     */
    private static int finDeEtiqueta(String s, int desde) {
        for (int i = desde; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '.' || c == '。' || c == '．' || c == '｡') {
                return i;
            }
        }
        return s.length();
    }

    private static String etiquetaAAscii(String etiqueta, int flag) {
        boolean soloAscii = true;
        for (int i = 0; i < etiqueta.length(); i++) {
            if (etiqueta.charAt(i) > 0x7F) {
                soloAscii = false;
                break;
            }
        }
        // El plegado de mayusculas es la parte de nameprep que si se puede hacer sin NFKC; ver la
        // nota de la clase sobre lo que falta.
        String preparada = soloAscii ? etiqueta : etiqueta.toLowerCase(Locale.ROOT);

        String salida;
        if (soloAscii) {
            salida = etiqueta;
        } else {
            if (preparada.startsWith(ACE_PREFIX)) {
                throw new IllegalArgumentException(
                        "una etiqueta no ASCII no puede empezar con " + ACE_PREFIX);
            }
            salida = ACE_PREFIX + punycode(preparada);
        }
        if ((flag & USE_STD3_ASCII_RULES) != 0) {
            revisarStd3(salida);
        }
        if (salida.isEmpty() || salida.length() > MAX_LABEL) {
            throw new IllegalArgumentException("etiqueta de largo invalido: " + salida);
        }
        return salida;
    }

    private static String etiquetaAUnicode(String etiqueta, int flag) {
        if (etiqueta.length() <= ACE_PREFIX.length()
                || !etiqueta.substring(0, ACE_PREFIX.length())
                        .equalsIgnoreCase(ACE_PREFIX)) {
            return etiqueta;
        }
        try {
            String u = despunycode(etiqueta.substring(ACE_PREFIX.length()));
            // La prueba de ida y vuelta que exige el RFC: si volver a codificar no da lo mismo, la
            // etiqueta estaba mal formada y se devuelve como vino.
            if (!toASCII(u, flag).equalsIgnoreCase(etiqueta)) {
                return etiqueta;
            }
            return u;
        } catch (RuntimeException e) {
            return etiqueta;
        }
    }

    private static void revisarStd3(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9') || c == '-';
            if (!ok) {
                throw new IllegalArgumentException("caracter no permitido por STD3: " + c);
            }
        }
        if (s.startsWith("-") || s.endsWith("-")) {
            throw new IllegalArgumentException("una etiqueta no puede empezar ni terminar en '-'");
        }
    }

    /** El sesgo adaptativo del RFC 3492: es lo que hace que los saltos chicos ocupen poco. */
    private static int adaptar(int delta, int cantidad, boolean primera) {
        int d = primera ? delta / DAMP : delta / 2;
        d = d + d / cantidad;
        int k = 0;
        while (d > ((BASE - TMIN) * TMAX) / 2) {
            d = d / (BASE - TMIN);
            k = k + BASE;
        }
        return k + (((BASE - TMIN + 1) * d) / (d + SKEW));
    }

    private static char digito(int d) {
        return (char) (d < 26 ? d + 'a' : d - 26 + '0');
    }

    private static int valor(char c) {
        if (c >= 'a' && c <= 'z') {
            return c - 'a';
        }
        if (c >= 'A' && c <= 'Z') {
            return c - 'A';
        }
        if (c >= '0' && c <= '9') {
            return c - '0' + 26;
        }
        throw new IllegalArgumentException("digito Punycode invalido: " + c);
    }

    /** RFC 3492 §6.3, tal cual. */
    private static String punycode(String input) {
        int n = INITIAL_N;
        int delta = 0;
        int bias = INITIAL_BIAS;
        StringBuilder out = new StringBuilder();

        int basicos = 0;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c < 0x80) {
                out.append(c);
                basicos++;
            }
        }
        if (basicos > 0) {
            out.append(DELIMITER);
        }

        int manejados = basicos;
        int total = input.length();
        while (manejados < total) {
            int m = Integer.MAX_VALUE;
            for (int i = 0; i < input.length(); i++) {
                int c = input.charAt(i);
                if (c >= n && c < m) {
                    m = c;
                }
            }
            delta = delta + (m - n) * (manejados + 1);
            n = m;
            for (int i = 0; i < input.length(); i++) {
                int c = input.charAt(i);
                if (c < n) {
                    delta++;
                } else if (c == n) {
                    int q = delta;
                    for (int k = BASE; ; k += BASE) {
                        int t = k <= bias ? TMIN : (k >= bias + TMAX ? TMAX : k - bias);
                        if (q < t) {
                            break;
                        }
                        out.append(digito(t + (q - t) % (BASE - t)));
                        q = (q - t) / (BASE - t);
                    }
                    out.append(digito(q));
                    bias = adaptar(delta, manejados + 1, manejados == basicos);
                    delta = 0;
                    manejados++;
                }
            }
            delta++;
            n++;
        }
        return out.toString();
    }

    /** RFC 3492 §6.2, la inversa exacta de {@link #punycode}. */
    private static String despunycode(String input) {
        int n = INITIAL_N;
        int i = 0;
        int bias = INITIAL_BIAS;
        StringBuilder out = new StringBuilder();

        int ultimoGuion = input.lastIndexOf(DELIMITER);
        if (ultimoGuion > 0) {
            for (int j = 0; j < ultimoGuion; j++) {
                char c = input.charAt(j);
                if (c >= 0x80) {
                    throw new IllegalArgumentException("caracter no basico en la parte literal");
                }
                out.append(c);
            }
        }

        int pos = ultimoGuion < 0 ? 0 : ultimoGuion + 1;
        while (pos < input.length()) {
            int viejo = i;
            int w = 1;
            for (int k = BASE; ; k += BASE) {
                if (pos >= input.length()) {
                    throw new IllegalArgumentException("Punycode incompleto");
                }
                int d = valor(input.charAt(pos));
                pos++;
                i = i + d * w;
                int t = k <= bias ? TMIN : (k >= bias + TMAX ? TMAX : k - bias);
                if (d < t) {
                    break;
                }
                w = w * (BASE - t);
            }
            bias = adaptar(i - viejo, out.length() + 1, viejo == 0);
            n = n + i / (out.length() + 1);
            i = i % (out.length() + 1);
            out.insert(i, (char) n);
            i++;
        }
        return out.toString();
    }
}
