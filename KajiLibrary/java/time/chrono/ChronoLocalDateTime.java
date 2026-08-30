package java.time.chrono;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;

// KajiLibrary's java.time.chrono.ChronoLocalDateTime — a date-time in an arbitrary calendar system:
// the chronology-agnostic supertype of LocalDateTime. It is exactly a ChronoLocalDate plus a
// LocalTime, and that split is the whole design — the TIME half is calendar-independent (every
// calendar's day has the same 24 hours), so only the date half is generic over the chronology.
//
// A KajiLibrary subset, mirroring the choices already made in ChronoLocalDate: NOT generic in the
// date type (the JDK's `<D extends ChronoLocalDate>` erases to ChronoLocalDate anyway, which is
// what the descriptors below say, and a bounded type variable erases to Object in our compiler —
// finding #100), not Comparable, and without `query`.
public interface ChronoLocalDateTime extends Temporal, TemporalAdjuster, Comparable<ChronoLocalDateTime> {

    ChronoLocalDate toLocalDate();

    LocalTime toLocalTime();

    /**
     * Esta fecha y hora, formateada con ese formateador.
     *
     * @throws java.time.DateTimeException si no se puede formatear
     */
    default String format(java.time.format.DateTimeFormatter formatter) {
        if (formatter == null) {
            throw new NullPointerException("formatter");
        }
        return formatter.format(this);
    }

    /** La fecha y hora que `temporal` tiene, en el calendario que el mismo indique. */
    static ChronoLocalDateTime from(java.time.temporal.TemporalAccessor temporal) {
        if (temporal == null) {
            throw new NullPointerException("temporal");
        }
        if (temporal instanceof ChronoLocalDateTime) {
            return (ChronoLocalDateTime) temporal;
        }
        Chronology chrono = temporal.query(java.time.temporal.TemporalQueries.chronology());
        if (chrono == null) {
            // Sin calendario declarado, el ISO: es lo que hace el JDK cuando el temporal no dice.
            return java.time.LocalDateTime.from(temporal);
        }
        ChronoLocalDate fecha = chrono.dateEpochDay(temporal.getLong(ChronoField.EPOCH_DAY));
        LocalTime hora = LocalTime.ofNanoOfDay(temporal.getLong(ChronoField.NANO_OF_DAY));
        return ChronoLocalDateTimeImpl.of(fecha, hora);
    }

    /**
     * El orden **solo por linea de tiempo**, ignorando el calendario.
     *
     * <p>Es el complemento de `compareTo`, que desempata por calendario. Ojo con usarlo en un
     * `TreeSet`: al no desempatar, dos momentos iguales de calendarios distintos comparan 0 y el
     * conjunto se queda con uno solo.
     */
    static java.util.Comparator<ChronoLocalDateTime> timeLineOrder() {
        return new LineaDeTiempoLocal();
    }

    boolean isSupported(TemporalField field);

    /**
     * Esta fecha y hora locales **en una zona**, que es lo que las convierte en un instante.
     *
     * <p>Una lectura de reloj de pared no es un momento hasta que uno dice donde cuelga el reloj.
     * Este es el metodo que lo dice.
     */
    ChronoZonedDateTime atZone(java.time.ZoneId zone);

    // ---- las seis redeclaraciones covariantes ---------------------------------------------------
    //
    // Repiten lo que `Temporal` ya declara, pero con el retorno estrechado: sumarle horas a una fecha
    // y hora sigue siendo una fecha y hora, y el que llama no tiene que castear. De paso son las que
    // hacen que el compilador emita los **metodos puente** en `LocalDateTime`, que estrecha todavia
    // mas -- sin ellas una llamada por la interfaz no encuentra la implementacion.

    ChronoLocalDateTime with(TemporalField field, long newValue);

    ChronoLocalDateTime plus(long amountToAdd, TemporalUnit unit);

    default ChronoLocalDateTime with(TemporalAdjuster adjuster) {
        // Ligado a una local: encadenar por un intermedio de tipo interfaz se pierde (#108).
        Temporal ajustado = adjuster.adjustInto(this);
        return (ChronoLocalDateTime) ajustado;
    }

    default ChronoLocalDateTime plus(java.time.temporal.TemporalAmount amount) {
        Temporal sumado = amount.addTo(this);
        return (ChronoLocalDateTime) sumado;
    }

    default ChronoLocalDateTime minus(java.time.temporal.TemporalAmount amount) {
        Temporal restado = amount.subtractFrom(this);
        return (ChronoLocalDateTime) restado;
    }

    default ChronoLocalDateTime minus(long amountToSubtract, TemporalUnit unit) {
        // `Long.MIN_VALUE` no se puede negar: se resta en dos pasos, como en el JDK.
        if (amountToSubtract == Long.MIN_VALUE) {
            ChronoLocalDateTime medio = this.plus(Long.MAX_VALUE, unit);
            return medio.plus(1L, unit);
        }
        return this.plus(-amountToSubtract, unit);
    }

    // The chronology comes from the date half — the time half has none. The intermediate is bound
    // to a local instead of chaining `toLocalDate().getChronology()`: a chained call through an
    // interface-typed intermediate is silently dropped (finding #108).
    default Chronology getChronology() {
        ChronoLocalDate date = this.toLocalDate();
        return date.getChronology();
    }

    default boolean isSupported(TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            return unit != ChronoUnit.FOREVER;
        }
        return unit != null && unit.isSupportedBy(this);
    }

    default Temporal adjustInto(Temporal temporal) {
        ChronoLocalDate date = this.toLocalDate();
        LocalTime time = this.toLocalTime();
        Temporal withDate = temporal.with(ChronoField.EPOCH_DAY, date.toEpochDay());
        return withDate.with(ChronoField.NANO_OF_DAY, time.toNanoOfDay());
    }

    // An offset turns a local date-time into an instant on the timeline: seconds since the epoch,
    // minus the offset that says how far this local reading runs ahead of UTC.
    default long toEpochSecond(ZoneOffset offset) {
        ChronoLocalDate date = this.toLocalDate();
        LocalTime time = this.toLocalTime();
        long epochDay = date.toEpochDay();
        long secs = epochDay * 86400L + (long) time.toSecondOfDay();
        return secs - (long) offset.getTotalSeconds();
    }

    default Instant toInstant(ZoneOffset offset) {
        LocalTime time = this.toLocalTime();
        return Instant.ofEpochSecond(this.toEpochSecond(offset), (long) time.getNano());
    }

    // Comparisons on the LOCAL reading (date first, then time) — not on the timeline, which needs
    // an offset. `isEqual` is not `equals`: two date-times of different calendars can name the same
    // local instant and still not be equal objects.
    /**
     * The natural order: by date, then by time. A {@code default} because it is one in the JDK,
     * so adding {@link Comparable} to this interface (#276) breaks no implementor.
     *
     * <p>The ordering itself already existed -- {@code isAfter}/{@code isBefore}/{@code isEqual}
     * are built on it. What was missing was the NAME the language knows it by: without
     * {@code Comparable}, none of these could be sorted, put in a {@code TreeSet}, or handed to
     * anything that orders.
     */
    @Override
    default int compareTo(ChronoLocalDateTime other) {
        return LocalOrder.compare(this, other);
    }

    default boolean isAfter(ChronoLocalDateTime other) {
        return LocalOrder.compare(this, other) > 0;
    }

    default boolean isBefore(ChronoLocalDateTime other) {
        return LocalOrder.compare(this, other) < 0;
    }

    default boolean isEqual(ChronoLocalDateTime other) {
        return LocalOrder.compare(this, other) == 0;
    }
}

// The shared ordering behind isAfter/isBefore/isEqual. It lives in a package-private class rather
// than a `private` interface method (Java 9+, and not worth betting the file on) and rather than a
// `default` one, which would be public surface the JDK's interface doesn't have — an EXTRA for the
// gate. The JDK spells this `compareTo`, inherited from Comparable, which this subset omits.
final class LocalOrder {

    private LocalOrder() {
    }

    static int compare(ChronoLocalDateTime self, ChronoLocalDateTime other) {
        ChronoLocalDate selfDate = self.toLocalDate();
        ChronoLocalDate otherDate = other.toLocalDate();
        long a = selfDate.toEpochDay();
        long b = otherDate.toEpochDay();
        int result = 0;
        if (a < b) {
            result = -1;
        } else if (a > b) {
            result = 1;
        } else {
            LocalTime selfTime = self.toLocalTime();
            LocalTime otherTime = other.toLocalTime();
            long ta = selfTime.toNanoOfDay();
            long tb = otherTime.toNanoOfDay();
            if (ta < tb) {
                result = -1;
            } else if (ta > tb) {
                result = 1;
            }
        }
        return result;
    }
}

// El comparador que devuelve `timeLineOrder()`: dia epoch y nano del dia, sin mirar el calendario.
final class LineaDeTiempoLocal implements java.util.Comparator<ChronoLocalDateTime> {

    public int compare(ChronoLocalDateTime a, ChronoLocalDateTime b) {
        return LocalOrder.compare(a, b);
    }
}
