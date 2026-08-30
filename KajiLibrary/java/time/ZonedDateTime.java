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
import java.time.format.DateTimeFormatter;
import java.time.zone.ZoneRulesException;

// KajiLibrary's java.time.ZonedDateTime -- una fecha y hora con zona, guardada como un
// `LocalDateTime`, el `ZoneOffset` resuelto, y el `ZoneId`.
//
// **Solo se admiten zonas de desplazamiento fijo.** Las de region --`America/Argentina/Buenos_Aires`--
// necesitan las reglas de transicion de la base IANA, que es una pared de datos y no de codigo:
// `ZoneId.of` las rechaza. La consecuencia visible es que aca no hay horario de verano, y por lo
// tanto no hay huecos ni solapamientos: los dos `*OffsetAtOverlap` devuelven `this` y estan
// documentados como tales.
//
// Lo que **no** cambia es la forma: la aritmetica reresuelve el desplazamiento contra la zona en cada
// operacion, aunque hoy siempre de el mismo. El dia que haya reglas de verdad, el lugar donde
// entran es `con(...)` y `ZonedDateTime.of`, y nada de lo de arriba se entera.
public final class ZonedDateTime
        implements Temporal, TemporalAdjuster, java.time.chrono.ChronoZonedDateTime {

    private final LocalDateTime dateTime;
    private final ZoneOffset offset;
    private final ZoneId zone;

    private ZonedDateTime(LocalDateTime dateTime, ZoneOffset offset, ZoneId zone) {
        this.dateTime = dateTime;
        this.offset = offset;
        this.zone = zone;
    }

    // Fixed-offset only: the zone must itself be a ZoneOffset, otherwise tzdb rules would be needed.
    private static ZoneOffset resolveOffset(ZoneId zone) {
        if (zone instanceof ZoneOffset) {
            return (ZoneOffset) zone;
        }
        throw new ZoneRulesException(
            "Region-based zones require time-zone rules (tzdb), unsupported in KajiLibrary: " + zone.getId());
    }

    public static ZonedDateTime of(LocalDateTime dateTime, ZoneId zone) {
        ZoneOffset offset = resolveOffset(zone);
        return new ZonedDateTime(dateTime, offset, zone);
    }

    public static ZonedDateTime of(int year, int month, int dayOfMonth, int hour, int minute,
            int second, int nanoOfSecond, ZoneId zone) {
        LocalDateTime dateTime = LocalDateTime.of(year, month, dayOfMonth, hour, minute, second, nanoOfSecond);
        return of(dateTime, zone);
    }

    public static ZonedDateTime ofInstant(Instant instant, ZoneId zone) {
        ZoneOffset offset = resolveOffset(zone);
        long localSecond = instant.getEpochSecond() + offset.getTotalSeconds();
        long epochDay = floorDiv(localSecond, 86400L);
        int secondOfDay = (int) (localSecond - epochDay * 86400L);
        LocalDate date = LocalDate.ofEpochDay(epochDay);
        LocalTime time = LocalTime.of(secondOfDay / 3600, (secondOfDay % 3600) / 60, secondOfDay % 60, instant.getNano());
        return new ZonedDateTime(LocalDateTime.of(date, time), offset, zone);
    }

    public static ZonedDateTime now() {
        return ofInstant(Instant.now(), ZoneOffset.UTC);
    }

    /** Ahora, en `zone`. */
    public static ZonedDateTime now(ZoneId zone) {
        if (zone == null) {
            throw new NullPointerException("zone");
        }
        return ofInstant(Instant.now(), zone);
    }

    /**
     * Ahora **segun `clock`**, y en la zona del reloj.
     *
     * <p>La forma que se puede probar: un `Clock.fixed` hace que esto devuelva siempre lo mismo, que
     * es la unica manera de escribir una prueba sobre codigo que mira la hora.
     */
    public static ZonedDateTime now(Clock clock) {
        if (clock == null) {
            throw new NullPointerException("clock");
        }
        // Ligados a locales: la llamada encadenada por un intermedio de tipo interfaz se pierde (#108).
        Instant ahora = clock.instant();
        ZoneId zona = clock.getZone();
        return ofInstant(ahora, zona);
    }

    /** La fecha y la hora por separado. */
    public static ZonedDateTime of(LocalDate date, LocalTime time, ZoneId zone) {
        return of(LocalDateTime.of(date, time), zone);
    }

    /**
     * El instante que designan `dateTime` **leidos con `offset`**, visto desde `zone`.
     *
     * <p>Los dos primeros argumentos dicen *que instante es*; el tercero, *como mostrarlo*. Si el
     * desplazamiento de la zona no es el mismo, el resultado tiene otra fecha y hora locales que las
     * que se pasaron, y eso es lo correcto: el instante manda.
     */
    public static ZonedDateTime ofInstant(LocalDateTime dateTime, ZoneOffset offset, ZoneId zone) {
        if (dateTime == null) {
            throw new NullPointerException("dateTime");
        }
        if (offset == null) {
            throw new NullPointerException("offset");
        }
        return ofInstant(dateTime.toInstant(offset), zone);
    }

    /**
     * La fecha y hora locales en `zone`, con `preferredOffset` para desempatar un solapamiento.
     *
     * <p>Aca no hay solapamientos --las zonas son de desplazamiento fijo-- asi que el preferido no
     * llega nunca a decidir nada y el desplazamiento sale de la zona. Se admite `null`, como en el
     * JDK. Ver la nota de la clase.
     */
    public static ZonedDateTime ofLocal(LocalDateTime localDateTime, ZoneId zone, ZoneOffset preferredOffset) {
        return of(localDateTime, zone);
    }

    /**
     * Los tres, **exigiendo que sean coherentes**: si `offset` no es un desplazamiento valido de
     * `zone` para esa fecha y hora, tira en vez de corregir.
     *
     * <p>Es la version estricta de {@link #ofInstant(LocalDateTime, ZoneOffset, ZoneId)}, que ante lo
     * mismo se queda con el instante y cambia la hora local. Cual de las dos se quiere depende de si
     * los datos vienen de una fuente en la que se confia.
     */
    public static ZonedDateTime ofStrict(LocalDateTime localDateTime, ZoneOffset offset, ZoneId zone) {
        if (localDateTime == null) {
            throw new NullPointerException("localDateTime");
        }
        if (offset == null) {
            throw new NullPointerException("offset");
        }
        ZoneOffset valido = resolveOffset(zone);
        if (!offset.equals(valido)) {
            throw new java.time.DateTimeException("ZoneOffset '" + offset
                    + "' is not valid for ZoneId '" + zone.getId() + "'");
        }
        return new ZonedDateTime(localDateTime, offset, zone);
    }

    /**
     * Parsea la forma ISO: `2007-12-03T10:15:30+01:00`, con `[zona]` opcional al final.
     *
     * <p>El corchete existe porque el desplazamiento **no alcanza** para recuperar la zona: `+01:00`
     * puede ser Paris o Lagos, y se comportan distinto seis meses despues. Aca solo se admiten zonas
     * de desplazamiento fijo, asi que un `[Europe/Paris]` lo rechaza `ZoneId.of` con su propio
     * mensaje, que dice exactamente lo que falta.
     */
    public static ZonedDateTime parse(CharSequence text) {
        if (text == null) {
            throw new NullPointerException("text");
        }
        String s = text.toString();
        String zonaEntre = null;
        int abre = s.indexOf('[');
        if (abre >= 0) {
            if (s.charAt(s.length() - 1) != ']') {
                throw new java.time.format.DateTimeParseException(
                        "Text '" + s + "' could not be parsed: unclosed zone region", text, abre);
            }
            zonaEntre = s.substring(abre + 1, s.length() - 1);
            s = s.substring(0, abre);
        }
        OffsetDateTime odt = OffsetDateTime.parse(s);
        ZoneOffset off = odt.getOffset();
        ZoneId zona = zonaEntre == null ? off : ZoneId.of(zonaEntre);
        return ofStrict(odt.toLocalDateTime(), off, zona);
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

    public ZoneOffset getOffset() {
        return this.offset;
    }

    public ZoneId getZone() {
        return this.zone;
    }

    public long toEpochSecond() {
        long epochDay = this.dateTime.toLocalDate().toEpochDay();
        long secondOfDay = this.dateTime.getHour() * 3600L + this.dateTime.getMinute() * 60L + this.dateTime.getSecond();
        return epochDay * 86400L + secondOfDay - this.offset.getTotalSeconds();
    }

    public Instant toInstant() {
        return Instant.ofEpochSecond(this.toEpochSecond(), this.dateTime.getNano());
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

    // ---- los dos `withZone`, y los dos del solapamiento ------------------------------------------
    //
    // Cuatro metodos que existen por una sola razon: **una fecha y hora local no siempre designa un
    // instante unico**. Cuando el reloj se atrasa por el fin del horario de verano, la hora que se
    // repite ocurre dos veces; cuando se adelanta, hay una hora que no ocurre.
    //
    // En esta biblioteca las zonas son de desplazamiento **fijo** --las de region necesitan la base
    // de datos IANA-- asi que no hay solapamientos ni huecos, y los dos `*OffsetAtOverlap` devuelven
    // `this`. Estan igual, y con esta nota, porque la firma es parte del contrato y porque el dia que
    // haya reglas de verdad este es el lugar donde se implementan.

    /** Otra zona, **la misma fecha y hora escritas**. Es otro instante. */
    public ZonedDateTime withZoneSameLocal(ZoneId zone) {
        if (zone == null) {
            throw new NullPointerException("zone");
        }
        return zone.equals(this.zone) ? this : ZonedDateTime.of(this.dateTime, zone);
    }

    /** Otra zona, **el mismo instante**: la fecha y hora se corrigen. */
    public ZonedDateTime withZoneSameInstant(ZoneId zone) {
        if (zone == null) {
            throw new NullPointerException("zone");
        }
        return zone.equals(this.zone) ? this : ZonedDateTime.ofInstant(this.toInstant(), zone);
    }

    /**
     * Esta misma fecha y hora con la zona reducida a su desplazamiento.
     *
     * <p>Sirve para congelar el momento: un `ZonedDateTime` de zona con reglas puede cambiar de
     * desplazamiento si las reglas cambian, y este no.
     */
    public ZonedDateTime withFixedOffsetZone() {
        return this.zone.equals(this.offset) ? this : ZonedDateTime.of(this.dateTime, this.offset);
    }

    /**
     * En un solapamiento, el **primero** de los dos instantes posibles.
     *
     * <p>Devuelve `this`: las zonas de esta biblioteca son de desplazamiento fijo, asi que no hay
     * solapamientos. Ver la nota de arriba.
     */
    public ZonedDateTime withEarlierOffsetAtOverlap() {
        return this;
    }

    /** El **segundo**. Ver la nota de arriba. */
    public ZonedDateTime withLaterOffsetAtOverlap() {
        return this;
    }

    // ---- `with*` de campo -----------------------------------------------------------------------

    public ZonedDateTime withYear(int year) {
        return this.con(this.dateTime.withYear(year));
    }

    public ZonedDateTime withMonth(int month) {
        return this.con(this.dateTime.withMonth(month));
    }

    public ZonedDateTime withDayOfMonth(int dayOfMonth) {
        return this.con(this.dateTime.withDayOfMonth(dayOfMonth));
    }

    public ZonedDateTime withDayOfYear(int dayOfYear) {
        return this.con(this.dateTime.withDayOfYear(dayOfYear));
    }

    public ZonedDateTime withHour(int hour) {
        return this.con(this.dateTime.withHour(hour));
    }

    public ZonedDateTime withMinute(int minute) {
        return this.con(this.dateTime.withMinute(minute));
    }

    public ZonedDateTime withSecond(int second) {
        return this.con(this.dateTime.withSecond(second));
    }

    public ZonedDateTime withNano(int nanoOfSecond) {
        return this.con(this.dateTime.withNano(nanoOfSecond));
    }

    public ZonedDateTime truncatedTo(TemporalUnit unit) {
        return this.con(this.dateTime.truncatedTo(unit));
    }

    // Rehace el objeto con otra fecha y hora local, **reresolviendo** el desplazamiento contra la
    // zona. Con zonas fijas da el mismo; con reglas de verdad es donde el horario de verano entra.
    private ZonedDateTime con(LocalDateTime nuevo) {
        return nuevo.equals(this.dateTime) ? this : ZonedDateTime.of(nuevo, this.zone);
    }

    public ZonedDateTime plusNanos(long nanos) {
        return this.con(this.dateTime.plusNanos(nanos));
    }

    public ZonedDateTime minusNanos(long nanos) {
        return this.plusNanos(-nanos);
    }

    public ZonedDateTime plus(long amountToAdd, TemporalUnit unit) {
        return this.con(this.dateTime.plus(amountToAdd, unit));
    }

    public ZonedDateTime minus(long amountToSubtract, TemporalUnit unit) {
        return this.plus(-amountToSubtract, unit);
    }

    public ZonedDateTime plus(TemporalAmount amount) {
        if (amount == null) {
            throw new NullPointerException("amount");
        }
        return (ZonedDateTime) amount.addTo(this);
    }

    public ZonedDateTime minus(TemporalAmount amount) {
        if (amount == null) {
            throw new NullPointerException("amount");
        }
        return (ZonedDateTime) amount.subtractFrom(this);
    }

    public ZonedDateTime with(TemporalAdjuster adjuster) {
        if (adjuster == null) {
            throw new NullPointerException("adjuster");
        }
        if (adjuster instanceof LocalDateTime) {
            return this.con((LocalDateTime) adjuster);
        }
        if (adjuster instanceof LocalDate) {
            return this.con(LocalDateTime.of((LocalDate) adjuster, this.dateTime.toLocalTime()));
        }
        if (adjuster instanceof LocalTime) {
            return this.con(LocalDateTime.of(this.dateTime.toLocalDate(), (LocalTime) adjuster));
        }
        if (adjuster instanceof ZonedDateTime) {
            return (ZonedDateTime) adjuster;
        }
        return (ZonedDateTime) adjuster.adjustInto(this);
    }

    public ZonedDateTime with(TemporalField field, long newValue) {
        if (field == null) {
            throw new NullPointerException("field");
        }
        if (field == ChronoField.OFFSET_SECONDS) {
            // Cambiar el desplazamiento de una zona fija es cambiar la zona.
            return ZonedDateTime.of(this.dateTime, ZoneOffset.ofTotalSeconds(
                    (int) ChronoField.OFFSET_SECONDS.checkValidValue(newValue)));
        }
        if (field == ChronoField.INSTANT_SECONDS) {
            return ZonedDateTime.ofInstant(
                    Instant.ofEpochSecond(newValue, (long) this.getNano()), this.zone);
        }
        if (field instanceof ChronoField) {
            return this.con(this.dateTime.with(field, newValue));
        }
        return (ZonedDateTime) field.adjustInto(this, newValue);
    }

    public boolean isSupported(TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            return unit != ChronoUnit.FOREVER;
        }
        return unit != null && unit.isSupportedBy(this);
    }

    public ValueRange range(TemporalField field) {
        if (field == ChronoField.OFFSET_SECONDS || field == ChronoField.INSTANT_SECONDS) {
            return field.range();
        }
        return this.dateTime.range(field);
    }

    public <R> R query(java.time.temporal.TemporalQuery<R> query) {
        if (query == java.time.temporal.TemporalQueries.zoneId()
                || query == java.time.temporal.TemporalQueries.zone()) {
            return (R) this.zone;
        }
        if (query == java.time.temporal.TemporalQueries.offset()) {
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

    /** Cuantas `unit` hay hasta `endExclusive`, llevandolo antes a **esta** zona. */
    public long until(Temporal endExclusive, TemporalUnit unit) {
        ZonedDateTime fin = ZonedDateTime.from(endExclusive);
        fin = fin.withZoneSameInstant(this.zone);
        return this.dateTime.until(fin.dateTime, unit);
    }

    /** La fecha y hora con zona que `temporal` tiene. */
    public static ZonedDateTime from(TemporalAccessor temporal) {
        if (temporal == null) {
            throw new NullPointerException("temporal");
        }
        if (temporal instanceof ZonedDateTime) {
            return (ZonedDateTime) temporal;
        }
        ZoneId z = temporal.query(java.time.temporal.TemporalQueries.zone());
        if (z == null) {
            throw new java.time.DateTimeException(
                    "Unable to obtain ZonedDateTime from TemporalAccessor: " + temporal);
        }
        if (temporal.isSupported(ChronoField.INSTANT_SECONDS)) {
            return ZonedDateTime.ofInstant(
                    Instant.ofEpochSecond(temporal.getLong(ChronoField.INSTANT_SECONDS),
                            temporal.getLong(ChronoField.NANO_OF_SECOND)), z);
        }
        return ZonedDateTime.of(LocalDateTime.from(temporal), z);
    }

    public OffsetDateTime toOffsetDateTime() {
        return OffsetDateTime.of(this.dateTime, this.offset);
    }

    public ZonedDateTime plusYears(long years) {
        return new ZonedDateTime(this.dateTime.plusYears(years), this.offset, this.zone);
    }

    public ZonedDateTime plusMonths(long months) {
        return new ZonedDateTime(this.dateTime.plusMonths(months), this.offset, this.zone);
    }

    public ZonedDateTime plusWeeks(long weeks) {
        return new ZonedDateTime(this.dateTime.plusWeeks(weeks), this.offset, this.zone);
    }

    public ZonedDateTime plusDays(long days) {
        return new ZonedDateTime(this.dateTime.plusDays(days), this.offset, this.zone);
    }

    public ZonedDateTime plusHours(long hours) {
        return new ZonedDateTime(this.dateTime.plusHours(hours), this.offset, this.zone);
    }

    public ZonedDateTime plusMinutes(long minutes) {
        return new ZonedDateTime(this.dateTime.plusMinutes(minutes), this.offset, this.zone);
    }

    public ZonedDateTime plusSeconds(long seconds) {
        return new ZonedDateTime(this.dateTime.plusSeconds(seconds), this.offset, this.zone);
    }

    public ZonedDateTime minusYears(long years) {
        return this.plusYears(-years);
    }

    public ZonedDateTime minusMonths(long months) {
        return this.plusMonths(-months);
    }

    public ZonedDateTime minusWeeks(long weeks) {
        return this.plusWeeks(-weeks);
    }

    public ZonedDateTime minusDays(long days) {
        return this.plusDays(-days);
    }

    public ZonedDateTime minusHours(long hours) {
        return this.plusHours(-hours);
    }

    public ZonedDateTime minusMinutes(long minutes) {
        return this.plusMinutes(-minutes);
    }

    public ZonedDateTime minusSeconds(long seconds) {
        return this.plusSeconds(-seconds);
    }

    // --- TemporalAccessor (field access delegates to the local date-time) ---

    // Los dos campos que **son de la zona y no de la fecha**: el desplazamiento, y el segundo del
    // epoch. Antes los tres metodos delegaban entero en el `LocalDateTime`, que no los tiene, asi que
    // pedirlos tiraba. El sintoma no era obvio: `formatter.format(zdt)` con un patron que llevara
    // `X` fallaba con un `IllegalArgumentException` sin mensaje, y desde afuera parecia un problema
    // del formateador. Lo encontro `FmtTest`, formateando una fecha con zona.
    public boolean isSupported(TemporalField field) {
        if (field == ChronoField.OFFSET_SECONDS || field == ChronoField.INSTANT_SECONDS) {
            return true;
        }
        return this.dateTime.isSupported(field);
    }

    public long getLong(TemporalField field) {
        if (field == ChronoField.OFFSET_SECONDS) {
            return (long) this.offset.getTotalSeconds();
        }
        if (field == ChronoField.INSTANT_SECONDS) {
            return this.toEpochSecond();
        }
        return this.dateTime.getLong(field);
    }

    public int get(TemporalField field) {
        if (field == ChronoField.INSTANT_SECONDS) {
            // No entra en un `int` y truncarlo daria un numero plausible y equivocado, que es lo
            // peor que puede pasar aca. El JDK tira, y con el mismo mensaje.
            throw new java.time.temporal.UnsupportedTemporalTypeException(
                    "Invalid field 'InstantSeconds' for get() method, use getLong() instead");
        }
        return (int) this.getLong(field);
    }

    public String format(DateTimeFormatter formatter) {
        return formatter.format(this);
    }

    // ISO-8601: local date-time + offset, with "[zoneId]" appended only for a non-offset (region)
    // zone. In the fixed-offset subset the zone is always the offset, so the bracket is never added.
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(this.dateTime.toString());
        buf.append(this.offset.getId());
        if (this.zone != this.offset) {
            buf.append("[");
            buf.append(this.zone.getId());
            buf.append("]");
        }
        return buf.toString();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ZonedDateTime) {
            ZonedDateTime other = (ZonedDateTime) obj;
            return this.dateTime.equals(other.dateTime) && this.offset.equals(other.offset)
                && this.zone.equals(other.zone);
        }
        return false;
    }

    public int hashCode() {
        int z = this.zone.hashCode();
        int zRot = (z << 3) | (z >>> 29);
        return this.dateTime.hashCode() ^ this.offset.hashCode() ^ zRot;
    }

    private static long floorDiv(long a, long b) {
        long q = a / b;
        if ((a % b != 0) && ((a ^ b) < 0)) {
            q = q - 1;
        }
        return q;
    }

    /**
     * Lee `text` con ese formateador.
     *
     * <p>El que decide que campos hay es el formateador; esta clase solo dice **cual de ellos
     * quiere**, pasando su propio `from`. Por eso un patron que no traiga fecha, hora y zona
     * falla aca y no al usar el resultado.
     *
     * @throws java.time.format.DateTimeParseException si el texto no encaja con el patron, o si lo
     *     que encaja no alcanza para una fecha y hora con zona
     */
    public static ZonedDateTime parse(CharSequence text, java.time.format.DateTimeFormatter formatter) {
        if (formatter == null) {
            throw new NullPointerException("formatter");
        }
        // Ligado a una local: encadenar por un intermedio de tipo interfaz se pierde (#108).
        java.time.temporal.TemporalQuery<ZonedDateTime> consulta = ZonedDateTime::from;
        return formatter.parse(text, consulta);
    }
}
