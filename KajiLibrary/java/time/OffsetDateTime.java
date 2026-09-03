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

// KajiLibrary's java.time.OffsetDateTime -- una fecha y hora con desplazamiento fijo respecto de
// UTC, como 2026-08-04T15:30+05:30. Compone un `LocalDateTime` y un `ZoneOffset`.
//
// Vale la misma nota que `OffsetTime` sobre los dos `withOffset*`, y aca importa mas porque el
// corrimiento puede cambiar el **dia**:
//
//   - `withOffsetSameLocal` deja la fecha y hora escritas y cambia el instante;
//   - `withOffsetSameInstant` conserva el instante y corrige la fecha y hora.
//
// La diferencia entre esta clase y `ZonedDateTime` es que aca el desplazamiento es **fijo**: no hay
// horario de verano, no hay reglas, no hay saltos. Es lo que se quiere para guardar un momento --un
// registro, una marca de tiempo-- y lo que **no** se quiere para agendar algo a futuro, donde la
// zona puede cambiar sus reglas antes de que llegue la fecha.
public final class OffsetDateTime implements Temporal, TemporalAdjuster, Comparable<OffsetDateTime> {

    private final LocalDateTime dateTime;
    private final ZoneOffset offset;

    private OffsetDateTime(LocalDateTime dateTime, ZoneOffset offset) {
        this.dateTime = dateTime;
        this.offset = offset;
    }

    /** El menor valor representable. */
    public static final OffsetDateTime MIN = new OffsetDateTime(LocalDateTime.MIN, ZoneOffset.MAX);

    /** El mayor. */
    public static final OffsetDateTime MAX = new OffsetDateTime(LocalDateTime.MAX, ZoneOffset.MIN);

    /** Ahora, en la zona por defecto. */
    public static OffsetDateTime now() {
        return OffsetDateTime.now(Clock.systemDefaultZone());
    }

    /** El que marca `clock`. La forma testeable de `now()`. */
    public static OffsetDateTime now(Clock clock) {
        if (clock == null) {
            throw new NullPointerException("clock");
        }
        return OffsetDateTime.ofInstant(clock.instant(), clock.getZone());
    }

    /** El de esa zona, ahora. */
    public static OffsetDateTime now(ZoneId zone) {
        if (zone == null) {
            throw new NullPointerException("zone");
        }
        return OffsetDateTime.ofInstant(Instant.now(), zone);
    }

    /** La fecha y hora local de ese instante en esa zona, con el desplazamiento de la zona. */
    public static OffsetDateTime ofInstant(Instant instant, ZoneId zone) {
        if (instant == null || zone == null) {
            throw new NullPointerException();
        }
        ZoneOffset off = zone.getRules().getOffset(instant);
        return new OffsetDateTime(
                LocalDateTime.ofEpochSecond(instant.getEpochSecond(), instant.getNano(), off), off);
    }

    /** La fecha y hora con desplazamiento que `temporal` tiene. */
    public static OffsetDateTime from(TemporalAccessor temporal) {
        if (temporal == null) {
            throw new NullPointerException("temporal");
        }
        if (temporal instanceof OffsetDateTime) {
            return (OffsetDateTime) temporal;
        }
        return new OffsetDateTime(LocalDateTime.from(temporal), ZoneOffset.from(temporal));
    }

    /** Parsea la forma ISO, `yyyy-MM-ddTHH:mm:ss+HH:MM`. */
    public static OffsetDateTime parse(CharSequence text) {
        if (text == null) {
            throw new NullPointerException("text");
        }
        String s = text.toString();
        // El desplazamiento arranca despues de la `T`: buscar desde el principio encontraria los
        // guiones de la fecha.
        int t = s.indexOf('T');
        if (t < 0) {
            throw new java.time.format.DateTimeParseException(
                    "Text '" + s + "' could not be parsed: no time part", text, 0);
        }
        int i = t + 1;
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
        return new OffsetDateTime(LocalDateTime.parse(s.substring(0, corte)),
                ZoneOffset.of(s.substring(corte)));
    }

    /** Con el mes como enum. */
    public static OffsetDateTime of(int year, Month month, int dayOfMonth, int hour, int minute,
            int second, int nanoOfSecond, ZoneOffset offset) {
        if (month == null) {
            throw new NullPointerException("month");
        }
        return OffsetDateTime.of(year, month.getValue(), dayOfMonth, hour, minute, second,
                nanoOfSecond, offset);
    }

    public static OffsetDateTime of(LocalDateTime dateTime, ZoneOffset offset) {
        return new OffsetDateTime(dateTime, offset);
    }

    public static OffsetDateTime of(LocalDate date, LocalTime time, ZoneOffset offset) {
        return new OffsetDateTime(LocalDateTime.of(date, time), offset);
    }

    public static OffsetDateTime of(int year, int month, int dayOfMonth, int hour, int minute,
                                    int second, int nanoOfSecond, ZoneOffset offset) {
        LocalDateTime dt = LocalDateTime.of(LocalDate.of(year, month, dayOfMonth),
            LocalTime.of(hour, minute, second, nanoOfSecond));
        return new OffsetDateTime(dt, offset);
    }

    public LocalDateTime toLocalDateTime() {
        return this.dateTime;
    }

    public LocalDate toLocalDate() {
        return this.dateTime.toLocalDate();
    }

    public LocalTime toLocalTime() {
        return this.dateTime.toLocalTime();
    }

    public ZoneOffset getOffset() {
        return this.offset;
    }

    public int getYear() {
        return this.dateTime.getYear();
    }

    public int getMonthValue() {
        return this.dateTime.getMonthValue();
    }

    public int getDayOfMonth() {
        return this.dateTime.getDayOfMonth();
    }

    public int getHour() {
        return this.dateTime.getHour();
    }

    public int getMinute() {
        return this.dateTime.getMinute();
    }

    public int getSecond() {
        return this.dateTime.getSecond();
    }

    public int getNano() {
        return this.dateTime.getNano();
    }

    public DayOfWeek getDayOfWeek() {
        return this.dateTime.getDayOfWeek();
    }

    public int getDayOfYear() {
        return this.dateTime.getDayOfYear();
    }

    public Month getMonth() {
        return this.dateTime.getMonth();
    }

    // ---- los dos `withOffset` -------------------------------------------------------------------

    /**
     * Otro desplazamiento, **la misma fecha y hora escritas**. Es otro instante.
     *
     * <p>Ver la nota de la clase: elegir esta cuando se queria la otra corre el momento por la
     * diferencia entre los dos desplazamientos, sin que nada lo avise.
     */
    public OffsetDateTime withOffsetSameLocal(ZoneOffset offset) {
        if (offset == null) {
            throw new NullPointerException("offset");
        }
        return offset.equals(this.offset) ? this : new OffsetDateTime(this.dateTime, offset);
    }

    /**
     * Otro desplazamiento, **el mismo instante**: la fecha y hora se corrigen, y pueden cambiar de
     * dia.
     */
    public OffsetDateTime withOffsetSameInstant(ZoneOffset offset) {
        if (offset == null) {
            throw new NullPointerException("offset");
        }
        if (offset.equals(this.offset)) {
            return this;
        }
        int diferencia = offset.getTotalSeconds() - this.offset.getTotalSeconds();
        return new OffsetDateTime(this.dateTime.plusSeconds((long) diferencia), offset);
    }

    // ---- `with*` de campo -----------------------------------------------------------------------

    public OffsetDateTime withYear(int year) {
        return this.con(this.dateTime.withYear(year));
    }

    public OffsetDateTime withMonth(int month) {
        return this.con(this.dateTime.withMonth(month));
    }

    public OffsetDateTime withDayOfMonth(int dayOfMonth) {
        return this.con(this.dateTime.withDayOfMonth(dayOfMonth));
    }

    public OffsetDateTime withDayOfYear(int dayOfYear) {
        return this.con(this.dateTime.withDayOfYear(dayOfYear));
    }

    public OffsetDateTime withHour(int hour) {
        return this.con(this.dateTime.withHour(hour));
    }

    public OffsetDateTime withMinute(int minute) {
        return this.con(this.dateTime.withMinute(minute));
    }

    public OffsetDateTime withSecond(int second) {
        return this.con(this.dateTime.withSecond(second));
    }

    public OffsetDateTime withNano(int nanoOfSecond) {
        return this.con(this.dateTime.withNano(nanoOfSecond));
    }

    public OffsetDateTime truncatedTo(TemporalUnit unit) {
        return this.con(this.dateTime.truncatedTo(unit));
    }

    private OffsetDateTime con(LocalDateTime nuevo) {
        return nuevo.equals(this.dateTime) ? this : new OffsetDateTime(nuevo, this.offset);
    }

    // ---- aritmetica -----------------------------------------------------------------------------
    //
    // Toda va a la fecha y hora local, conservando el desplazamiento. Sumar un dia a
    // 2026-03-28T23:00+01:00 da 2026-03-29T23:00+01:00: la hora escrita no se mueve. Es la
    // diferencia con `ZonedDateTime`, donde el mismo dia puede tener 23 o 25 horas.

    public OffsetDateTime plusYears(long years) {
        return this.con(this.dateTime.plusYears(years));
    }

    public OffsetDateTime plusMonths(long months) {
        return this.con(this.dateTime.plusMonths(months));
    }

    public OffsetDateTime plusWeeks(long weeks) {
        return this.con(this.dateTime.plusWeeks(weeks));
    }

    public OffsetDateTime plusDays(long days) {
        return this.con(this.dateTime.plusDays(days));
    }

    public OffsetDateTime plusHours(long hours) {
        return this.con(this.dateTime.plusHours(hours));
    }

    public OffsetDateTime plusMinutes(long minutes) {
        return this.con(this.dateTime.plusMinutes(minutes));
    }

    public OffsetDateTime plusSeconds(long seconds) {
        return this.con(this.dateTime.plusSeconds(seconds));
    }

    public OffsetDateTime plusNanos(long nanos) {
        return this.con(this.dateTime.plusNanos(nanos));
    }

    public OffsetDateTime minusYears(long years) {
        return this.plusYears(-years);
    }

    public OffsetDateTime minusMonths(long months) {
        return this.plusMonths(-months);
    }

    public OffsetDateTime minusWeeks(long weeks) {
        return this.plusWeeks(-weeks);
    }

    public OffsetDateTime minusDays(long days) {
        return this.plusDays(-days);
    }

    public OffsetDateTime minusHours(long hours) {
        return this.plusHours(-hours);
    }

    public OffsetDateTime minusMinutes(long minutes) {
        return this.plusMinutes(-minutes);
    }

    public OffsetDateTime minusSeconds(long seconds) {
        return this.plusSeconds(-seconds);
    }

    public OffsetDateTime minusNanos(long nanos) {
        return this.plusNanos(-nanos);
    }

    public OffsetDateTime plus(long amountToAdd, TemporalUnit unit) {
        return this.con(this.dateTime.plus(amountToAdd, unit));
    }

    public OffsetDateTime minus(long amountToSubtract, TemporalUnit unit) {
        return this.plus(-amountToSubtract, unit);
    }

    public OffsetDateTime plus(TemporalAmount amount) {
        if (amount == null) {
            throw new NullPointerException("amount");
        }
        return (OffsetDateTime) amount.addTo(this);
    }

    public OffsetDateTime minus(TemporalAmount amount) {
        if (amount == null) {
            throw new NullPointerException("amount");
        }
        return (OffsetDateTime) amount.subtractFrom(this);
    }

    public OffsetDateTime with(TemporalAdjuster adjuster) {
        if (adjuster == null) {
            throw new NullPointerException("adjuster");
        }
        if (adjuster instanceof LocalDate || adjuster instanceof LocalTime
                || adjuster instanceof LocalDateTime) {
            return this.con((LocalDateTime) LocalDateTime.from(
                    (TemporalAccessor) adjuster.adjustInto(this.dateTime)));
        }
        if (adjuster instanceof ZoneOffset) {
            return this.withOffsetSameLocal((ZoneOffset) adjuster);
        }
        if (adjuster instanceof OffsetDateTime) {
            return (OffsetDateTime) adjuster;
        }
        return (OffsetDateTime) adjuster.adjustInto(this);
    }

    public OffsetDateTime with(TemporalField field, long newValue) {
        if (field == null) {
            throw new NullPointerException("field");
        }
        if (field == ChronoField.OFFSET_SECONDS) {
            return this.withOffsetSameLocal(ZoneOffset.ofTotalSeconds(
                    (int) ChronoField.OFFSET_SECONDS.checkValidValue(newValue)));
        }
        if (field == ChronoField.INSTANT_SECONDS) {
            return OffsetDateTime.ofInstant(
                    Instant.ofEpochSecond(newValue, (long) this.getNano()), this.offset);
        }
        if (field instanceof ChronoField) {
            return this.con(this.dateTime.with(field, newValue));
        }
        return (OffsetDateTime) field.adjustInto(this, newValue);
    }

    // ---- TemporalAccessor / Temporal -------------------------------------------------------------

    /**
     * Los campos que una fecha y hora con desplazamiento tiene: **todos** los de `ChronoField`.
     *
     * <p>Y esa es la respuesta correcta, no una simplificacion: al tener fecha, hora y
     * desplazamiento, hay con que contestar `INSTANT_SECONDS` --que es lo que un `LocalDateTime`
     * solo no puede-- y tambien los de fecha y los de hora.
     *
     * <p>Lo que estaba escrito era `field != INSTANT_SECONDS || true`, que es siempre cierto: la
     * primera mitad no hace nada. Daba la respuesta correcta por accidente, y el `|| true` escondia
     * la intencion -- alguien que lo leyera se preguntaria que caso se quiso excluir.
     */
    public boolean isSupported(TemporalField field) {
        if (field instanceof ChronoField) {
            return true;
        }
        return field != null && field.isSupportedBy(this);
    }

    public boolean isSupported(TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            return unit != ChronoUnit.FOREVER;
        }
        return unit != null && unit.isSupportedBy(this);
    }

    public long getLong(TemporalField field) {
        if (field == ChronoField.OFFSET_SECONDS) {
            return this.offset.getTotalSeconds();
        }
        if (field == ChronoField.INSTANT_SECONDS) {
            return this.toEpochSecond();
        }
        return this.dateTime.getLong(field);
    }

    public int get(TemporalField field) {
        if (field == ChronoField.OFFSET_SECONDS) {
            return this.offset.getTotalSeconds();
        }
        return this.dateTime.get(field);
    }

    public ValueRange range(TemporalField field) {
        if (field == ChronoField.OFFSET_SECONDS || field == ChronoField.INSTANT_SECONDS) {
            return field.range();
        }
        return this.dateTime.range(field);
    }

    public <R> R query(java.time.temporal.TemporalQuery<R> query) {
        if (query == java.time.temporal.TemporalQueries.offset()
                || query == java.time.temporal.TemporalQueries.zone()) {
            return (R) this.offset;
        }
        if (query == java.time.temporal.TemporalQueries.localDate()) {
            return (R) this.toLocalDate();
        }
        if (query == java.time.temporal.TemporalQueries.localTime()) {
            return (R) this.toLocalTime();
        }
        if (query == java.time.temporal.TemporalQueries.precision()) {
            return (R) ChronoUnit.NANOS;
        }
        return query.queryFrom(this);
    }

    public Temporal adjustInto(Temporal temporal) {
        return temporal
                .with(ChronoField.EPOCH_DAY, this.toLocalDate().toEpochDay())
                .with(ChronoField.NANO_OF_DAY, this.toLocalTime().toNanoOfDay())
                .with(ChronoField.OFFSET_SECONDS, this.offset.getTotalSeconds());
    }

    /**
     * Cuantas `unit` hay hasta `endExclusive`.
     *
     * <p>El otro se lleva **a este desplazamiento** antes de contar, conservando su instante. Sin
     * eso, la diferencia entre 15:00+02:00 y 15:00+00:00 daria cero horas cuando son dos.
     */
    public long until(Temporal endExclusive, TemporalUnit unit) {
        OffsetDateTime fin = OffsetDateTime.from(endExclusive);
        fin = fin.withOffsetSameInstant(this.offset);
        return this.dateTime.until(fin.dateTime, unit);
    }

    // ---- conversiones y comparacion --------------------------------------------------------------

    /** Esta fecha y hora en esa zona, **conservando el instante**. */
    public ZonedDateTime atZoneSameInstant(ZoneId zone) {
        if (zone == null) {
            throw new NullPointerException("zone");
        }
        return ZonedDateTime.ofInstant(this.toInstant(), zone);
    }

    /**
     * Esta fecha y hora en esa zona, **conservando la fecha y hora escritas**.
     *
     * <p>Es otro instante, y ademas puede caer en un hueco o en una superposicion del horario de
     * verano -- ahi manda la zona, no este objeto.
     */
    public ZonedDateTime atZoneSimilarLocal(ZoneId zone) {
        if (zone == null) {
            throw new NullPointerException("zone");
        }
        return ZonedDateTime.of(this.dateTime, zone);
    }

    /** Esta fecha y hora como `ZonedDateTime` con el desplazamiento como zona. */
    public ZonedDateTime toZonedDateTime() {
        return ZonedDateTime.of(this.dateTime, this.offset);
    }

    /**
     * El orden **solo por instante**, ignorando el desplazamiento.
     *
     * <p>Es el complemento de `compareTo`, que desempata por fecha y hora local. Este dice "el mismo
     * momento es el mismo momento", y sirve para ordenar registros de husos distintos por cuando
     * ocurrieron.
     *
     * <p>Ojo con usarlo en un `TreeSet`: al no desempatar, dos fechas del mismo instante y distinto
     * desplazamiento comparan 0 y el conjunto se queda con una sola.
     */
    public static java.util.Comparator<OffsetDateTime> timeLineOrder() {
        return new OdtLineaDeTiempo();
    }

    /** Solo la hora, con este desplazamiento. */
    public OffsetTime toOffsetTime() {
        return OffsetTime.of(this.toLocalTime(), this.offset);
    }

    /** El dia epoch de la fecha local. */
    public long toEpochDay() {
        return this.toLocalDate().toEpochDay();
    }

    /** Formateada. */
    public String format(java.time.format.DateTimeFormatter formatter) {
        if (formatter == null) {
            throw new NullPointerException("formatter");
        }
        return formatter.format(this);
    }

    /** Si designan el **mismo instante**, aunque su fecha y hora escritas difieran. */
    public boolean isEqual(OffsetDateTime other) {
        return this.toEpochSecond() == other.toEpochSecond()
                && this.getNano() == other.getNano();
    }

    public boolean isBefore(OffsetDateTime other) {
        long a = this.toEpochSecond();
        long b = other.toEpochSecond();
        return a < b || (a == b && this.getNano() < other.getNano());
    }

    public boolean isAfter(OffsetDateTime other) {
        long a = this.toEpochSecond();
        long b = other.toEpochSecond();
        return a > b || (a == b && this.getNano() > other.getNano());
    }

    // Seconds from the epoch of 1970-01-01T00:00:00Z (the local date-time shifted by the offset).
    public long toEpochSecond() {
        long epochDay = this.dateTime.toLocalDate().toEpochDay();
        long secs = epochDay * 86400L + this.getHour() * 3600L + this.getMinute() * 60L + this.getSecond();
        return secs - this.offset.getTotalSeconds();
    }

    public Instant toInstant() {
        return Instant.ofEpochSecond(this.toEpochSecond(), this.getNano());
    }

    public String toString() {
        return this.dateTime.toString() + this.offset.toString();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof OffsetDateTime) {
            OffsetDateTime o = (OffsetDateTime) obj;
            return this.dateTime.equals(o.dateTime) && this.offset.equals(o.offset);
        }
        return false;
    }

    public int hashCode() {
        return this.dateTime.hashCode() ^ this.offset.hashCode();
    }

    /**
     * Por instante, y a igual instante por fecha y hora local.
     *
     * <p>El desempate no es decoracion: sin el, 15:00+02:00 y 13:00+00:00 comparan 0 sin ser
     * `equals`, y un `TreeSet` se quedaria con una sola de las dos en silencio.
     */
    public int compareTo(OffsetDateTime other) {
        if (this.offset.equals(other.offset)) {
            return this.dateTime.compareTo(other.dateTime);
        }
        long a = this.toEpochSecond();
        long b = other.toEpochSecond();
        if (a < b) {
            return -1;
        }
        if (a > b) {
            return 1;
        }
        int na = this.getNano();
        int nb = other.getNano();
        if (na < nb) {
            return -1;
        }
        if (na > nb) {
            return 1;
        }
        return this.dateTime.compareTo(other.dateTime);
    }

    /**
     * Lee `text` con ese formateador.
     *
     * <p>El que decide que campos hay es el formateador; esta clase solo dice **cual de ellos
     * quiere**, pasando su propio `from`. Por eso un patron que no traiga fecha, hora y
     * desplazamiento falla aca y no al usar el resultado.
     *
     * @throws java.time.format.DateTimeParseException si el texto no encaja con el patron, o si lo
     *     que encaja no alcanza para una fecha y hora con desplazamiento
     */
    public static OffsetDateTime parse(CharSequence text, java.time.format.DateTimeFormatter formatter) {
        if (formatter == null) {
            throw new NullPointerException("formatter");
        }
        // Ligado a una local: encadenar por un intermedio de tipo interfaz se pierde (#108).
        java.time.temporal.TemporalQuery<OffsetDateTime> consulta = OffsetDateTime::from;
        return formatter.parse(text, consulta);
    }
}

// El comparador que devuelve `OffsetDateTime.timeLineOrder()`: solo el instante.
final class OdtLineaDeTiempo implements java.util.Comparator<OffsetDateTime> {

    public int compare(OffsetDateTime a, OffsetDateTime b) {
        int c = Long.compare(a.toEpochSecond(), b.toEpochSecond());
        return c != 0 ? c : Integer.compare(a.getNano(), b.getNano());
    }
}
