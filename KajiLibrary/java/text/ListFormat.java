package java.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Une una lista de textos como los une un idioma: {@code "a, b y c"}, no {@code "[a, b, c]"}.
 *
 * <p>Parece trivial y no lo es. La conjunción no va entre todos los elementos sino sólo antes del
 * último; el separador de los del medio puede no ser el mismo que el del primer par; y hay idiomas
 * donde una lista de dos se escribe distinto que las dos primeras de una lista de tres. Por eso los
 * patrones son CINCO y no uno: {@code start}, {@code middle}, {@code end}, {@code two} y
 * {@code three}.
 *
 * <p>Una lista larga se arma componiendo: {@code start} junta los dos primeros, {@code middle} va
 * agregando, y {@code end} pega el último. Las de dos y de tres tienen patrón propio porque en
 * varios idiomas no son un caso particular de la fórmula general.
 *
 * @implNote Las tres fábricas están. Esta nota decía que las dos localizadas quedaban afuera porque
 *           los patrones por locale son datos del CLDR —la "y", la "o", la coma que en japonés es
 *           {@code U+3001}— y poner "and"/"or" para todos daría un resultado plausible y falso en la
 *           mayoría. Sigue siendo cierto lo segundo; lo que cambió es que los datos ya no se
 *           inventan: la tabla trae los patrones <b>exactos</b> de los mismos seis locales que cubre
 *           {@link DecimalFormatSymbols}, extraídos del JDK 25 y no transcriptos a mano, y un locale
 *           desconocido cae en ROOT — que es lo que hace el JDK con un locale del que no tiene
 *           datos.
 *
 * @implNote Lo que sigue siendo un subconjunto son los DATOS, no la superficie: seis locales y no
 *           los cientos del JDK. Ampliarlo es agregar filas a la tabla, no escribir código.
 */
public final class ListFormat extends Format {

    /** Qué relación tiene la lista: enumeración, alternativa o unidades compuestas. */
    public static enum Type {
        STANDARD,
        OR,
        UNIT
    }

    /** Qué tan larga es la forma de la conjunción. */
    public static enum Style {
        FULL,
        SHORT,
        NARROW
    }

    private static final int START = 0;
    private static final int MIDDLE = 1;
    private static final int END = 2;
    private static final int TWO = 3;
    private static final int THREE = 4;

    private final Locale locale;
    private final String[] patterns;

    private ListFormat(Locale locale, String[] patterns) {
        this.locale = locale;
        this.patterns = patterns;
    }

    /**
     * Los locales con datos propios.
     *
     * <p>Son <b>los mismos</b> que los de {@link DecimalFormatSymbols}, y no por casualidad: esta
     * clase lee su tabla con el mismo índice, así que las dos cubren exactamente el mismo conjunto.
     * Atarlas es lo que evita el estado incómodo de tener símbolos de un locale y patrones de otro.
     */
    public static Locale[] getAvailableLocales() {
        return DecimalFormatSymbols.getAvailableLocales();
    }

    /**
     * El formateador de listas del locale por omisión, en la forma estándar y larga.
     *
     * <p>Es {@code getInstance(Locale.getDefault(FORMAT), Type.STANDARD, Style.FULL)}, que es lo
     * que el contrato define. La categoría es {@code FORMAT} y no el default a secas: en una máquina
     * donde el locale de presentación y el de formato difieren --pasa, y se vio contra el JDK 25--
     * son dos respuestas distintas, y la que este método promete es la de formato.
     */
    public static ListFormat getInstance() {
        return ListFormat.getInstance(Locale.getDefault(Locale.Category.FORMAT), Type.STANDARD,
                                      Style.FULL);
    }

    /**
     * El formateador de listas de ese locale, tipo y estilo.
     *
     * <p>Un locale sin datos propios cae en ROOT, que es lo mismo que hace el JDK con un locale del
     * que no tiene datos — no una aproximación de esta biblioteca.
     *
     * @throws NullPointerException si alguno de los tres es null
     */
    public static ListFormat getInstance(Locale locale, Type type, Style style) {
        if (locale == null || type == null || style == null) {
            throw new NullPointerException();
        }
        String[] fila = ListFormat.tabla(type, style)[DecimalFormatSymbols.indexOf(locale)];
        String[] copia = new String[5];
        for (int i = 0; i < 5; i = i + 1) {
            copia[i] = fila[i];
        }
        return new ListFormat(locale, copia);
    }

    // Los patrones del CLDR, en el orden [start, middle, end, two, three] y con una fila por locale,
    // en el mismo orden que la tabla de `DecimalFormatSymbols`: und, en-US, es-AR, de-DE, fr-FR,
    // ja-JP. Index 0 es ROOT, que además es la caída.
    //
    // **No están transcriptos a mano.** Se extrajeron del JDK 25 formateando listas con marcadores
    // únicos y mirando qué quedó entre ellos: un patrón de lista del CLDR siempre tiene la forma
    // `{0}<literal>{1}`, así que el literal es exactamente el texto que separa dos marcadores. Se
    // hizo así porque estos datos son texto traducido y una coma de más en el locale equivocado no
    // la ve nadie hasta que la ve un usuario.
    //
    // Todo carácter no ASCII va como escape `\uXXXX`, por la misma razón que en
    // `DecimalFormatSymbols`: la fuente queda ASCII y no la puede corromper un percance de
    // codificación.
    private static String[][] tabla(Type type, Style style) {
        if (type == Type.STANDARD) {
            if (style == Style.FULL) {
                return new String[][] {
                    {"{0}, {1}", "{0}, {1}", "{0}, {1}", "{0}, {1}", "{0}, {1}, {2}"},
                    {"{0}, {1}", "{0}, {1}", "{0}, and {1}", "{0} and {1}",
                     "{0}, {1}, and {2}"},
                    {"{0}, {1}", "{0}, {1}", "{0} y {1}", "{0} y {1}", "{0}, {1} y {2}"},
                    {"{0}, {1}", "{0}, {1}", "{0} und {1}", "{0} und {1}", "{0}, {1} und {2}"},
                    {"{0}, {1}", "{0}, {1}", "{0} et {1}", "{0} et {1}", "{0}, {1} et {2}"},
                    {"{0}\u3001{1}", "{0}\u3001{1}", "{0}\u3001{1}", "{0}\u3001{1}",
                     "{0}\u3001{1}\u3001{2}"},
                };
            }
            if (style == Style.SHORT) {
                return new String[][] {
                    {"{0}, {1}", "{0}, {1}", "{0}, {1}", "{0}, {1}", "{0}, {1}, {2}"},
                    {"{0}, {1}", "{0}, {1}", "{0}, & {1}", "{0} & {1}", "{0}, {1}, & {2}"},
                    {"{0}, {1}", "{0}, {1}", "{0} y {1}", "{0} y {1}", "{0}, {1} y {2}"},
                    {"{0}, {1}", "{0}, {1}", "{0} und {1}", "{0} und {1}", "{0}, {1} und {2}"},
                    {"{0}, {1}", "{0}, {1}", "{0} et {1}", "{0} et {1}", "{0}, {1} et {2}"},
                    {"{0}\u3001{1}", "{0}\u3001{1}", "{0}\u3001{1}", "{0}\u3001{1}",
                     "{0}\u3001{1}\u3001{2}"},
                };
            }
            return new String[][] {
                {"{0}, {1}", "{0}, {1}", "{0}, {1}", "{0}, {1}", "{0}, {1}, {2}"},
                {"{0}, {1}", "{0}, {1}", "{0}, {1}", "{0}, {1}", "{0}, {1}, {2}"},
                {"{0}, {1}", "{0}, {1}", "{0} y {1}", "{0} y {1}", "{0}, {1} y {2}"},
                {"{0}, {1}", "{0}, {1}", "{0} und {1}", "{0} und {1}", "{0}, {1} und {2}"},
                {"{0}, {1}", "{0}, {1}", "{0}, {1}", "{0}, {1}", "{0}, {1}, {2}"},
                {"{0}\u3001{1}", "{0}\u3001{1}", "{0}\u3001{1}", "{0}\u3001{1}",
                 "{0}\u3001{1}\u3001{2}"},
            };
        }
        if (type == Type.OR) {
            // Las tres formas de OR coinciden en los cinco locales latinos; la japonesa usa
            // "\u307e\u305f\u306f" (mataha) en las tres.
            return new String[][] {
                {"{0}, {1}", "{0}, {1}", "{0}, or {1}", "{0} or {1}", "{0}, {1}, or {2}"},
                {"{0}, {1}", "{0}, {1}", "{0}, or {1}", "{0} or {1}", "{0}, {1}, or {2}"},
                {"{0}, {1}", "{0}, {1}", "{0} o {1}", "{0} o {1}", "{0}, {1} o {2}"},
                {"{0}, {1}", "{0}, {1}", "{0} oder {1}", "{0} oder {1}", "{0}, {1} oder {2}"},
                {"{0}, {1}", "{0}, {1}", "{0} ou {1}", "{0} ou {1}", "{0}, {1} ou {2}"},
                {"{0}\u3001{1}", "{0}\u3001{1}", "{0}\u3001\u307e\u305f\u306f{1}",
                 "{0}\u307e\u305f\u306f{1}", "{0}\u3001{1}\u3001\u307e\u305f\u306f{2}"},
            };
        }
        if (style == Style.FULL) {
            return new String[][] {
                {"{0}, {1}", "{0}, {1}", "{0}, {1}", "{0}, {1}", "{0}, {1}, {2}"},
                {"{0}, {1}", "{0}, {1}", "{0}, {1}", "{0}, {1}", "{0}, {1}, {2}"},
                {"{0}, {1}", "{0}, {1}", "{0} y {1}", "{0} y {1}", "{0}, {1} y {2}"},
                {"{0}, {1}", "{0}, {1}", "{0} und {1}", "{0} und {1}", "{0}, {1} und {2}"},
                {"{0}, {1}", "{0}, {1}", "{0} et {1}", "{0} et {1}", "{0}, {1} et {2}"},
                {"{0}\u3001{1}", "{0}\u3001{1}", "{0}\u3001{1}", "{0}\u3001{1}",
                 "{0}\u3001{1}\u3001{2}"},
            };
        }
        if (style == Style.SHORT) {
            return new String[][] {
                {"{0}, {1}", "{0}, {1}", "{0}, {1}", "{0}, {1}", "{0}, {1}, {2}"},
                {"{0}, {1}", "{0}, {1}", "{0}, {1}", "{0}, {1}", "{0}, {1}, {2}"},
                {"{0}, {1}", "{0}, {1}", "{0}, {1}", "{0} y {1}", "{0}, {1}, {2}"},
                {"{0}, {1}", "{0}, {1}", "{0} und {1}", "{0}, {1}", "{0}, {1} und {2}"},
                {"{0}, {1}", "{0}, {1}", "{0} et {1}", "{0} et {1}", "{0}, {1} et {2}"},
                {"{0} {1}", "{0} {1}", "{0} {1}", "{0} {1}", "{0} {1} {2}"},
            };
        }
        return new String[][] {
            {"{0}, {1}", "{0}, {1}", "{0}, {1}", "{0}, {1}", "{0}, {1}, {2}"},
            {"{0} {1}", "{0} {1}", "{0} {1}", "{0} {1}", "{0} {1} {2}"},
            {"{0} {1}", "{0} {1}", "{0} {1}", "{0} {1}", "{0} {1} {2}"},
            {"{0}, {1}", "{0}, {1}", "{0} und {1}", "{0} und {1}", "{0}, {1} und {2}"},
            {"{0} {1}", "{0} {1}", "{0} {1}", "{0} {1}", "{0} {1} {2}"},
            {"{0}{1}", "{0}{1}", "{0}{1}", "{0}{1}", "{0}{1}{2}"},
        };
    }

    /**
     * Arma un formateador con los cinco patrones dados, en el orden
     * {@code [start, middle, end, two, three]}.
     *
     * @throws IllegalArgumentException si el arreglo no tiene cinco entradas, o si alguna no
     *         referencia los argumentos que le corresponden. La validación es lo que impide que un
     *         patrón mal escrito se descubra recién al formatear, con una lista en la mano.
     */
    public static ListFormat getInstance(String[] patterns) {
        if (patterns == null) {
            throw new NullPointerException();
        }
        if (patterns.length != 5) {
            throw new IllegalArgumentException("Pattern array length should be 5");
        }
        String[] copia = new String[5];
        for (int i = 0; i < 5; i = i + 1) {
            copia[i] = patterns[i];
        }
        ListFormat.verificar(copia[ListFormat.START], 2, "start");
        ListFormat.verificar(copia[ListFormat.MIDDLE], 2, "middle");
        ListFormat.verificar(copia[ListFormat.END], 2, "end");
        ListFormat.verificar(copia[ListFormat.TWO], 2, "two");
        ListFormat.verificar(copia[ListFormat.THREE], 3, "three");
        return new ListFormat(Locale.ROOT, copia);
    }

    private static void verificar(String patron, int cuantos, String nombre) {
        if (patron == null) {
            throw new NullPointerException();
        }
        for (int i = 0; i < cuantos; i = i + 1) {
            if (patron.indexOf("{" + Integer.toString(i) + "}") < 0) {
                throw new IllegalArgumentException("pattern for " + nombre + " is incorrect: "
                        + patron);
            }
        }
    }

    /**
     * El locale de este formateador. Para uno armado con patrones explícitos es {@code ROOT}: los
     * patrones no vinieron de ningún locale y decir otra cosa sería atribuirles un origen.
     */
    public Locale getLocale() {
        return this.locale;
    }

    public String[] getPatterns() {
        String[] out = new String[5];
        for (int i = 0; i < 5; i = i + 1) {
            out[i] = this.patterns[i];
        }
        return out;
    }

    public String format(List<String> input) {
        return this.format(input, new StringBuffer(), new FieldPosition(0)).toString();
    }

    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        List<String> lista = ListFormat.aLista(obj);
        MessageFormat mf = new MessageFormat(this.patronPara(lista.size()), this.locale);
        return mf.format(lista.toArray(), toAppendTo, pos);
    }

    public AttributedCharacterIterator formatToCharacterIterator(Object obj) {
        List<String> lista = ListFormat.aLista(obj);
        MessageFormat mf = new MessageFormat(this.patronPara(lista.size()), this.locale);
        return mf.formatToCharacterIterator(lista.toArray());
    }

    private static List<String> aLista(Object obj) {
        if (obj == null) {
            throw new NullPointerException();
        }
        List<String> out = new ArrayList<String>();
        if (obj instanceof List) {
            List<?> l = (List<?>) obj;
            for (int i = 0; i < l.size(); i = i + 1) {
                Object o = l.get(i);
                if (o == null) {
                    throw new NullPointerException();
                }
                out.add(o.toString());
            }
        } else if (obj instanceof Object[]) {
            Object[] arr = (Object[]) obj;
            for (int i = 0; i < arr.length; i = i + 1) {
                if (arr[i] == null) {
                    throw new NullPointerException();
                }
                out.add(arr[i].toString());
            }
        } else {
            throw new IllegalArgumentException("Cannot format given Object as a List");
        }
        if (out.isEmpty()) {
            throw new IllegalArgumentException("There should at least be one input string");
        }
        return out;
    }

    // El patrón de MessageFormat que corresponde a N elementos. Uno solo no se junta con nada; dos
    // y tres tienen patrón propio; de cuatro en adelante se compone start + middle... + end.
    private String patronPara(int n) {
        if (n == 1) {
            return "{0}";
        }
        if (n == 2) {
            return this.patterns[ListFormat.TWO];
        }
        if (n == 3) {
            return this.patterns[ListFormat.THREE];
        }
        String acc = "{0}";
        for (int i = 1; i < n; i = i + 1) {
            String p;
            if (i == 1) {
                p = this.patterns[ListFormat.START];
            } else if (i == n - 1) {
                p = this.patterns[ListFormat.END];
            } else {
                p = this.patterns[ListFormat.MIDDLE];
            }
            acc = ListFormat.sustituir(p, acc, "{" + Integer.toString(i) + "}");
        }
        return acc;
    }

    // Sustitución textual de {0} y {1}, hecha en UNA pasada: reemplazar {0} y después {1} sobre el
    // resultado volvería a tocar las llaves que acaba de insertar el primer reemplazo.
    private static String sustituir(String patron, String cero, String uno) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < patron.length()) {
            if (i + 2 < patron.length() && patron.charAt(i) == '{' && patron.charAt(i + 2) == '}') {
                char d = patron.charAt(i + 1);
                if (d == '0') {
                    sb.append(cero);
                    i = i + 3;
                    continue;
                }
                if (d == '1') {
                    sb.append(uno);
                    i = i + 3;
                    continue;
                }
            }
            sb.append(patron.charAt(i));
            i = i + 1;
        }
        return sb.toString();
    }

    /**
     * Recupera la lista de un texto que este formateador podría haber producido.
     *
     * <p>Se prueba con una cantidad de elementos y otra hasta que una encaje entera, empezando por
     * la más grande posible. De la más grande a la más chica y no al revés: con patrones donde el
     * separador del medio aparece también dentro del último par, la lectura corta encajaría igual y
     * se comería elementos.
     */
    public List<String> parse(String source) throws ParseException {
        ParsePosition pos = new ParsePosition(0);
        Object r = this.parseObject(source, pos);
        if (r == null) {
            throw new ParseException("Parse failed", pos.getErrorIndex());
        }
        return (List<String>) r;
    }

    public Object parseObject(String source, ParsePosition parsePos) {
        if (source == null) {
            throw new NullPointerException();
        }
        int inicio = parsePos.getIndex();
        int maximo = source.length() + 1;
        for (int n = maximo; n >= 1; n = n - 1) {
            MessageFormat mf = new MessageFormat(this.patronPara(n), this.locale);
            ParsePosition p = new ParsePosition(inicio);
            Object[] got = mf.parse(source, p);
            if (got != null && p.getIndex() == source.length() && got.length == n) {
                List<String> out = new ArrayList<String>();
                boolean completo = true;
                for (int i = 0; i < n; i = i + 1) {
                    if (got[i] == null) {
                        completo = false;
                    } else {
                        out.add(got[i].toString());
                    }
                }
                if (completo) {
                    parsePos.setIndex(source.length());
                    return out;
                }
            }
        }
        parsePos.setErrorIndex(inicio);
        return null;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }
        ListFormat other = (ListFormat) obj;
        if (!this.locale.equals(other.locale)) {
            return false;
        }
        for (int i = 0; i < 5; i = i + 1) {
            if (!this.patterns[i].equals(other.patterns[i])) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        int h = this.locale.hashCode();
        for (int i = 0; i < 5; i = i + 1) {
            h = h * 31 + this.patterns[i].hashCode();
        }
        return h;
    }

    public String toString() {
        return "ListFormat [locale: \"" + this.locale.toString()
                + "\", start: \"" + this.patterns[ListFormat.START]
                + "\", middle: \"" + this.patterns[ListFormat.MIDDLE]
                + "\", end: \"" + this.patterns[ListFormat.END]
                + "\", two: \"" + this.patterns[ListFormat.TWO]
                + "\", three: \"" + this.patterns[ListFormat.THREE] + "\"]";
    }
}
