package java.time;

import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.ValueRange;

// KajiLibrary's java.time.OffsetTime -- una hora con un desplazamiento fijo respecto de UTC, como
// 15:30+05:00. Un tipo valor que compone un `LocalTime` y un `ZoneOffset`.
//
// **La distincion que organiza toda la clase** son los dos `withOffset*`, y vale entenderla antes de
// leer el resto:
//
//   - `withOffsetSameLocal` cambia el desplazamiento y **deja la hora**: 15:30+02:00 pasa a
//     15:30+05:00. Es otro instante.
//   - `withOffsetSameInstant` cambia el desplazamiento y **corrige la hora** para que siga siendo el
//     mismo momento: 15:30+02:00 pasa a 18:30+05:00.
//
// El mismo par existe en `OffsetDateTime` y `ZonedDateTime`, y elegir el equivocado da un error de
// horas que ninguna prueba de tamaños detecta.
//
// La comparacion es por el instante equivalente en UTC, no por la hora escrita: 15:30+02:00 es
// **anterior** a 15:00+00:00 aunque su hora sea mayor. Por eso `compareTo` desempata despues por
// hora local -- si no, dos horas distintas que designan el mismo instante compararian 0 y un
// `TreeSet` se quedaria con una sola.
public final class OffsetTime implements Temporal, TemporalAdjuster, Comparable<OffsetTime> {

    private final LocalTime time;
    private final ZoneOffset offset;

    private OffsetTime(LocalTime time, ZoneOffset offset) {
        if (time == null || offset == null) {
            throw new NullPointerException();
        }
        this.time = time;
        this.offset = offset;
    }

    /** La menor hora representable, 00:00+18:00. */
    public static final OffsetTime MIN = new OffsetTime(LocalTime.MIN, ZoneOffset.MAX);

    /** La mayor, 23:59:59.999999999-18:00. */
    public static final OffsetTime MAX = new OffsetTime(LocalTime.MAX, ZoneOffset.MIN);

    public static OffsetTime of(LocalTime time, ZoneOffset offset) {
        return new OffsetTime(time, offset);
    }

    public static OffsetTime of(int hour, int minute, int second, int nanoOfSecond, ZoneOffset offset) {
        return new OffsetTime(LocalTime.of(hour, minute, second, nanoOfSecond), offset);
    }

    /** La hora con desplazamiento de ahora, en la zona por defecto. */
    public static OffsetTime now() {
        return OffsetTime.now(Clock.systemDefaultZone());
    }

    /** La que marca `clock`. La forma testeable de `now()`. */
    public static OffsetTime now(Clock clock) {
        if (clock == null) {
            throw new NullPointerException("clock");
        }
        return OffsetTime.ofInstant(clock.instant(), clock.getZone());
    }

    /** La de esa zona, ahora. */
    public static OffsetTime now(ZoneId zone) {
        if (zone == null) {
            throw new NullPointerException("zone");
        }
        return OffsetTime.ofInstant(Instant.now(), zone);
    }

    /** La hora local que ese instante marca en esa zona, con el desplazamiento de la zona. */
    public static OffsetTime ofInstant(Instant instant, ZoneId zone) {
        if (instant == null || zone == null) {
            throw new NullPointerException();
        }
        ZoneOffset off = zone.getRules().getOffset(instant);
        long segsLocales = instant.getEpochSecond() + off.getTotalSeconds();
        int segsDelDia = (int) Math.floorMod(segsLocales, 86400L);
        return new OffsetTime(LocalTime.of(segsDelDia / 3600, (segsDelDia / 60) % 60,
                segsDelDia % 60, instant.getNano()), off);
    }

    /** La hora con desplazamiento que `temporal` tiene. */
    public static OffsetTime from(TemporalAccessor temporal) {
        if (temporal == null) {
            throw new NullPointerException("temporal");
        }
        if (temporal instanceof OffsetTime) {
            return (OffsetTime) temporal;
        }
        return new OffsetTime(LocalTime.from(temporal), ZoneOffset.from(temporal));
    }

    /** Parsea la forma ISO, `HH:mm:ss+HH:MM`. */
    public static OffsetTime parse(CharSequence text) {
        if (text == null) {
            throw new NullPointerException("text");
        }
        String s = text.toString();
        // El desplazamiento arranca en el primer `+`, `-` o `Z` **despues** de la hora: buscarlo
        // desde el principio encontraria el `-` de una hora negativa, que no existe, pero el codigo
        // quedaria fragil ante un formato distinto.
        int i = 1;
        int corte = -1;
        while (i < s.length() && corte < 0) {
            char c = s.charAt(i);
            if (c == '+' || c == '-' || c == 'Z') {
                corte = i;
            }
            i = i + 1;
        }
        if (corte < 0) {
            throw new java.time.format.DateTimeParseException(
                    "Text '" + s + "' could not be parsed: no offset", text, 0);
        }
        return new OffsetTime(LocalTime.parse(s.substring(0, corte)),
                ZoneOffset.of(s.substring(corte)));
    }

    public LocalTime toLocalTime() {
        return this.time;
    }

    public ZoneOffset getOffset() {
        return this.offset;
    }

    public int getHour() {
        return this.time.getHour();
    }

    public int getMinute() {
        return this.time.getMinute();
    }

    public int getSecond() {
        return this.time.getSecond();
    }

    public int getNano() {
        return this.time.getNano();
    }

    // ---- los dos `withOffset` -------------------------------------------------------------------

    /**
     * Otro desplazamiento, **la misma hora escrita**: 15:30+02:00 pasa a 15:30+05:00.
     *
     * <p>Es **otro instante**. Ver la nota de la clase.
     */
    public OffsetTime withOffsetSameLocal(ZoneOffset offset) {
        if (offset == null) {
            throw new NullPointerException("offset");
        }
        return offset.equals(this.offset) ? this : new OffsetTime(this.time, offset);
    }

    /**
     * Otro desplazamiento, **el mismo instante**: 15:30+02:00 pasa a 18:30+05:00.
     *
     * <p>La hora se corrige por la diferencia entre los dos desplazamientos.
     */
    public OffsetTime withOffsetSameInstant(ZoneOffset offset) {
        if (offset == null) {
            throw new NullPointerException("offset");
        }
        if (offset.equals(this.offset)) {
            return this;
        }
        int diferencia = offset.getTotalSeconds() - this.offset.getTotalSeconds();
        return new OffsetTime(this.time.plusSeconds((long) diferencia), offset);
    }

    // ---- los `with*` de campo -------------------------------------------------------------------

    public OffsetTime withHour(int hour) {
        return this.conHora(this.time.withHour(hour));
    }

    public OffsetTime withMinute(int minute) {
        return this.conHora(this.time.withMinute(minute));
    }

    public OffsetTime withSecond(int second) {
        return this.conHora(this.time.withSecond(second));
    }

    public OffsetTime withNano(int nanoOfSecond) {
        return this.conHora(this.time.withNano(nanoOfSecond));
    }

    /** Truncada a un multiplo de `unit`; el desplazamiento no se toca. */
    public OffsetTime truncatedTo(TemporalUnit unit) {
        return this.conHora(this.time.truncatedTo(unit));
    }

    private OffsetTime conHora(LocalTime nueva) {
        return nueva.equals(this.time) ? this : new OffsetTime(nueva, this.offset);
    }

    // ---- aritmetica -----------------------------------------------------------------------------
    //
    // Toda va a la hora local y conserva el desplazamiento. Es lo correcto: sumar una hora a
    // 23:30+02:00 da 00:30+02:00, no cambia de zona.

    public OffsetTime plusHours(long hours) {
        return this.conHora(this.time.plusHours(hours));
    }

    public OffsetTime plusMinutes(long minutes) {
        return this.conHora(this.time.plusMinutes(minutes));
    }

    public OffsetTime plusSeconds(long seconds) {
        return this.conHora(this.time.plusSeconds(seconds));
    }

    public OffsetTime plusNanos(long nanos) {
        return this.conHora(this.time.plusNanos(nanos));
    }

    public OffsetTime minusHours(long hours) {
        return this.plusHours(-hours);
    }

    public OffsetTime minusMinutes(long minutes) {
        return this.plusMinutes(-minutes);
    }

    public OffsetTime minusSeconds(long seconds) {
        return this.plusSeconds(-seconds);
    }

    public OffsetTime minusNanos(long nanos) {
        return this.plusNanos(-nanos);
    }

    public OffsetTime plus(long amountToAdd, TemporalUnit unit) {
        return this.conHora(this.time.plus(amountToAdd, unit));
    }

    public OffsetTime minus(long amountToSubtract, TemporalUnit unit) {
        return this.plus(-amountToSubtract, unit);
    }

    public OffsetTime plus(TemporalAmount amount) {
        if (amount == null) {
            throw new NullPointerException("amount");
        }
        return (OffsetTime) amount.addTo(this);
    }

    public OffsetTime minus(TemporalAmount amount) {
        if (amount == null) {
            throw new NullPointerException("amount");
        }
        return (OffsetTime) amount.subtractFrom(this);
    }

    public OffsetTime with(TemporalAdjuster adjuster) {
        if (adjuster == null) {
            throw new NullPointerException("adjuster");
        }
        // Un ajustador que **es** un desplazamiento o una hora reemplaza esa mitad; cualquier otro
        // se aplica sobre el conjunto.
        if (adjuster instanceof LocalTime) {
            return this.conHora((LocalTime) adjuster);
        }
        if (adjuster instanceof ZoneOffset) {
            return this.withOffsetSameLocal((ZoneOffset) adjuster);
        }
        if (adjuster instanceof OffsetTime) {
            return (OffsetTime) adjuster;
        }
        return (OffsetTime) adjuster.adjustInto(this);
    }

    public OffsetTime with(TemporalField field, long newValue) {
        if (field == null) {
            throw new NullPointerException("field");
        }
        if (field == ChronoField.OFFSET_SECONDS) {
            return this.withOffsetSameLocal(
                    ZoneOffset.ofTotalSeconds((int) ChronoField.OFFSET_SECONDS.checkValidValue(newValue)));
        }
        if (field instanceof ChronoField) {
            return this.conHora(this.time.with(field, newValue));
        }
        return (OffsetTime) field.adjustInto(this, newValue);
    }

    // ---- TemporalAccessor / Temporal -------------------------------------------------------------

    public boolean isSupported(TemporalField field) {
        if (field instanceof ChronoField) {
            return field.isTimeBased() || field == ChronoField.OFFSET_SECONDS;
        }
        return field != null && field.isSupportedBy(this);
    }

    public boolean isSupported(TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            return unit.isTimeBased();
        }
        return unit != null && unit.isSupportedBy(this);
    }

    public long getLong(TemporalField field) {
        if (field == ChronoField.OFFSET_SECONDS) {
            return this.offset.getTotalSeconds();
        }
        return this.time.getLong(field);
    }

    public int get(TemporalField field) {
        if (field == ChronoField.OFFSET_SECONDS) {
            return this.offset.getTotalSeconds();
        }
        return this.time.get(field);
    }

    public ValueRange range(TemporalField field) {
        if (field == ChronoField.OFFSET_SECONDS) {
            return field.range();
        }
        return this.time.range(field);
    }

    public <R> R query(java.time.temporal.TemporalQuery<R> query) {
        if (query == java.time.temporal.TemporalQueries.offset()
                || query == java.time.temporal.TemporalQueries.zone()) {
            return (R) this.offset;
        }
        if (query == java.time.temporal.TemporalQueries.localTime()) {
            return (R) this.time;
        }
        if (query == java.time.temporal.TemporalQueries.precision()) {
            return (R) ChronoUnit.NANOS;
        }
        return query.queryFrom(this);
    }

    /** Pone en `temporal` la hora y el desplazamiento de esta. */
    public Temporal adjustInto(Temporal temporal) {
        return temporal
                .with(ChronoField.NANO_OF_DAY, this.time.toNanoOfDay())
                .with(ChronoField.OFFSET_SECONDS, this.offset.getTotalSeconds());
    }

    /** Cuantas `unit` hay entre esta hora y `endExclusive`, **comparadas en UTC**. */
    public long until(Temporal endExclusive, TemporalUnit unit) {
        OffsetTime fin = OffsetTime.from(endExclusive);
        long nanos = fin.toEpochNanoUtc() - this.toEpochNanoUtc();
        if (unit == ChronoUnit.NANOS) {
            return nanos;
        }
        if (unit == ChronoUnit.MICROS) {
            return nanos / 1000L;
        }
        if (unit == ChronoUnit.MILLIS) {
            return nanos / 1000000L;
        }
        if (unit == ChronoUnit.SECONDS) {
            return nanos / 1000000000L;
        }
        if (unit == ChronoUnit.MINUTES) {
            return nanos / 60000000000L;
        }
        if (unit == ChronoUnit.HOURS) {
            return nanos / 3600000000000L;
        }
        if (unit == ChronoUnit.HALF_DAYS) {
            return nanos / 43200000000000L;
        }
        throw new java.time.temporal.UnsupportedTemporalTypeException("Unsupported unit: " + unit);
    }

    // ---- conversiones y comparacion --------------------------------------------------------------

    /** Esta hora en esa fecha, conservando el desplazamiento. */
    public OffsetDateTime atDate(LocalDate date) {
        if (date == null) {
            throw new NullPointerException("date");
        }
        return OffsetDateTime.of(LocalDateTime.of(date, this.time), this.offset);
    }

    /** Los segundos desde la epoca de esta hora en esa fecha. */
    public long toEpochSecond(LocalDate date) {
        if (date == null) {
            throw new NullPointerException("date");
        }
        return date.toEpochDay() * 86400L + this.time.toSecondOfDay() - this.offset.getTotalSeconds();
    }

    /** Formateada. */
    public String format(java.time.format.DateTimeFormatter formatter) {
        if (formatter == null) {
            throw new NullPointerException("formatter");
        }
        return formatter.format(this);
    }

    /** Si designan el **mismo instante**, aunque su hora escrita difiera. */
    public boolean isEqual(OffsetTime other) {
        return this.toEpochNanoUtc() == other.toEpochNanoUtc();
    }

    public boolean isBefore(OffsetTime other) {
        return this.toEpochNanoUtc() < other.toEpochNanoUtc();
    }

    public boolean isAfter(OffsetTime other) {
        return this.toEpochNanoUtc() > other.toEpochNanoUtc();
    }

    public String toString() {
        return this.time.toString() + this.offset.toString();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof OffsetTime) {
            OffsetTime o = (OffsetTime) obj;
            return this.time.equals(o.time) && this.offset.equals(o.offset);
        }
        return false;
    }

    public int hashCode() {
        return this.time.hashCode() ^ this.offset.hashCode();
    }

    /**
     * Por instante, y a igual instante por hora local.
     *
     * <p>El desempate no es decoracion: sin el, 15:30+02:00 y 13:30+00:00 comparan 0 sin ser
     * `equals`, y un `TreeSet` se quedaria con una sola de las dos en silencio.
     */
    public int compareTo(OffsetTime other) {
        if (this.offset.equals(other.offset)) {
            return this.time.compareTo(other.time);
        }
        long a = this.toEpochNanoUtc();
        long b = other.toEpochNanoUtc();
        if (a < b) {
            return -1;
        }
        if (a > b) {
            return 1;
        }
        return this.time.compareTo(other.time);
    }

    private long toEpochNanoUtc() {
        long nod = this.getHour() * 3600000000000L + this.getMinute() * 60000000000L
            + this.getSecond() * 1000000000L + this.getNano();
        return nod - this.offset.getTotalSeconds() * 1000000000L;
    }

    /**
     * Lee `text` con ese formateador.
     *
     * <p>El que decide que campos hay es el formateador; esta clase solo dice **cual de ellos
     * quiere**, pasando su propio `from`. Por eso un patron que no traiga hora y desplazamiento
     * falla aca y no al usar el resultado.
     *
     * @throws java.time.format.DateTimeParseException si el texto no encaja con el patron, o si lo
     *     que encaja no alcanza para una hora con desplazamiento
     */
    public static OffsetTime parse(CharSequence text, java.time.format.DateTimeFormatter formatter) {
        if (formatter == null) {
            throw new NullPointerException("formatter");
        }
        // Ligado a una local: encadenar por un intermedio de tipo interfaz se pierde (#108).
        java.time.temporal.TemporalQuery<OffsetTime> consulta = OffsetTime::from;
        return formatter.parse(text, consulta);
    }
}
