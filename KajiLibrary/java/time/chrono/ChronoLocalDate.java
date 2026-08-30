package java.time.chrono;

import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;

// KajiLibrary's java.time.chrono.ChronoLocalDate — a date in an arbitrary calendar system, the
// chronology-agnostic supertype of LocalDate (and of the Minguo/ThaiBuddhist dates). Mirrors the
// JDK's abstract/default split so concrete calendar dates only implement the calendar-specific
// primitives (getChronology, lengthOfMonth, toEpochDay) plus the Temporal arithmetic, and inherit the
// rest as defaults.
public interface ChronoLocalDate extends Temporal, TemporalAdjuster, Comparable<ChronoLocalDate> {

    Chronology getChronology();

    /**
     * El periodo entre esta fecha y `endDateExclusive`, en el calendario de **esta**.
     *
     * <p>Abstracto y no `default` porque la respuesta depende del calendario: "un mes" no significa
     * lo mismo en el ISO que en el Hijri, y no hay una cuenta generica que sirva para los dos.
     */
    ChronoPeriod until(ChronoLocalDate endDateExclusive);

    // ---- las comparaciones entre calendarios ----------------------------------------------------
    //
    // Las tres comparan por **dia epoch**, no por año/mes/dia, y es la unica forma de que la
    // comparacion entre calendarios distintos signifique algo: un 1 de enero japones y uno ISO son
    // el mismo dia si caen en el mismo punto de la linea, sin importar como cada uno lo numere.

    default boolean isAfter(ChronoLocalDate other) {
        return this.toEpochDay() > other.toEpochDay();
    }

    default boolean isBefore(ChronoLocalDate other) {
        return this.toEpochDay() < other.toEpochDay();
    }

    /**
     * Si designan el **mismo dia**, aunque sean de calendarios distintos.
     *
     * <p>Distinto de `equals`, que exige ademas el mismo calendario. Es la diferencia entre "es el
     * mismo dia" y "es la misma fecha", y por eso las dos existen.
     */
    default boolean isEqual(ChronoLocalDate other) {
        return this.toEpochDay() == other.toEpochDay();
    }

    /** La era de esta fecha, segun su calendario. */
    default Era getEra() {
        return this.getChronology().eraOf(this.get(ChronoField.ERA));
    }

    /** Esta fecha con esa hora. */
    default ChronoLocalDateTime atTime(java.time.LocalTime localTime) {
        if (localTime == null) {
            throw new NullPointerException("localTime");
        }
        return java.time.LocalDateTime.of(java.time.LocalDate.ofEpochDay(this.toEpochDay()),
                localTime);
    }

    /**
     * Esta fecha formateada con ese formateador.
     *
     * @throws java.time.DateTimeException si no se puede formatear
     */
    default String format(java.time.format.DateTimeFormatter formatter) {
        if (formatter == null) {
            throw new NullPointerException("formatter");
        }
        return formatter.format(this);
    }

    // ---- los retornos estrechados ---------------------------------------------------------------
    //
    // Los seis repiten los de `Temporal` con el retorno estrechado a `ChronoLocalDate`. No son
    // adorno: sin ellos, `fecha.plus(1, DAYS)` sobre una referencia `ChronoLocalDate` devuelve
    // `Temporal` y hay que castear. Y son lo que hace que el compilador emita los **metodos puente**
    // en cada implementacion concreta -- sin los puentes, una llamada por el supertipo termina en
    // `NoSuchMethodError`.

    // Los tres que `Temporal` declara **abstractos** se re-declaran estrechados, sin cuerpo: cada
    // calendario concreto ya los implementa, y esto solo cambia el tipo que el llamador ve.
    ChronoLocalDate plus(long amountToAdd, TemporalUnit unit);

    ChronoLocalDate minus(long amountToSubtract, TemporalUnit unit);

    ChronoLocalDate with(TemporalField field, long newValue);

    // Y los tres que `Temporal` declara `default` repiten **su mismo cuerpo**, no una llamada a el.
    //
    // El JDK escribe `Temporal.super.plus(amount)`, que es la forma cualificada de llamar al default
    // de una superinterfaz (§15.12.1). Nuestro parser todavia no la acepta, y llamar `plus(amount)`
    // a secas seria recursion infinita -- este metodo **es** el mas especifico. Repetir el cuerpo es
    // una linea y hace exactamente lo mismo; queda anotado por si algun dia se puede escribir asi.
    default ChronoLocalDate plus(java.time.temporal.TemporalAmount amount) {
        return (ChronoLocalDate) amount.addTo(this);
    }

    default ChronoLocalDate minus(java.time.temporal.TemporalAmount amount) {
        return (ChronoLocalDate) amount.subtractFrom(this);
    }

    default ChronoLocalDate with(TemporalAdjuster adjuster) {
        return (ChronoLocalDate) adjuster.adjustInto(this);
    }

    /** La fecha que `temporal` tiene, en el calendario que el mismo indique. */
    static ChronoLocalDate from(java.time.temporal.TemporalAccessor temporal) {
        if (temporal == null) {
            throw new NullPointerException("temporal");
        }
        if (temporal instanceof ChronoLocalDate) {
            return (ChronoLocalDate) temporal;
        }
        return java.time.LocalDate.from(temporal);
    }

    /**
     * El orden **solo por linea de tiempo**, ignorando el calendario.
     *
     * <p>Es el complemento de `compareTo`, que desempata por calendario. Este dice "el mismo dia es
     * el mismo dia", y sirve para ordenar fechas de calendarios mezclados por cuando ocurrieron.
     *
     * <p>Ojo con usarlo en un `TreeSet`: al no desempatar, dos fechas del mismo dia y distinto
     * calendario comparan 0 y el conjunto se queda con una sola.
     */
    static java.util.Comparator<ChronoLocalDate> timeLineOrder() {
        return new LineaDeTiempo();
    }

    int lengthOfMonth();

    long toEpochDay();

    /**
     * The natural order: by epoch day, and -- when two dates of DIFFERENT calendars name the same
     * day -- by chronology id. That tie-break is not decoration: without it two dates that are not
     * {@code equals} would compare 0, and a {@code TreeSet} would silently drop one of them.
     *
     * <p>A {@code default} because it is one in the JDK, so adding {@link Comparable} (#276)
     * breaks no implementor.
     */
    @Override
    default int compareTo(ChronoLocalDate other) {
        long mine = this.toEpochDay();
        long theirs = other.toEpochDay();
        if (mine < theirs) {
            return -1;
        }
        if (mine > theirs) {
            return 1;
        }
        Chronology chrono = this.getChronology();
        Chronology otherChrono = other.getChronology();
        return chrono.getId().compareTo(otherChrono.getId());
    }

    default boolean isLeapYear() {
        return this.getChronology().isLeapYear(this.getLong(ChronoField.YEAR));
    }

    default int lengthOfYear() {
        if (this.isLeapYear()) {
            return 366;
        }
        return 365;
    }

    default boolean isSupported(TemporalField field) {
        if (field instanceof ChronoField) {
            return ((ChronoField) field).isDateBased();
        }
        return field != null && field.isSupportedBy(this);
    }

    default boolean isSupported(TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            return ((ChronoUnit) unit).isDateBased();
        }
        return unit != null && unit.isSupportedBy(this);
    }

    default Temporal adjustInto(Temporal temporal) {
        return temporal.with(ChronoField.EPOCH_DAY, this.toEpochDay());
    }
}

// El comparador que devuelve `timeLineOrder()`: solo el dia epoch, sin desempatar por calendario.
final class LineaDeTiempo implements java.util.Comparator<ChronoLocalDate> {

    public int compare(ChronoLocalDate a, ChronoLocalDate b) {
        return Long.compare(a.toEpochDay(), b.toEpochDay());
    }
}
