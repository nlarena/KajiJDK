package java.time.format;

import java.time.chrono.ChronoLocalDate;
import java.time.temporal.ChronoField;
import java.time.temporal.IsoFields;
import java.time.temporal.JulianFields;
import java.time.temporal.TemporalField;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// El armador de `DateTimeFormatter`: se nombran los pedazos y el arma el formateador.
//
// **Este es el modelo de verdad, y `ofPattern` es azucar sobre el.** La version anterior era al
// reves: el formateador guardaba una cadena de patron y el armador la componia. Eso ponia un techo
// duro --lo que una cadena de patron no puede decir, el armador no podia ofrecer-- y dejaba afuera
// las secciones opcionales, el relleno y los nombres puestos a mano. Ahora `appendPattern` compila la
// cadena a las **mismas piezas** que los `append*` producen, asi que las dos entradas no se pueden
// desincronizar y el techo desaparecio.
//
// Lo que sigue afuera, y es todo lo mismo: **los doce miembros que necesitan datos de texto del
// CLDR**, que esta biblioteca no trae.
//
//   - `appendLocalized(FormatStyle, FormatStyle)` y `appendLocalized(String)` piden el *patron* que
//     un locale usa para una fecha corta o larga. No es un patron que se pueda deducir: `M/d/yy` en
//     los Estados Unidos, `dd/MM/yyyy` en la Argentina, `yyyy/MM/dd` en el Japon. Devolver uno fijo
//     seria dar una respuesta con la forma de la correcta para todos los locales menos uno.
//   - `getLocalizedDateTimePattern` en sus dos formas es exactamente esa consulta, expuesta.
//   - `appendZoneText` y `appendGenericZoneText` (dos formas cada uno) piden el *nombre* de la zona
//     --"Pacific Standard Time", "hora estandar del Pacifico"--, que es una tabla del CLDR por zona y
//     por idioma. El **id** si esta: `appendZoneId`.
//   - `appendChronologyText` pide el nombre del calendario; `appendChronologyId` --que si esta-- da
//     la clave.
//   - `appendDayPeriodText` pide "in the morning" / "at night", que es dato del CLDR y ademas
//     depende de reglas de corte propias de cada idioma.
//   - `appendLocalizedOffset` escribe `GMT+8` con la palabra "GMT" traducida y el formato de la hora
//     tomado del locale.
//
// La distincion que importa: **no es que falte el dato, es que inventarlo seria mentir**. Un
// `appendZoneText(FULL)` que escriba `Europe/Paris` no esta incompleto, esta mal: dice "este es el
// nombre" y no lo es. `appendText(TemporalField, Map)` --que si esta-- es la salida para el que
// necesite nombres propios: pone los suyos y el resultado es cierto por construccion.
public final class DateTimeFormatterBuilder {

    private final DateTimeFormatterBuilder padre;
    private final boolean opcional;
    private final List<Pieza> piezas;
    // La seccion en la que caen los `append`. Es `this` mientras no haya un `optionalStart` abierto;
    // adentro de uno apunta al armador hijo. Vive solo en el armador raiz --que es el que el llamador
    // tiene en la mano-- porque todos los `append` devuelven la raiz.
    private DateTimeFormatterBuilder activo;
    private int anchoRelleno;
    private char relleno;

    public DateTimeFormatterBuilder() {
        this(null, false);
    }

    private DateTimeFormatterBuilder(DateTimeFormatterBuilder padre, boolean opcional) {
        this.padre = padre;
        this.opcional = opcional;
        this.piezas = new ArrayList<Pieza>();
        this.activo = this;
        this.anchoRelleno = 0;
        this.relleno = ' ';
    }

    private DateTimeFormatterBuilder agregar(Pieza pieza) {
        DateTimeFormatterBuilder a = this.activo;
        Pieza p = pieza;
        if (a.anchoRelleno > 0) {
            p = new PiezaRelleno(p, a.anchoRelleno, a.relleno);
            a.anchoRelleno = 0;
        }
        a.piezas.add(p);
        return this;
    }

    // ---------------------------------------------------------------- numeros

    public DateTimeFormatterBuilder appendValue(TemporalField field) {
        if (field == null) {
            throw new NullPointerException("field");
        }
        return this.agregar(new PiezaNumero(field, 1, 19, SignStyle.NORMAL));
    }

    public DateTimeFormatterBuilder appendValue(TemporalField field, int width) {
        if (field == null) {
            throw new NullPointerException("field");
        }
        if (width < 1 || width > 19) {
            throw new IllegalArgumentException("The width must be from 1 to 19 inclusive but was "
                    + width);
        }
        return this.agregar(new PiezaNumero(field, width, width, SignStyle.NOT_NEGATIVE));
    }

    public DateTimeFormatterBuilder appendValue(TemporalField field, int minWidth, int maxWidth,
            SignStyle signStyle) {
        if (field == null) {
            throw new NullPointerException("field");
        }
        if (signStyle == null) {
            throw new NullPointerException("signStyle");
        }
        if (minWidth < 1 || minWidth > 19) {
            throw new IllegalArgumentException("The minimum width must be from 1 to 19 inclusive but"
                    + " was " + minWidth);
        }
        if (maxWidth < 1 || maxWidth > 19) {
            throw new IllegalArgumentException("The maximum width must be from 1 to 19 inclusive but"
                    + " was " + maxWidth);
        }
        if (maxWidth < minWidth) {
            throw new IllegalArgumentException("The maximum width must exceed or equal the minimum"
                    + " width but " + maxWidth + " < " + minWidth);
        }
        return this.agregar(new PiezaNumero(field, minWidth, maxWidth, signStyle));
    }

    public DateTimeFormatterBuilder appendValueReduced(TemporalField field, int width, int maxWidth,
            int baseValue) {
        if (field == null) {
            throw new NullPointerException("field");
        }
        return this.agregar(new PiezaNumeroReducido(field, width, maxWidth, baseValue, null));
    }

    public DateTimeFormatterBuilder appendValueReduced(TemporalField field, int width, int maxWidth,
            ChronoLocalDate baseDate) {
        if (field == null) {
            throw new NullPointerException("field");
        }
        if (baseDate == null) {
            throw new NullPointerException("baseDate");
        }
        return this.agregar(new PiezaNumeroReducido(field, width, maxWidth, 0, baseDate));
    }

    public DateTimeFormatterBuilder appendFraction(TemporalField field, int minWidth, int maxWidth,
            boolean decimalPoint) {
        if (field == null) {
            throw new NullPointerException("field");
        }
        if (minWidth < 0 || minWidth > 9) {
            throw new IllegalArgumentException("The minimum width must be from 0 to 9 inclusive but"
                    + " was " + minWidth);
        }
        if (maxWidth < 1 || maxWidth > 9) {
            throw new IllegalArgumentException("The maximum width must be from 1 to 9 inclusive but"
                    + " was " + maxWidth);
        }
        if (maxWidth < minWidth) {
            throw new IllegalArgumentException("The maximum width must exceed or equal the minimum"
                    + " width but " + maxWidth + " < " + minWidth);
        }
        return this.agregar(new PiezaFraccion(field, minWidth, maxWidth, decimalPoint));
    }

    // ---------------------------------------------------------------- texto

    public DateTimeFormatterBuilder appendText(TemporalField field) {
        return this.appendText(field, TextStyle.FULL);
    }

    // Los nombres son los ingleses --lo unico que esta biblioteca tiene-- y con otro locale esta
    // pieza **tira** en vez de escribirlos igual. El porque esta en `PiezaTexto`.
    public DateTimeFormatterBuilder appendText(TemporalField field, TextStyle textStyle) {
        if (field == null) {
            throw new NullPointerException("field");
        }
        if (textStyle == null) {
            throw new NullPointerException("textStyle");
        }
        return this.agregar(new PiezaTexto(field, textStyle));
    }

    // La forma que **no** depende de ningun dato de locale: los nombres los trae el llamador.
    public DateTimeFormatterBuilder appendText(TemporalField field, Map<Long, String> textLookup) {
        if (field == null) {
            throw new NullPointerException("field");
        }
        if (textLookup == null) {
            throw new NullPointerException("textLookup");
        }
        return this.agregar(new PiezaTextoMapa(field,
                new HashMap<Long, String>(textLookup)));
    }

    // ---------------------------------------------------------------- literales

    public DateTimeFormatterBuilder appendLiteral(char literal) {
        return this.agregar(new PiezaLiteral(String.valueOf(literal)));
    }

    public DateTimeFormatterBuilder appendLiteral(String literal) {
        if (literal == null) {
            throw new NullPointerException("literal");
        }
        if (literal.length() > 0) {
            this.agregar(new PiezaLiteral(literal));
        }
        return this;
    }

    // ---------------------------------------------------------------- zona y calendario

    public DateTimeFormatterBuilder appendOffsetId() {
        return this.appendOffset("+HH:MM:ss", "Z");
    }

    public DateTimeFormatterBuilder appendOffset(String pattern, String noOffsetText) {
        if (pattern == null) {
            throw new NullPointerException("pattern");
        }
        if (noOffsetText == null) {
            throw new NullPointerException("noOffsetText");
        }
        return this.agregar(new PiezaOffset(pattern, noOffsetText));
    }

    public DateTimeFormatterBuilder appendZoneId() {
        return this.agregar(new PiezaZonaId(PiezaZonaId.ZONA));
    }

    public DateTimeFormatterBuilder appendZoneOrOffsetId() {
        return this.agregar(new PiezaZonaId(PiezaZonaId.ZONA_U_OFFSET));
    }

    public DateTimeFormatterBuilder appendZoneRegionId() {
        return this.agregar(new PiezaZonaId(PiezaZonaId.REGION));
    }

    public DateTimeFormatterBuilder appendChronologyId() {
        return this.agregar(new PiezaCronologiaId());
    }

    public DateTimeFormatterBuilder appendInstant() {
        return this.agregar(new PiezaInstante(-2));
    }

    public DateTimeFormatterBuilder appendInstant(int fractionalDigits) {
        if (fractionalDigits < -1 || fractionalDigits > 9) {
            throw new IllegalArgumentException("The fractional digits must be from -1 to 9 inclusive"
                    + " but was " + fractionalDigits);
        }
        return this.agregar(new PiezaInstante(fractionalDigits));
    }

    // ---------------------------------------------------------------- ajustes del parseo

    public DateTimeFormatterBuilder parseCaseSensitive() {
        return this.agregar(new PiezaAjuste(PiezaAjuste.SENSIBLE));
    }

    public DateTimeFormatterBuilder parseCaseInsensitive() {
        return this.agregar(new PiezaAjuste(PiezaAjuste.INSENSIBLE));
    }

    public DateTimeFormatterBuilder parseStrict() {
        return this.agregar(new PiezaAjuste(PiezaAjuste.ESTRICTO));
    }

    public DateTimeFormatterBuilder parseLenient() {
        return this.agregar(new PiezaAjuste(PiezaAjuste.LAXO));
    }

    public DateTimeFormatterBuilder parseDefaulting(TemporalField field, long value) {
        if (field == null) {
            throw new NullPointerException("field");
        }
        return this.agregar(new PiezaPorDefecto(field, value));
    }

    // ---------------------------------------------------------------- relleno

    public DateTimeFormatterBuilder padNext(int padWidth) {
        return this.padNext(padWidth, ' ');
    }

    // Afecta **a la proxima pieza y a ninguna mas**. Es lo que dice el JDK, y es lo razonable: el
    // relleno es del campo, no del formateador.
    public DateTimeFormatterBuilder padNext(int padWidth, char padChar) {
        if (padWidth < 1) {
            throw new IllegalArgumentException("The pad width must be at least one but was "
                    + padWidth);
        }
        this.activo.anchoRelleno = padWidth;
        this.activo.relleno = padChar;
        return this;
    }

    // ---------------------------------------------------------------- secciones opcionales

    public DateTimeFormatterBuilder optionalStart() {
        this.activo = new DateTimeFormatterBuilder(this.activo, true);
        return this;
    }

    public DateTimeFormatterBuilder optionalEnd() {
        if (this.activo.padre == null) {
            throw new IllegalStateException("Cannot call optionalEnd() as there was no previous call"
                    + " to optionalStart()");
        }
        DateTimeFormatterBuilder cerrada = this.activo;
        this.activo = cerrada.padre;
        if (cerrada.piezas.size() > 0) {
            this.agregar(cerrada.compuesta());
        }
        return this;
    }

    // ---------------------------------------------------------------- composicion

    public DateTimeFormatterBuilder append(DateTimeFormatter formatter) {
        if (formatter == null) {
            throw new NullPointerException("formatter");
        }
        return this.agregar(formatter.piezas());
    }

    public DateTimeFormatterBuilder appendOptional(DateTimeFormatter formatter) {
        if (formatter == null) {
            throw new NullPointerException("formatter");
        }
        return this.agregar(new PiezaCompuesta(new Pieza[] {formatter.piezas()}, true));
    }

    private PiezaCompuesta compuesta() {
        Pieza[] a = new Pieza[this.piezas.size()];
        int i = 0;
        while (i < a.length) {
            a[i] = this.piezas.get(i);
            i = i + 1;
        }
        return new PiezaCompuesta(a, this.opcional);
    }

    public DateTimeFormatter toFormatter() {
        // **Divergencia deliberada**: el JDK usa el locale por defecto de la maquina. Aca es
        // `Locale.ROOT`, porque el unico juego de nombres que hay es el de la raiz, y tomar el locale
        // de la maquina haria que `ofPattern("dd MMM yyyy")` tirara en cualquier equipo que no este
        // en ingles --por una limitacion de datos que no tiene nada que ver con lo que el llamador
        // pidio--. `toFormatter(Locale)` deja elegir explicitamente.
        return this.toFormatter(Locale.ROOT);
    }

    public DateTimeFormatter toFormatter(Locale locale) {
        if (locale == null) {
            throw new NullPointerException("locale");
        }
        while (this.activo.padre != null) {
            this.optionalEnd();
        }
        return new DateTimeFormatter(this.compuesta(), locale, DecimalStyle.STANDARD,
                ResolverStyle.SMART, null, null, null);
    }

    // ---------------------------------------------------------------- el patron

    public DateTimeFormatterBuilder appendPattern(String pattern) {
        if (pattern == null) {
            throw new NullPointerException("pattern");
        }
        this.compilar(pattern);
        return this;
    }

    // El compilador del lenguaje de patrones. Cada letra se traduce a las mismas piezas que el
    // `append` correspondiente produce, y **las letras que necesitarian CLDR se rechazan con el
    // motivo**: es preferible un error en el sitio donde se escribio el patron a un formateador que
    // escribe en el idioma equivocado.
    private void compilar(String p) {
        int i = 0;
        while (i < p.length()) {
            char c = p.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                int inicio = i;
                while (i < p.length() && p.charAt(i) == c) {
                    i = i + 1;
                }
                this.letra(c, i - inicio, p);
            } else if (c == '\'') {
                i = i + 1;
                StringBuilder lit = new StringBuilder();
                boolean cerrada = false;
                while (i < p.length()) {
                    if (p.charAt(i) == '\'') {
                        if (i + 1 < p.length() && p.charAt(i + 1) == '\'') {
                            lit.append('\'');
                            i = i + 2;
                        } else {
                            i = i + 1;
                            cerrada = true;
                            break;
                        }
                    } else {
                        lit.append(p.charAt(i));
                        i = i + 1;
                    }
                }
                if (!cerrada) {
                    throw new IllegalArgumentException("Pattern ends with an incomplete string"
                            + " literal: " + p);
                }
                if (lit.length() == 0) {
                    // `''` suelto es una comilla literal.
                    this.appendLiteral('\'');
                } else {
                    this.appendLiteral(lit.toString());
                }
            } else if (c == '[') {
                this.optionalStart();
                i = i + 1;
            } else if (c == ']') {
                this.optionalEnd();
                i = i + 1;
            } else if (c == '#' || c == '{' || c == '}') {
                throw new IllegalArgumentException("Pattern includes reserved character: '" + c
                        + "'");
            } else {
                this.appendLiteral(c);
                i = i + 1;
            }
        }
    }

    private static void noHayCldr(char c, String que) {
        throw new IllegalArgumentException("Pattern letter '" + c + "' needs " + que
                + ", which comes from CLDR locale data that this library does not ship."
                + " Use a numeric field, or appendText(field, Map) with your own names.");
    }

    private void letra(char c, int n, String patron) {
        if (c == 'u' || c == 'y') {
            // `u` es el anio proleptico y `y` el anio de la era. Con dos letras los dos se recortan a
            // los ultimos dos digitos, con la ventana 2000-2099.
            TemporalField campo = c == 'u' ? ChronoField.YEAR : ChronoField.YEAR_OF_ERA;
            if (n == 2) {
                this.appendValueReduced(campo, 2, 2, 2000);
            } else {
                this.appendValue(campo, n, 10, n < 4 ? SignStyle.NORMAL : SignStyle.EXCEEDS_PAD);
            }
        } else if (c == 'M' || c == 'L') {
            if (n <= 2) {
                this.appendValue(ChronoField.MONTH_OF_YEAR, n, 2, SignStyle.NOT_NEGATIVE);
            } else {
                this.appendText(ChronoField.MONTH_OF_YEAR, estilo(n, c == 'L'));
            }
        } else if (c == 'd') {
            this.appendValue(ChronoField.DAY_OF_MONTH, n, 2, SignStyle.NOT_NEGATIVE);
        } else if (c == 'D') {
            this.appendValue(ChronoField.DAY_OF_YEAR, n, 3, SignStyle.NOT_NEGATIVE);
        } else if (c == 'g') {
            this.appendValue(JulianFields.MODIFIED_JULIAN_DAY, n, 19, SignStyle.NORMAL);
        } else if (c == 'E') {
            this.appendText(ChronoField.DAY_OF_WEEK, estilo(n, false));
        } else if (c == 'G') {
            this.appendText(ChronoField.ERA, estilo(n, false));
        } else if (c == 'a') {
            this.appendText(ChronoField.AMPM_OF_DAY, estilo(n, false));
        } else if (c == 'h') {
            this.appendValue(ChronoField.CLOCK_HOUR_OF_AMPM, n, 2, SignStyle.NOT_NEGATIVE);
        } else if (c == 'K') {
            this.appendValue(ChronoField.HOUR_OF_AMPM, n, 2, SignStyle.NOT_NEGATIVE);
        } else if (c == 'k') {
            this.appendValue(ChronoField.CLOCK_HOUR_OF_DAY, n, 2, SignStyle.NOT_NEGATIVE);
        } else if (c == 'H') {
            this.appendValue(ChronoField.HOUR_OF_DAY, n, 2, SignStyle.NOT_NEGATIVE);
        } else if (c == 'm') {
            this.appendValue(ChronoField.MINUTE_OF_HOUR, n, 2, SignStyle.NOT_NEGATIVE);
        } else if (c == 's') {
            this.appendValue(ChronoField.SECOND_OF_MINUTE, n, 2, SignStyle.NOT_NEGATIVE);
        } else if (c == 'S') {
            // `S` es la fraccion, no el numero: `.5` es medio segundo y no cinco nanos.
            this.appendFraction(ChronoField.NANO_OF_SECOND, n, n, false);
        } else if (c == 'A') {
            this.appendValue(ChronoField.MILLI_OF_DAY, n, 19, SignStyle.NOT_NEGATIVE);
        } else if (c == 'n') {
            this.appendValue(ChronoField.NANO_OF_SECOND, n, 19, SignStyle.NOT_NEGATIVE);
        } else if (c == 'N') {
            this.appendValue(ChronoField.NANO_OF_DAY, n, 19, SignStyle.NOT_NEGATIVE);
        } else if (c == 'Q') {
            if (n <= 2) {
                this.appendValue(IsoFields.QUARTER_OF_YEAR, n, 2, SignStyle.NOT_NEGATIVE);
            } else {
                noHayCldr(c, "quarter names");
            }
        } else if (c == 'V') {
            if (n != 2) {
                throw new IllegalArgumentException("Pattern letter count must be 2 for 'V': "
                        + patron);
            }
            this.appendZoneId();
        } else if (c == 'v') {
            noHayCldr(c, "generic zone names");
        } else if (c == 'z') {
            noHayCldr(c, "zone names");
        } else if (c == 'O') {
            noHayCldr(c, "localized offset text (\"GMT+8\")");
        } else if (c == 'B') {
            noHayCldr(c, "day period text (\"in the morning\")");
        } else if (c == 'w' || c == 'W' || c == 'e' || c == 'c') {
            // La semana del anio depende de que dia empieza la semana y de cuantos dias tiene la
            // primera, y las dos cosas cambian por region --dato del CLDR--. `IsoFields` da la
            // version ISO, que si es fija: se llega por `appendValue(IsoFields.WEEK_OF_WEEK_BASED_YEAR, ...)`.
            noHayCldr(c, "locale week rules (first day of week, minimal days in first week)");
        } else if (c == 'p') {
            // El relleno se aplica a lo que venga despues, que es como el JDK lo define.
            this.padNext(n);
        } else if (c == 'x' || c == 'X' || c == 'Z') {
            this.offsetDePatron(c, n, patron);
        } else {
            throw new IllegalArgumentException("Unknown pattern letter: " + c);
        }
    }

    private static TextStyle estilo(int n, boolean solo) {
        TextStyle t;
        if (n == 5) {
            t = TextStyle.NARROW;
        } else if (n >= 4) {
            t = TextStyle.FULL;
        } else {
            t = TextStyle.SHORT;
        }
        return solo ? t.asStandalone() : t;
    }

    private void offsetDePatron(char c, int n, String patron) {
        String[] formas = {"+HHmm", "+HHMM", "+HH:MM", "+HHMMss", "+HH:MM:ss"};
        if (c == 'Z') {
            if (n <= 3) {
                this.appendOffset("+HHMM", "+0000");
            } else if (n == 4) {
                noHayCldr(c, "localized offset text (\"GMT+8\")");
            } else if (n == 5) {
                this.appendOffset("+HH:MM:ss", "Z");
            } else {
                throw new IllegalArgumentException("Too many pattern letters: " + c);
            }
            return;
        }
        if (n < 1 || n > 5) {
            throw new IllegalArgumentException("Too many pattern letters: " + c);
        }
        // `X` escribe `Z` para el cero; `x` lo escribe con numeros. Es la unica diferencia entre las
        // dos letras, y por eso comparten la tabla de formas.
        this.appendOffset(formas[n - 1], c == 'X' ? "Z" : "");
    }
}
