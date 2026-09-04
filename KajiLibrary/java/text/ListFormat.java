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
 * @implNote <b>Sólo está la fábrica de patrones explícitos.</b> {@code getInstance()} y
 *           {@code getInstance(Locale, Type, Style)} quedaron afuera: los patrones por locale son
 *           datos del CLDR y traen texto traducido —la "y", la "o", la coma que en japonés es
 *           {@code U+3001}— que esta biblioteca no tiene. Poner "and"/"or" para todos los locales
 *           daría un resultado plausible y falso en la mayoría. {@link #getInstance(String[])} sí
 *           está, porque el llamador aporta los patrones y no hay nada que inventar; y
 *           {@link #getAvailableLocales()} devuelve un arreglo VACÍO, que es la verdad: no hay
 *           ningún locale para el que se pueda devolver una instancia localizada.
 *
 * @implNote Los enums {@link ListFormat.Type} y {@link ListFormat.Style} están aunque hoy ninguna
 *           fábrica los reciba: son parte de la forma pública del tipo, y sacarlos rompería el
 *           código que los nombra sin ganar nada.
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
     * Los locales con datos propios: ninguno.
     *
     * <p>Un arreglo vacío no es un hueco sin llenar, es la respuesta correcta al contrato — "los
     * locales para los que se pueden obtener instancias localizadas". Sin datos del CLDR de listas,
     * no hay ninguno.
     */
    public static Locale[] getAvailableLocales() {
        return new Locale[0];
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
