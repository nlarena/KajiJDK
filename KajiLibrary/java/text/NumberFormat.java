package java.text;

import java.io.InvalidObjectException;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

// KajiLibrary's java.text.NumberFormat — the abstract base for number formatters, and the layer
// that turns Format's Object-shaped contract into a numeric one.
//
// Its job in the hierarchy is dispatch: Format speaks Object, but a number formatter wants a
// primitive, and `double` and `long` are NOT interchangeable — a long past 2^53 cannot round-trip
// through a double. So NumberFormat declares two abstract seams, one per primitive, and routes
// Object to whichever one preserves the value.
//
// La otra mitad de esta clase es ESTADO, no comportamiento: cuántos dígitos como mínimo, cuántos
// como máximo, si se agrupa, si al parsear se corta en el punto. Vive acá y no en DecimalFormat
// porque es la parte del contrato que un llamador puede ajustar sin saber qué implementación tiene
// enfrente.
//
// Sobre `getCompactNumberInstance()` y `getCompactNumberInstance(Locale, Style)`: esta nota decía
// que quedaban afuera porque la tabla de patrones compactos del CLDR —"0 mil", "0 millones", "0万",
// "0億"— es texto traducido por locale y por regla de plural, y rellenarla con "K/M/B" para todos
// daría un resultado plausible y falso en la mayoría. Lo segundo sigue siendo cierto; lo que cambió
// es que los datos ya no se inventan.
//
// La tabla de abajo trae los patrones **exactos** de los mismos seis locales que cubre
// `DecimalFormatSymbols`, y no están transcriptos a mano: se leyeron del JDK 25 por reflexión sobre
// el campo `compactPatterns` de su propio `CompactNumberFormat`. Un locale desconocido cae en ROOT,
// que es lo que hace el JDK con un locale del que no tiene datos.
//
// `CompactNumberFormat` ya sabía leerlos: evalúa las variantes `{one:... other:...}` contra las
// reglas de plural del locale. Lo único que le faltaba era de dónde sacarlas.
public abstract class NumberFormat extends Format {

    /**
     * Marca el campo entero para {@link FieldPosition}. Convive con {@link java.text.NumberFormat.Field#INTEGER},
     * que es la forma nueva de nombrar lo mismo; ninguna de las dos reemplaza a la otra en una API
     * ya publicada.
     */
    public static final int INTEGER_FIELD = 0;

    /** Marca el campo fraccionario para {@link FieldPosition}. */
    public static final int FRACTION_FIELD = 1;

    /**
     * La clave con la que un formateador numérico marca cada pedazo del texto que produjo.
     *
     * <p>Es lo que permite preguntar "¿dónde quedó el separador de miles?" sin reparsear la salida,
     * y lo que un renderer usa para, por ejemplo, poner el signo de moneda en otra tipografía.
     */
    public static class Field extends java.text.Format.Field {

        // Mismo registro por nombre que Attribute, y por el mismo motivo: sólo se puebla con
        // instancias de ESTA clase exacta, para que una subclase no le pise las constantes.
        private static final Map<String, java.text.NumberFormat.Field> INSTANCIAS =
                new HashMap<String, java.text.NumberFormat.Field>();

        protected Field(String name) {
            super(name);
            if (this.getClass() == java.text.NumberFormat.Field.class) {
                INSTANCIAS.put(name, this);
            }
        }

        protected Object readResolve() throws InvalidObjectException {
            if (this.getClass() != java.text.NumberFormat.Field.class) {
                throw new InvalidObjectException("subclass didn't correctly implement readResolve");
            }
            java.text.NumberFormat.Field f = INSTANCIAS.get(this.getName());
            if (f != null) {
                return f;
            }
            throw new InvalidObjectException("unknown attribute name");
        }

        public static final java.text.NumberFormat.Field INTEGER = new java.text.NumberFormat.Field("integer");
        public static final java.text.NumberFormat.Field FRACTION = new java.text.NumberFormat.Field("fraction");
        public static final java.text.NumberFormat.Field EXPONENT = new java.text.NumberFormat.Field("exponent");
        public static final java.text.NumberFormat.Field DECIMAL_SEPARATOR =
                new java.text.NumberFormat.Field("decimal separator");
        public static final java.text.NumberFormat.Field SIGN = new java.text.NumberFormat.Field("sign");
        public static final java.text.NumberFormat.Field GROUPING_SEPARATOR =
                new java.text.NumberFormat.Field("grouping separator");
        public static final java.text.NumberFormat.Field EXPONENT_SYMBOL =
                new java.text.NumberFormat.Field("exponent symbol");
        public static final java.text.NumberFormat.Field PERCENT = new java.text.NumberFormat.Field("percent");
        public static final java.text.NumberFormat.Field PERMILLE = new java.text.NumberFormat.Field("per mille");
        public static final java.text.NumberFormat.Field CURRENCY = new java.text.NumberFormat.Field("currency");
        public static final java.text.NumberFormat.Field EXPONENT_SIGN = new java.text.NumberFormat.Field("exponent sign");
        public static final java.text.NumberFormat.Field PREFIX = new java.text.NumberFormat.Field("prefix");
        public static final java.text.NumberFormat.Field SUFFIX = new java.text.NumberFormat.Field("suffix");
    }

    /**
     * Qué tan largo escribe sus sufijos un formateador compacto: {@code 1K} contra {@code 1 mil}.
     */
    public static enum Style {
        SHORT,
        LONG
    }

    private boolean groupingUsed;
    private boolean parseIntegerOnly;
    private int maximumIntegerDigits;
    private int minimumIntegerDigits;
    private int maximumFractionDigits;
    private int minimumFractionDigits;

    protected NumberFormat() {
        this.groupingUsed = true;
        this.parseIntegerOnly = false;
        this.maximumIntegerDigits = 40;
        this.minimumIntegerDigits = 1;
        this.maximumFractionDigits = 3;
        this.minimumFractionDigits = 0;
    }

    // The Object entry point. Integral wrappers go through the long seam so their exact value
    // survives; everything else through the double one.
    public StringBuffer format(Object number, StringBuffer toAppendTo, FieldPosition pos) {
        if (number instanceof Long || number instanceof Integer
                || number instanceof Short || number instanceof Byte) {
            Number n = (Number) number;
            return this.format(n.longValue(), toAppendTo, pos);
        }
        if (number instanceof Number) {
            Number n = (Number) number;
            return this.format(n.doubleValue(), toAppendTo, pos);
        }
        throw new IllegalArgumentException("Cannot format given Object as a Number");
    }

    public final String format(double number) {
        return this.format(number, new StringBuffer(), new FieldPosition(0)).toString();
    }

    public final String format(long number) {
        return this.format(number, new StringBuffer(), new FieldPosition(0)).toString();
    }

    // The two seams a concrete formatter fills in.
    public abstract StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos);

    public abstract StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos);

    public abstract Number parse(String source, ParsePosition parsePosition);

    /**
     * Parsea desde el principio y falla con excepción.
     *
     * <p>Como en {@link Format#parseObject(String)}, el fracaso se detecta por el cursor sin
     * avanzar y no por un null.
     */
    public Number parse(String source) throws ParseException {
        ParsePosition pos = new ParsePosition(0);
        Number result = this.parse(source, pos);
        if (pos.getIndex() == 0) {
            throw new ParseException("Unparseable number: \"" + source + "\"", pos.getErrorIndex());
        }
        return result;
    }

    // Final: un formateador numérico parsea números, y dejar que una subclase devuelva otra cosa
    // por esta puerta rompería la equivalencia con parse().
    public final Object parseObject(String source, ParsePosition pos) {
        return this.parse(source, pos);
    }

    public boolean isParseIntegerOnly() {
        return this.parseIntegerOnly;
    }

    public void setParseIntegerOnly(boolean value) {
        this.parseIntegerOnly = value;
    }

    public boolean isGroupingUsed() {
        return this.groupingUsed;
    }

    public void setGroupingUsed(boolean newValue) {
        this.groupingUsed = newValue;
    }

    public int getMaximumIntegerDigits() {
        return this.maximumIntegerDigits;
    }

    // Los cuatro setters se pisan entre sí a propósito: un máximo por debajo del mínimo no es un
    // estado representable, así que el que se acaba de fijar gana y el otro lo sigue. Rechazarlo con
    // una excepción obligaría al llamador a conocer el orden en que hay que llamarlos.
    public void setMaximumIntegerDigits(int newValue) {
        this.maximumIntegerDigits = Math.max(0, newValue);
        if (this.minimumIntegerDigits > this.maximumIntegerDigits) {
            this.minimumIntegerDigits = this.maximumIntegerDigits;
        }
    }

    public int getMinimumIntegerDigits() {
        return this.minimumIntegerDigits;
    }

    public void setMinimumIntegerDigits(int newValue) {
        this.minimumIntegerDigits = Math.max(0, newValue);
        if (this.minimumIntegerDigits > this.maximumIntegerDigits) {
            this.maximumIntegerDigits = this.minimumIntegerDigits;
        }
    }

    public int getMaximumFractionDigits() {
        return this.maximumFractionDigits;
    }

    public void setMaximumFractionDigits(int newValue) {
        this.maximumFractionDigits = Math.max(0, newValue);
        if (this.minimumFractionDigits > this.maximumFractionDigits) {
            this.minimumFractionDigits = this.maximumFractionDigits;
        }
    }

    public int getMinimumFractionDigits() {
        return this.minimumFractionDigits;
    }

    public void setMinimumFractionDigits(int newValue) {
        this.minimumFractionDigits = Math.max(0, newValue);
        if (this.minimumFractionDigits > this.maximumFractionDigits) {
            this.maximumFractionDigits = this.minimumFractionDigits;
        }
    }

    /**
     * @throws UnsupportedOperationException siempre, salvo que la subclase lo redefina
     * @implSpec La base NO tiene moneda: no sabe qué símbolo usaría ni dónde lo pondría. Lanzar es
     *           el comportamiento que define el contrato para ese caso — devolver {@code null}
     *           haría pasar por "sin moneda" a lo que en realidad es "esta clase no sabe".
     */
    public Currency getCurrency() {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws UnsupportedOperationException siempre, salvo que la subclase lo redefina
     */
    public void setCurrency(Currency currency) {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws UnsupportedOperationException siempre, salvo que la subclase lo redefina
     */
    public RoundingMode getRoundingMode() {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws UnsupportedOperationException siempre, salvo que la subclase lo redefina
     */
    public void setRoundingMode(RoundingMode roundingMode) {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws UnsupportedOperationException siempre, salvo que la subclase lo redefina
     */
    public boolean isStrict() {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws UnsupportedOperationException siempre, salvo que la subclase lo redefina
     */
    public void setStrict(boolean strict) {
        throw new UnsupportedOperationException();
    }

    // ---- fábricas por locale ----
    //
    // Todas terminan en un DecimalFormat armado con dos cosas separadas: el PATRÓN, que dice el
    // orden y sale de PatronesLocales, y los SÍMBOLOS, que dicen los caracteres y salen de
    // DecimalFormatSymbols. Esa separación es la que hace que no haga falta una clase por locale.

    public static final NumberFormat getInstance() {
        return NumberFormat.getNumberInstance(Locale.getDefault());
    }

    public static NumberFormat getInstance(Locale inLocale) {
        return NumberFormat.getNumberInstance(inLocale);
    }

    public static final NumberFormat getNumberInstance() {
        return NumberFormat.getNumberInstance(Locale.getDefault());
    }

    public static NumberFormat getNumberInstance(Locale inLocale) {
        return new DecimalFormat(PatronesLocales.numero(inLocale),
                new DecimalFormatSymbols(inLocale));
    }

    public static final NumberFormat getIntegerInstance() {
        return NumberFormat.getIntegerInstance(Locale.getDefault());
    }

    // parseIntegerOnly va en true, que es lo que distingue a esta fábrica de un getNumberInstance
    // con cero decimales: además de no imprimirlos, al parsear se detiene en el separador decimal.
    public static NumberFormat getIntegerInstance(Locale inLocale) {
        DecimalFormat f = new DecimalFormat(PatronesLocales.entero(inLocale),
                new DecimalFormatSymbols(inLocale));
        f.setParseIntegerOnly(true);
        return f;
    }

    public static final NumberFormat getCurrencyInstance() {
        return NumberFormat.getCurrencyInstance(Locale.getDefault());
    }

    public static NumberFormat getCurrencyInstance(Locale inLocale) {
        return new DecimalFormat(PatronesLocales.moneda(inLocale),
                new DecimalFormatSymbols(inLocale));
    }

    public static final NumberFormat getPercentInstance() {
        return NumberFormat.getPercentInstance(Locale.getDefault());
    }

    // Sin parseIntegerOnly, a diferencia del entero: el patrón de porcentaje no tiene decimales,
    // pero al PARSEAR "12.5%" el 12.5 es un valor legítimo y cortarlo en el punto lo perdería.
    public static NumberFormat getPercentInstance(Locale inLocale) {
        return new DecimalFormat(PatronesLocales.porciento(inLocale),
                new DecimalFormatSymbols(inLocale));
    }

    /**
     * Los locales para los que hay datos de verdad.
     *
     * <p>Son seis, no cientos, y la lista dice la verdad sobre eso: un locale que no esté acá
     * igual funciona, pero cae en ROOT. Devolver una lista larga fingiendo cobertura sería
     * exactamente la clase de mentira que este paquete evita.
     */
    public static Locale[] getAvailableLocales() {
        return DecimalFormatSymbols.getAvailableLocales();
    }

    public int hashCode() {
        int h = this.maximumIntegerDigits * 37 + this.maxFractionSeed();
        if (this.groupingUsed) {
            h = h + 1;
        }
        return h;
    }

    private int maxFractionSeed() {
        return this.maximumFractionDigits;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }
        NumberFormat other = (NumberFormat) obj;
        return this.maximumIntegerDigits == other.maximumIntegerDigits
                && this.minimumIntegerDigits == other.minimumIntegerDigits
                && this.maximumFractionDigits == other.maximumFractionDigits
                && this.minimumFractionDigits == other.minimumFractionDigits
                && this.groupingUsed == other.groupingUsed
                && this.parseIntegerOnly == other.parseIntegerOnly;
    }

    // ---- los formateadores compactos ---------------------------------------------------------

    /**
     * El formateador compacto del locale por omisión, en estilo corto.
     *
     * <p>Es {@code getCompactNumberInstance(Locale.getDefault(FORMAT), Style.SHORT)}, que es lo
     * que el contrato define. La categoría es {@code FORMAT} y no el default a secas: en una máquina
     * donde el locale de presentación y el de formato difieren son dos respuestas distintas, y la
     * que este método promete es la de formato. Se vio contra el JDK 25.
     */
    public static NumberFormat getCompactNumberInstance() {
        return NumberFormat.getCompactNumberInstance(Locale.getDefault(Locale.Category.FORMAT),
                                                     NumberFormat.Style.SHORT);
    }

    /**
     * El formateador compacto de ese locale y estilo.
     *
     * <p>Un locale sin datos propios cae en ROOT. Ver la nota de la cabecera sobre de dónde salen
     * los patrones.
     *
     * @throws NullPointerException si alguno de los dos es null
     */
    public static NumberFormat getCompactNumberInstance(Locale locale,
                                                        NumberFormat.Style formatStyle) {
        if (locale == null || formatStyle == null) {
            throw new NullPointerException();
        }
        int i = DecimalFormatSymbols.indexOf(locale);
        String[] fila = formatStyle == NumberFormat.Style.SHORT
                ? NumberFormat.compactosCortos()[i]
                : NumberFormat.compactosLargos()[i];
        String[] copia = new String[fila.length];
        for (int k = 0; k < fila.length; k = k + 1) {
            copia[k] = fila[k];
        }
        return new CompactNumberFormat(NumberFormat.PATRON_DECIMAL,
                                       new DecimalFormatSymbols(locale), copia,
                                       NumberFormat.reglasDePlural()[i]);
    }

    // El patrón decimal de base es el mismo en los seis locales; lo que cambia entre ellos son los
    // símbolos, y esos los trae `DecimalFormatSymbols`.
    private static final String PATRON_DECIMAL = "#,##0.###";

    // Las reglas de plural del locale, en la sintaxis del CLDR. Vacías donde el idioma no distingue
    // --japonés no tiene plural gramatical-- y ahí `CompactNumberFormat` usa `other`, que es la
    // categoría que el CLDR garantiza en todos.
    private static String[] reglasDePlural() {
        return new String[] {
            "",
            "one:i = 1 and v = 0",
            "one:n = 1;many:e = 0 and i != 0 and i % 1000000 = 0 and v = 0 or e != 0..5",
            "one:i = 1 and v = 0",
            "one:i = 0,1;many:e = 0 and i != 0 and i % 1000000 = 0 and v = 0 or e != 0..5",
            "",
        };
    }

    // Una fila por locale, en el mismo orden que la tabla de `DecimalFormatSymbols`: und, en-US,
    // es-AR, de-DE, fr-FR, ja-JP. Cada entrada es la potencia de diez de su índice, y las primeras
    // tres están vacías porque abajo de mil no se compacta nada.
    //
    // La fila japonesa es más larga que las demás: su sistema de miradas agrupa de a diez mil
    // (万, 億, 兆, 京) y el CLDR le da entradas hasta 10^18. No es un descuido de las otras.
    private static String[][] compactosCortos() {
        return new String[][] {
            {"", "", "", "{other:0K}", "{other:00K}", "{other:000K}", "{other:0M}", "{other:00M}",
             "{other:000M}", "{other:0G}", "{other:00G}", "{other:000G}", "{other:0T}",
             "{other:00T}", "{other:000T}"},
            {"", "", "", "{one:0K other:0K}", "{one:00K other:00K}", "{one:000K other:000K}",
             "{one:0M other:0M}", "{one:00M other:00M}", "{one:000M other:000M}",
             "{one:0B other:0B}", "{one:00B other:00B}", "{one:000B other:000B}",
             "{one:0T other:0T}", "{one:00T other:00T}", "{one:000T other:000T}"},
            {"", "", "", "{one:0\u00a0K other:0\u00a0K}", "{one:00\u00a0k other:00\u00a0k}",
             "{one:000\u00a0k other:000\u00a0k}", "{one:0\u00a0M other:0\u00a0M}",
             "{one:00\u00a0M other:00\u00a0M}", "{one:000\u00a0M other:000\u00a0M}",
             "{one:0000\u00a0M other:0000\u00a0M}",
             "{one:00\u00a0mil\u00a0M other:00\u00a0mil\u00a0M}",
             "{one:000\u00a0mil\u00a0M other:000\u00a0mil\u00a0M}",
             "{one:0\u00a0B other:0\u00a0B}", "{one:00\u00a0B other:00\u00a0B}",
             "{one:000\u00a0B other:000\u00a0B}"},
            {"", "", "", "{one:0 other:0}", "{one:0 other:0}", "{one:0 other:0}",
             "{one:0\u00a0Mio'.' other:0\u00a0Mio'.'}",
             "{one:00\u00a0Mio'.' other:00\u00a0Mio'.'}",
             "{one:000\u00a0Mio'.' other:000\u00a0Mio'.'}",
             "{one:0\u00a0Mrd'.' other:0\u00a0Mrd'.'}",
             "{one:00\u00a0Mrd'.' other:00\u00a0Mrd'.'}",
             "{one:000\u00a0Mrd'.' other:000\u00a0Mrd'.'}",
             "{one:0\u00a0Bio'.' other:0\u00a0Bio'.'}",
             "{one:00\u00a0Bio'.' other:00\u00a0Bio'.'}",
             "{one:000\u00a0Bio'.' other:000\u00a0Bio'.'}"},
            {"", "", "", "{one:0\u00a0k other:0\u00a0k}", "{one:00\u00a0k other:00\u00a0k}",
             "{one:000\u00a0k other:000\u00a0k}", "{one:0\u00a0M other:0\u00a0M}",
             "{one:00\u00a0M other:00\u00a0M}", "{one:000\u00a0M other:000\u00a0M}",
             "{one:0\u00a0Md other:0\u00a0Md}", "{one:00\u00a0Md other:00\u00a0Md}",
             "{one:000\u00a0Md other:000\u00a0Md}", "{one:0\u00a0Bn other:0\u00a0Bn}",
             "{one:00\u00a0Bn other:00\u00a0Bn}", "{one:000\u00a0Bn other:000\u00a0Bn}"},
            {"", "", "", "{other:0}", "{other:0\u4e07}", "{other:00\u4e07}", "{other:000\u4e07}",
             "{other:0000\u4e07}", "{other:0\u5104}", "{other:00\u5104}", "{other:000\u5104}",
             "{other:0000\u5104}", "{other:0\u5146}", "{other:00\u5146}", "{other:000\u5146}",
             "{other:0000\u5146}", "{other:0\u4eac}", "{other:00\u4eac}", "{other:000\u4eac}",
             "{other:0000\u4eac}"},
        };
    }

    private static String[][] compactosLargos() {
        return new String[][] {
            {"", "", "", "{other:0K}", "{other:00K}", "{other:000K}", "{other:0M}", "{other:00M}",
             "{other:000M}", "{other:0G}", "{other:00G}", "{other:000G}", "{other:0T}",
             "{other:00T}", "{other:000T}"},
            {"", "", "", "{one:0' 'thousand other:0' 'thousand}",
             "{one:00' 'thousand other:00' 'thousand}", "{one:000' 'thousand other:000' 'thousand}",
             "{one:0' 'million other:0' 'million}", "{one:00' 'million other:00' 'million}",
             "{one:000' 'million other:000' 'million}", "{one:0' 'billion other:0' 'billion}",
             "{one:00' 'billion other:00' 'billion}", "{one:000' 'billion other:000' 'billion}",
             "{one:0' 'trillion other:0' 'trillion}", "{one:00' 'trillion other:00' 'trillion}",
             "{one:000' 'trillion other:000' 'trillion}"},
            {"", "", "", "{one:0\u00a0K other:0\u00a0K}", "{one:00\u00a0k other:00\u00a0k}",
             "{one:000\u00a0k other:000\u00a0k}", "{one:0\u00a0M other:0\u00a0M}",
             "{one:00\u00a0M other:00\u00a0M}", "{one:000\u00a0M other:000\u00a0M}",
             "{one:0000\u00a0M other:0000\u00a0M}",
             "{one:00\u00a0mil\u00a0M other:00\u00a0mil\u00a0M}",
             "{one:000\u00a0mil\u00a0M other:000\u00a0mil\u00a0M}",
             "{one:0\u00a0B other:0\u00a0B}", "{one:00\u00a0B other:00\u00a0B}",
             "{one:000\u00a0B other:000\u00a0B}"},
            {"", "", "", "{one:0' 'Tausend other:0' 'Tausend}",
             "{one:00' 'Tausend other:00' 'Tausend}", "{one:000' 'Tausend other:000' 'Tausend}",
             "{one:0' 'Million other:0' 'Millionen}", "{one:00' 'Millionen other:00' 'Millionen}",
             "{one:000' 'Millionen other:000' 'Millionen}",
             "{one:0' 'Milliarde other:0' 'Milliarden}",
             "{one:00' 'Milliarden other:00' 'Milliarden}",
             "{one:000' 'Milliarden other:000' 'Milliarden}",
             "{one:0' 'Billion other:0' 'Billionen}", "{one:00' 'Billionen other:00' 'Billionen}",
             "{one:000' 'Billionen other:000' 'Billionen}"},
            {"", "", "", "{1:mille one:0' 'millier other:0' 'mille}",
             "{one:00' 'mille other:00' 'mille}", "{one:000' 'mille other:000' 'mille}",
             "{one:0' 'million other:0' 'millions}", "{one:00' 'million other:00' 'millions}",
             "{one:000' 'million other:000' 'millions}", "{one:0' 'milliard other:0' 'milliards}",
             "{one:00' 'milliard other:00' 'milliards}",
             "{one:000' 'milliard other:000' 'milliards}", "{one:0' 'billion other:0' 'billions}",
             "{one:00' 'billion other:00' 'billions}", "{one:000' 'billion other:000' 'billions}"},
            {"", "", "", "{other:0}", "{other:0\u4e07}", "{other:00\u4e07}", "{other:000\u4e07}",
             "{other:0000\u4e07}", "{other:0\u5104}", "{other:00\u5104}", "{other:000\u5104}",
             "{other:0000\u5104}", "{other:0\u5146}", "{other:00\u5146}", "{other:000\u5146}",
             "{other:0000\u5146}", "{other:0\u4eac}", "{other:00\u4eac}", "{other:000\u4eac}",
             "{other:0000\u4eac}"},
        };
    }
}
