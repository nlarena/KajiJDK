package java.time.format;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQuery;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.time.temporal.ValueRange;
import java.util.HashMap;
import java.util.Map;

// KajiLibrary's java.time.format.DateTimeFormatter — a pattern-based formatter for java.time value
// types. Built from a pattern string via ofPattern (e.g. "yyyy-MM-dd HH:mm:ss") and applied with
// format(TemporalAccessor), reading fields through TemporalAccessor.getLong(ChronoField).
//
// Supported pattern letters (count = run length): u/y year (yy → 2-digit, else zero-padded to count),
// M month (1-2 numeric, 3 short name, 4+ full name), d day-of-month, D day-of-year, H hour-of-day,
// h clock-hour 1-12, m minute, s second, S fraction-of-second (first `count` nano digits), a AM/PM,
// E day-of-week (1-3 short name, 4+ full name), X/x/Z offset, V zone id. Text in single quotes is
// literal ('' → a literal quote); non-letter characters pass through. Names are English (Locale-aware
// output is out of scope).
//
// **Formatea y parsea exactamente el mismo lenguaje**, y esa simetria es a proposito: lo que este
// formateador escribe, lo vuelve a leer. Un patron con una letra que no soporta falla igual en los
// dos sentidos, con el mismo mensaje, en vez de escribir algo que despues no se puede releer.
//
// Lo que **no** hay es el resto de `java.time.format`: los formateadores predefinidos (`ISO_*`), los
// localizados, `DecimalStyle`, `ResolverStyle` configurable, `parseBest`, `toFormat`. Se resuelve con
// `ResolverStyle.SMART`, que es el del JDK.
public final class DateTimeFormatter {

    private static final String[] MONTHS = {"January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"};
    private static final String[] MON3 = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
    private static final String[] DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday",
        "Saturday", "Sunday"};
    private static final String[] DAY3 = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

    private final String pattern;

    private DateTimeFormatter(String pattern) {
        this.pattern = pattern;
    }

    public static DateTimeFormatter ofPattern(String pattern) {
        return new DateTimeFormatter(pattern);
    }

    // Package-private seam for DateTimeFormatterBuilder.append(DateTimeFormatter), which composes
    // by concatenating patterns. Not public: the JDK has no such accessor, so it would be an EXTRA.
    String pattern() {
        return this.pattern;
    }

    public String format(TemporalAccessor temporal) {
        StringBuilder out = new StringBuilder();
        String p = this.pattern;
        int i = 0;
        while (i < p.length()) {
            char c = p.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                int start = i;
                while (i < p.length() && p.charAt(i) == c) {
                    i = i + 1;
                }
                out.append(field(c, i - start, temporal));
            } else if (c == '\'') {
                i = i + 1;
                if (i < p.length() && p.charAt(i) == '\'') {
                    out.append('\'');
                    i = i + 1;
                } else {
                    while (i < p.length() && p.charAt(i) != '\'') {
                        out.append(p.charAt(i));
                        i = i + 1;
                    }
                    i = i + 1;
                }
            } else {
                out.append(c);
                i = i + 1;
            }
        }
        return out.toString();
    }

    /**
     * Lee `text` con este patron y devuelve los campos que encontro.
     *
     * <p>Lo que vuelve **no es una fecha**: es el conjunto de campos que el texto traia, ya
     * resueltos --anio+mes+dia se convierten en el dia epoch, hora+minuto en el nano del dia-- pero
     * sin decidir en que clase entran. Esa decision es del que llama, y por eso existe la otra
     * version: `parse(texto, LocalDate::from)`.
     *
     * @throws java.time.format.DateTimeParseException si el texto no encaja con el patron
     */
    public TemporalAccessor parse(CharSequence text) {
        return this.parseTodo(text);
    }

    /**
     * Lee `text` con este patron y arma con el lo que `query` pida.
     *
     * <p>Es la forma que usan los `parse(texto, formateador)` de `LocalDate`, `LocalTime` y las
     * demas: cada una pasa su propio `from`.
     *
     * @throws java.time.format.DateTimeParseException si el texto no encaja con el patron, o si lo
     *     que encaja no alcanza para lo que `query` pide
     */
    public <T> T parse(CharSequence text, TemporalQuery<T> query) {
        if (query == null) {
            throw new NullPointerException("query");
        }
        Parsed parsed = this.parseTodo(text);
        try {
            return query.queryFrom(parsed);
        } catch (java.time.DateTimeException e) {
            // El texto encajo pero no traia lo que hacia falta --un patron de solo hora al que le
            // piden una fecha--. Se reetiqueta como error de parseo porque desde afuera es lo mismo:
            // el texto no dio lo que se pedia. El mensaje original va adentro.
            throw new DateTimeParseException(
                    "Text '" + text + "' could not be parsed: " + e.getMessage(), text, 0);
        }
    }

    private Parsed parseTodo(CharSequence text) {
        if (text == null) {
            throw new NullPointerException("text");
        }
        String t = text.toString();
        Map<TemporalField, Long> campos = new HashMap<TemporalField, Long>();
        Long ampm = null;
        Long clockHour = null;
        ZoneOffset offset = null;
        ZoneId zona = null;

        String p = this.pattern;
        int i = 0;
        int j = 0;
        while (i < p.length()) {
            char c = p.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                int inicio = i;
                while (i < p.length() && p.charAt(i) == c) {
                    i = i + 1;
                }
                int count = i - inicio;
                if (c == 'a') {
                    j = this.esperarTexto(t, j, "AM", "PM", text);
                    ampm = Long.valueOf(t.regionMatches(true, j - 2, "AM", 0, 2) ? 0L : 1L);
                } else if (c == 'E') {
                    int dow = this.leerNombre(t, j, count <= 3 ? DAY3 : DAYS, text);
                    j = j + (count <= 3 ? 3 : DAYS[dow].length());
                    campos.put(ChronoField.DAY_OF_WEEK, Long.valueOf((long) (dow + 1)));
                } else if (c == 'M' && count >= 3) {
                    int mes = this.leerNombre(t, j, count == 3 ? MON3 : MONTHS, text);
                    j = j + (count == 3 ? 3 : MONTHS[mes].length());
                    campos.put(ChronoField.MONTH_OF_YEAR, Long.valueOf((long) (mes + 1)));
                } else if (c == 'X' || c == 'x' || c == 'Z') {
                    int[] fin = new int[1];
                    offset = this.leerOffset(t, j, c, text, fin);
                    j = fin[0];
                } else if (c == 'V') {
                    int fin = j;
                    while (fin < t.length() && !this.esSeparador(t.charAt(fin))) {
                        fin = fin + 1;
                    }
                    zona = ZoneId.of(t.substring(j, fin));
                    j = fin;
                } else {
                    int[] fin = new int[1];
                    long v = this.leerNumero(t, j, c, count, text, fin);
                    j = fin[0];
                    if (c == 'y' || c == 'u') {
                        // `yy` son los dos ultimos digitos: se completan al siglo XXI, que es la
                        // ventana que usa el JDK (2000-2099) para un patron de dos.
                        if (count == 2) {
                            v = 2000L + v;
                        }
                        campos.put(ChronoField.YEAR, Long.valueOf(v));
                    } else if (c == 'M') {
                        campos.put(ChronoField.MONTH_OF_YEAR, Long.valueOf(v));
                    } else if (c == 'd') {
                        campos.put(ChronoField.DAY_OF_MONTH, Long.valueOf(v));
                    } else if (c == 'D') {
                        campos.put(ChronoField.DAY_OF_YEAR, Long.valueOf(v));
                    } else if (c == 'H') {
                        campos.put(ChronoField.HOUR_OF_DAY, Long.valueOf(v));
                    } else if (c == 'h') {
                        clockHour = Long.valueOf(v);
                    } else if (c == 'm') {
                        campos.put(ChronoField.MINUTE_OF_HOUR, Long.valueOf(v));
                    } else if (c == 's') {
                        campos.put(ChronoField.SECOND_OF_MINUTE, Long.valueOf(v));
                    } else if (c == 'S') {
                        // `S` son los primeros `count` digitos del nano: hay que devolverlos a su
                        // escala. `.5` es medio segundo, no cinco nanos.
                        long escala = 1L;
                        int k = count;
                        while (k < 9) {
                            escala = escala * 10L;
                            k = k + 1;
                        }
                        campos.put(ChronoField.NANO_OF_SECOND, Long.valueOf(v * escala));
                    } else {
                        throw new IllegalArgumentException("Unsupported pattern letter: " + c);
                    }
                }
            } else if (c == '\'') {
                i = i + 1;
                if (i < p.length() && p.charAt(i) == '\'') {
                    j = this.esperar(t, j, '\'', text);
                    i = i + 1;
                } else {
                    while (i < p.length() && p.charAt(i) != '\'') {
                        j = this.esperar(t, j, p.charAt(i), text);
                        i = i + 1;
                    }
                    i = i + 1;
                }
            } else {
                j = this.esperar(t, j, c, text);
                i = i + 1;
            }
        }
        if (j != t.length()) {
            throw new DateTimeParseException(
                    "Text '" + t + "' could not be parsed, unparsed text found at index " + j, text, j);
        }

        // El reloj de 12 horas se convierte **ahora**, y no al leerlo, porque el AM/PM puede venir
        // despues en el patron: `hh:mm a`.
        if (clockHour != null) {
            long h = clockHour.longValue() % 12L;
            if (ampm != null && ampm.longValue() == 1L) {
                h = h + 12L;
            }
            campos.put(ChronoField.HOUR_OF_DAY, Long.valueOf(h));
        }
        return new Parsed(campos, offset, zona);
    }

    private int esperar(String t, int j, char c, CharSequence text) {
        if (j >= t.length() || t.charAt(j) != c) {
            throw new DateTimeParseException(
                    "Text '" + t + "' could not be parsed at index " + j, text, j);
        }
        return j + 1;
    }

    private int esperarTexto(String t, int j, String a, String b, CharSequence text) {
        if (t.regionMatches(true, j, a, 0, a.length())
                || t.regionMatches(true, j, b, 0, b.length())) {
            return j + a.length();
        }
        throw new DateTimeParseException(
                "Text '" + t + "' could not be parsed at index " + j, text, j);
    }

    // El indice del nombre que empieza en `j`. Se prueban todos porque no hay forma de saber cual es
    // sin mirar: los nombres no tienen todos el mismo largo.
    private int leerNombre(String t, int j, String[] nombres, CharSequence text) {
        int k = 0;
        while (k < nombres.length) {
            String n = nombres[k];
            if (t.regionMatches(true, j, n, 0, n.length())) {
                return k;
            }
            k = k + 1;
        }
        throw new DateTimeParseException(
                "Text '" + t + "' could not be parsed at index " + j, text, j);
    }

    // Cuantos digitos como mucho admite cada letra. Un patron de mas de un caracter fija el ancho
    // --`MM` son dos digitos-- salvo el anio, que puede tener mas.
    private static int maxDigitos(char c, int count) {
        if (c == 'y' || c == 'u') {
            return count == 2 ? 2 : 10;
        }
        if (c == 'D') {
            return count > 1 ? count : 3;
        }
        if (c == 'S') {
            return count;
        }
        return count > 1 ? count : 2;
    }

    private long leerNumero(String t, int j, char c, int count, CharSequence text, int[] fin) {
        int k = j;
        boolean negativo = false;
        if ((c == 'y' || c == 'u') && k < t.length()
                && (t.charAt(k) == '-' || t.charAt(k) == '+')) {
            negativo = t.charAt(k) == '-';
            k = k + 1;
        }
        int max = maxDigitos(c, count);
        int desde = k;
        long v = 0L;
        while (k < t.length() && k - desde < max && t.charAt(k) >= '0' && t.charAt(k) <= '9') {
            v = v * 10L + (long) (t.charAt(k) - '0');
            k = k + 1;
        }
        int leidos = k - desde;
        if (leidos == 0 || leidos < count && c != 'y' && c != 'u') {
            throw new DateTimeParseException(
                    "Text '" + t + "' could not be parsed at index " + j, text, j);
        }
        fin[0] = k;
        return negativo ? -v : v;
    }

    // `X` admite `Z` para el cero; `x` y `Z` escriben `+00:00` o `+0000`. Los tres leen las mismas
    // formas: es mas util aceptar de mas al leer que rechazar un texto que otro escribio.
    private ZoneOffset leerOffset(String t, int j, char c, CharSequence text, int[] fin) {
        if (j < t.length() && t.charAt(j) == 'Z') {
            fin[0] = j + 1;
            return ZoneOffset.UTC;
        }
        int k = j;
        while (k < t.length() && !this.esSeparador(t.charAt(k))) {
            k = k + 1;
        }
        if (k == j) {
            throw new DateTimeParseException(
                    "Text '" + t + "' could not be parsed at index " + j, text, j);
        }
        fin[0] = k;
        return ZoneOffset.of(t.substring(j, k));
    }

    private boolean esSeparador(char c) {
        return c == ' ' || c == '[' || c == ']';
    }

    private static String field(char c, int count, TemporalAccessor ta) {
        if (c == 'y' || c == 'u') {
            long y = ta.getLong(ChronoField.YEAR);
            if (count == 2) {
                return padN(y % 100, 2);
            }
            return padN(y, count);
        }
        if (c == 'M') {
            long m = ta.getLong(ChronoField.MONTH_OF_YEAR);
            if (count <= 2) {
                return padN(m, count);
            }
            if (count == 3) {
                return MON3[(int) m - 1];
            }
            return MONTHS[(int) m - 1];
        }
        if (c == 'd') {
            return padN(ta.getLong(ChronoField.DAY_OF_MONTH), count);
        }
        if (c == 'H') {
            return padN(ta.getLong(ChronoField.HOUR_OF_DAY), count);
        }
        if (c == 'h') {
            long h = ta.getLong(ChronoField.HOUR_OF_DAY) % 12;
            if (h == 0) {
                h = 12;
            }
            return padN(h, count);
        }
        if (c == 'm') {
            return padN(ta.getLong(ChronoField.MINUTE_OF_HOUR), count);
        }
        if (c == 's') {
            return padN(ta.getLong(ChronoField.SECOND_OF_MINUTE), count);
        }
        if (c == 'S') {
            String n9 = padN(ta.getLong(ChronoField.NANO_OF_SECOND), 9);
            return n9.substring(0, count);
        }
        if (c == 'a') {
            if (ta.getLong(ChronoField.HOUR_OF_DAY) < 12) {
                return "AM";
            }
            return "PM";
        }
        if (c == 'E') {
            long dow = ta.getLong(ChronoField.DAY_OF_WEEK);
            if (count <= 3) {
                return DAY3[(int) dow - 1];
            }
            return DAYS[(int) dow - 1];
        }
        if (c == 'D') {
            return padN(ta.getLong(ChronoField.DAY_OF_YEAR), count);
        }
        if (c == 'X' || c == 'x' || c == 'Z') {
            long secs = ta.getLong(ChronoField.OFFSET_SECONDS);
            // La diferencia entre las tres letras es **solo** el cero: `X` lo escribe `Z`, las otras
            // dos con numeros. Fuera de eso escriben lo mismo, y `Z` sin dos puntos.
            if (secs == 0L && c == 'X') {
                return "Z";
            }
            String signo = secs < 0L ? "-" : "+";
            long abs = secs < 0L ? -secs : secs;
            String hh = padN(abs / 3600L, 2);
            String mm = padN(abs % 3600L / 60L, 2);
            return signo + hh + (c == 'Z' ? "" : ":") + mm;
        }
        if (c == 'V') {
            ZoneId z = ta.query(java.time.temporal.TemporalQueries.zoneId());
            if (z == null) {
                throw new java.time.DateTimeException("Unable to extract ZoneId from " + ta);
            }
            return z.getId();
        }
        throw new IllegalArgumentException("Unsupported pattern letter: " + c);
    }

    private static String padN(long v, int n) {
        String s = Long.toString(v);
        while (s.length() < n) {
            s = "0" + s;
        }
        return s;
    }
}

// Lo que quedo de un parseo: los campos que el texto traia, ya resueltos.
//
// No es una fecha ni una hora: es la bolsa intermedia, y existe porque el que parsea no sabe --ni
// tiene por que saber-- en que clase van a entrar los campos. `LocalDate.from(esto)`, `Year.from(esto)`
// y `OffsetTime.from(esto)` sacan cada uno lo suyo de la misma bolsa.
//
// Los campos **crudos** se conservan ademas de los resueltos, y eso importa: `Year.from` lee `YEAR`
// directo, asi que si la resolucion los consumiera, un patron de `"yyyy"` no daria un `Year`.
final class Parsed implements TemporalAccessor {

    private final Map<TemporalField, Long> campos;
    private final ZoneOffset offset;
    private final ZoneId zona;
    private final LocalDate fecha;
    private final LocalTime hora;

    Parsed(Map<TemporalField, Long> campos, ZoneOffset offset, ZoneId zona) {
        this.campos = campos;
        this.offset = offset;
        // Sin zona escrita pero con desplazamiento, la zona **es** el desplazamiento: es lo unico
        // que se sabe del lugar, y es cierto.
        this.zona = zona != null ? zona : offset;
        this.fecha = resolverFecha(campos);
        this.hora = resolverHora(campos);
        if (this.fecha != null) {
            campos.put(ChronoField.EPOCH_DAY, Long.valueOf(this.fecha.toEpochDay()));
        }
        if (this.hora != null) {
            campos.put(ChronoField.NANO_OF_DAY, Long.valueOf(this.hora.toNanoOfDay()));
        }
        if (this.offset != null) {
            campos.put(ChronoField.OFFSET_SECONDS,
                    Long.valueOf((long) this.offset.getTotalSeconds()));
        }
    }

    // Se resuelve sobre una **copia**: el resolvedor consume el mapa que le dan, y los campos crudos
    // tienen que seguir estando para el que los lea directo.
    private static LocalDate resolverFecha(Map<TemporalField, Long> campos) {
        Map<TemporalField, Long> copia = new HashMap<TemporalField, Long>(campos);
        java.time.chrono.ChronoLocalDate d = java.time.chrono.IsoChronology.INSTANCE
                .resolveDate(copia, ResolverStyle.SMART);
        return d == null ? null : (LocalDate) d;
    }

    private static LocalTime resolverHora(Map<TemporalField, Long> campos) {
        Long h = campos.get(ChronoField.HOUR_OF_DAY);
        if (h == null) {
            return null;
        }
        Long m = campos.get(ChronoField.MINUTE_OF_HOUR);
        Long sg = campos.get(ChronoField.SECOND_OF_MINUTE);
        Long n = campos.get(ChronoField.NANO_OF_SECOND);
        return LocalTime.of((int) h.longValue(), m == null ? 0 : (int) m.longValue(),
                sg == null ? 0 : (int) sg.longValue(), n == null ? 0 : (int) n.longValue());
    }

    public boolean isSupported(TemporalField field) {
        return field != null && this.campos.containsKey(field);
    }

    public long getLong(TemporalField field) {
        Long v = this.campos.get(field);
        if (v == null) {
            throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return v.longValue();
    }

    public int get(TemporalField field) {
        long v = this.getLong(field);
        ValueRange rango = field.range();
        return (int) rango.checkValidIntValue(v, field);
    }

    public <R> R query(TemporalQuery<R> query) {
        if (query == java.time.temporal.TemporalQueries.localDate()) {
            return (R) this.fecha;
        }
        if (query == java.time.temporal.TemporalQueries.localTime()) {
            return (R) this.hora;
        }
        if (query == java.time.temporal.TemporalQueries.offset()) {
            return (R) this.offset;
        }
        if (query == java.time.temporal.TemporalQueries.zone()
                || query == java.time.temporal.TemporalQueries.zoneId()) {
            return (R) this.zona;
        }
        if (query == java.time.temporal.TemporalQueries.chronology()) {
            return (R) java.time.chrono.IsoChronology.INSTANCE;
        }
        return query.queryFrom(this);
    }

    public String toString() {
        return this.campos.toString();
    }
}
