package java.time.chrono;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQuery;
import java.time.temporal.TemporalUnit;
import java.time.temporal.ValueRange;

// KajiLibrary's java.time.chrono.ChronoLocalDateTimeImpl -- una fecha y hora locales en un calendario
// **que no es el ISO**: exactamente una `ChronoLocalDate` mas una `LocalTime`, que es lo que dice la
// interfaz.
//
// Existe por una razon concreta: sin ella, `minguoDate.atTime(hora)` devolvia un `LocalDateTime`, y
// un `LocalDateTime` **es del calendario ISO**. El resultado compilaba, se veia bien, y su
// `getChronology()` contestaba `ISO` sobre una fecha Minguo. Un miembro que miente.
//
// La division fecha/hora es todo el diseno: la mitad de la hora no depende del calendario --el dia de
// cualquier calendario tiene las mismas 24 horas-- asi que la aritmetica de tiempo se hace sobre la
// `LocalTime`, se cuentan los dias que se desbordan, y se los suma a la fecha. La fecha no se entera
// de que existen las horas y la hora no se entera de que existen los calendarios.
//
// Es de paquete: nadie la nombra desde afuera, se la obtiene por `atTime` o por
// `Chronology.localDateTime(...)`, como en el JDK.
final class ChronoLocalDateTimeImpl implements ChronoLocalDateTime {

    private final ChronoLocalDate date;
    private final LocalTime time;

    private ChronoLocalDateTimeImpl(ChronoLocalDate date, LocalTime time) {
        this.date = date;
        this.time = time;
    }

    static ChronoLocalDateTime of(ChronoLocalDate date, LocalTime time) {
        if (date == null) {
            throw new NullPointerException("date");
        }
        if (time == null) {
            throw new NullPointerException("time");
        }
        // El ISO tiene su propia clase, que es mejor: `LocalDateTime` sabe cosas que esta no.
        if (date instanceof java.time.LocalDate) {
            return java.time.LocalDateTime.of((java.time.LocalDate) date, time);
        }
        return new ChronoLocalDateTimeImpl(date, time);
    }

    public ChronoLocalDate toLocalDate() {
        return this.date;
    }

    public LocalTime toLocalTime() {
        return this.time;
    }

    private ChronoLocalDateTime con(ChronoLocalDate nuevaFecha, LocalTime nuevaHora) {
        if (this.date == nuevaFecha && this.time == nuevaHora) {
            return this;
        }
        return ChronoLocalDateTimeImpl.of(nuevaFecha, nuevaHora);
    }

    public boolean isSupported(TemporalField field) {
        if (field instanceof ChronoField) {
            ChronoField f = (ChronoField) field;
            return f.isDateBased() || f.isTimeBased();
        }
        return field != null && field.isSupportedBy(this);
    }

    public ValueRange range(TemporalField field) {
        if (field instanceof ChronoField) {
            ChronoField f = (ChronoField) field;
            if (f.isTimeBased()) {
                return this.time.range(field);
            }
            return this.date.range(field);
        }
        return field.rangeRefinedBy(this);
    }

    public int get(TemporalField field) {
        if (field instanceof ChronoField) {
            ChronoField f = (ChronoField) field;
            if (f.isTimeBased()) {
                return this.time.get(field);
            }
            return this.date.get(field);
        }
        return (int) field.getFrom(this);
    }

    public long getLong(TemporalField field) {
        if (field instanceof ChronoField) {
            ChronoField f = (ChronoField) field;
            if (f.isTimeBased()) {
                return this.time.getLong(field);
            }
            return this.date.getLong(field);
        }
        return field.getFrom(this);
    }

    public ChronoLocalDateTime with(TemporalField field, long newValue) {
        if (field instanceof ChronoField) {
            ChronoField f = (ChronoField) field;
            if (f.isTimeBased()) {
                return this.con(this.date, this.time.with(field, newValue));
            }
            return this.con(this.date.with(field, newValue), this.time);
        }
        Temporal ajustado = field.adjustInto(this, newValue);
        return (ChronoLocalDateTime) ajustado;
    }

    public ChronoLocalDateTime with(TemporalAdjuster adjuster) {
        if (adjuster instanceof ChronoLocalDate) {
            return this.con((ChronoLocalDate) adjuster, this.time);
        }
        if (adjuster instanceof LocalTime) {
            return this.con(this.date, (LocalTime) adjuster);
        }
        if (adjuster instanceof ChronoLocalDateTime) {
            return (ChronoLocalDateTime) adjuster;
        }
        Temporal ajustado = adjuster.adjustInto(this);
        return (ChronoLocalDateTime) ajustado;
    }

    public ChronoLocalDateTime plus(long amountToAdd, TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            ChronoUnit u = (ChronoUnit) unit;
            if (u == ChronoUnit.DAYS) {
                return this.con(this.date.plus(amountToAdd, ChronoUnit.DAYS), this.time);
            }
            if (u.isDateBased()) {
                // Meses, anios y demas: son cosa del calendario, la hora no cambia.
                return this.con(this.date.plus(amountToAdd, unit), this.time);
            }
            return this.masNanos(amountToAdd, u);
        }
        Temporal sumado = unit.addTo(this, amountToAdd);
        return (ChronoLocalDateTime) sumado;
    }

    // Suma en nanos y **arrastra los dias que se desbordan a la fecha**, que es lo unico que une las
    // dos mitades. El piso se toma con division hacia abajo: sumarle -1 hora a la medianoche tiene
    // que caer en el dia anterior, no quedarse en el mismo con una hora negativa.
    private ChronoLocalDateTime masNanos(long cantidad, ChronoUnit unidad) {
        long nanosPorUnidad = nanosDe(unidad);
        long total = this.time.toNanoOfDay() + cantidad * nanosPorUnidad;
        long dia = Math.floorDiv(total, 86400000000000L);
        long resto = Math.floorMod(total, 86400000000000L);
        ChronoLocalDate nuevaFecha = this.date;
        if (dia != 0L) {
            nuevaFecha = this.date.plus(dia, ChronoUnit.DAYS);
        }
        return this.con(nuevaFecha, LocalTime.ofNanoOfDay(resto));
    }

    private static long nanosDe(ChronoUnit unidad) {
        if (unidad == ChronoUnit.NANOS) {
            return 1L;
        }
        if (unidad == ChronoUnit.MICROS) {
            return 1000L;
        }
        if (unidad == ChronoUnit.MILLIS) {
            return 1000000L;
        }
        if (unidad == ChronoUnit.SECONDS) {
            return 1000000000L;
        }
        if (unidad == ChronoUnit.MINUTES) {
            return 60000000000L;
        }
        if (unidad == ChronoUnit.HOURS) {
            return 3600000000000L;
        }
        if (unidad == ChronoUnit.HALF_DAYS) {
            return 43200000000000L;
        }
        throw new java.time.temporal.UnsupportedTemporalTypeException("Unsupported unit: " + unidad);
    }

    public ChronoZonedDateTime atZone(ZoneId zone) {
        return ChronoZonedDateTimeImpl.of(this, zone);
    }

    public long until(Temporal endExclusive, TemporalUnit unit) {
        ChronoLocalDateTime fin = (ChronoLocalDateTime) endExclusive;
        if (unit instanceof ChronoUnit) {
            ChronoUnit u = (ChronoUnit) unit;
            LocalTime horaFin = fin.toLocalTime();
            ChronoLocalDate fechaFin = fin.toLocalDate();
            if (u.isDateBased()) {
                // Un dia no esta completo si la hora de llegada es anterior: se descuenta uno.
                ChronoLocalDate ajustada = fechaFin;
                if (horaFin.toNanoOfDay() < this.time.toNanoOfDay()) {
                    ajustada = fechaFin.minus(1L, ChronoUnit.DAYS);
                }
                return this.date.until(ajustada, unit);
            }
            long dias = this.date.until(fechaFin, ChronoUnit.DAYS);
            long nanos = dias * 86400000000000L + horaFin.toNanoOfDay() - this.time.toNanoOfDay();
            return nanos / nanosDe(u);
        }
        return unit.between(this, fin);
    }

    public <R> R query(TemporalQuery<R> query) {
        if (query == java.time.temporal.TemporalQueries.localDate()) {
            return (R) java.time.LocalDate.ofEpochDay(this.date.toEpochDay());
        }
        if (query == java.time.temporal.TemporalQueries.localTime()) {
            return (R) this.time;
        }
        if (query == java.time.temporal.TemporalQueries.chronology()) {
            return (R) this.date.getChronology();
        }
        if (query == java.time.temporal.TemporalQueries.precision()) {
            return (R) ChronoUnit.NANOS;
        }
        return query.queryFrom(this);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ChronoLocalDateTime) {
            ChronoLocalDateTime otro = (ChronoLocalDateTime) obj;
            ChronoLocalDate suFecha = otro.toLocalDate();
            LocalTime suHora = otro.toLocalTime();
            return this.date.equals(suFecha) && this.time.equals(suHora);
        }
        return false;
    }

    public int hashCode() {
        return this.date.hashCode() ^ this.time.hashCode();
    }

    public String toString() {
        return this.date.toString() + "T" + this.time.toString();
    }
}
