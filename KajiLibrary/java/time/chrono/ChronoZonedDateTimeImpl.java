package java.time.chrono;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQuery;
import java.time.temporal.TemporalUnit;
import java.time.temporal.ValueRange;

// KajiLibrary's java.time.chrono.ChronoZonedDateTimeImpl -- una fecha y hora **con zona** en un
// calendario que no es el ISO: una `ChronoLocalDateTime` mas la zona y el desplazamiento resuelto.
//
// Vale la misma limitacion que en `java.time.ZonedDateTime`: **solo zonas de desplazamiento fijo**.
// Las de region necesitan las reglas de la base IANA, y `ZoneId.of` ya las rechaza. La consecuencia
// es que no hay huecos ni solapamientos, y los dos `*OffsetAtOverlap` devuelven `this`.
//
// Es de paquete, como en el JDK: se llega por `atZone` o por `Chronology.zonedDateTime(...)`.
final class ChronoZonedDateTimeImpl implements ChronoZonedDateTime {

    private final ChronoLocalDateTime dateTime;
    private final ZoneOffset offset;
    private final ZoneId zone;

    private ChronoZonedDateTimeImpl(ChronoLocalDateTime dateTime, ZoneOffset offset, ZoneId zone) {
        this.dateTime = dateTime;
        this.offset = offset;
        this.zone = zone;
    }

    // Una zona sin reglas no tiene con que dar un desplazamiento: se rechaza aca y no mas adelante,
    // donde el error ya no diria de donde vino.
    private static ZoneOffset resolver(ZoneId zone) {
        if (zone == null) {
            throw new NullPointerException("zone");
        }
        if (zone instanceof ZoneOffset) {
            return (ZoneOffset) zone;
        }
        throw new java.time.zone.ZoneRulesException(
                "Region-based zones require time-zone rules (tzdb), unsupported in KajiLibrary: "
                        + zone.getId());
    }

    static ChronoZonedDateTime of(ChronoLocalDateTime dateTime, ZoneId zone) {
        if (dateTime == null) {
            throw new NullPointerException("dateTime");
        }
        ZoneOffset offset = resolver(zone);
        if (dateTime instanceof java.time.LocalDateTime) {
            // El ISO tiene su propia clase, que sabe mas que esta.
            return java.time.ZonedDateTime.of((java.time.LocalDateTime) dateTime, zone);
        }
        return new ChronoZonedDateTimeImpl(dateTime, offset, zone);
    }

    /** El instante `instant` visto desde `zone`, en el calendario de `chrono`. */
    static ChronoZonedDateTime ofInstant(Chronology chrono, Instant instant, ZoneId zone) {
        ZoneOffset offset = resolver(zone);
        long segundoLocal = instant.getEpochSecond() + (long) offset.getTotalSeconds();
        long diaEpoch = Math.floorDiv(segundoLocal, 86400L);
        int segundoDelDia = (int) Math.floorMod(segundoLocal, 86400L);
        ChronoLocalDate fecha = chrono.dateEpochDay(diaEpoch);
        LocalTime hora = LocalTime.ofNanoOfDay(segundoDelDia * 1000000000L + (long) instant.getNano());
        return of(ChronoLocalDateTimeImpl.of(fecha, hora), zone);
    }

    public ChronoLocalDateTime toLocalDateTime() {
        return this.dateTime;
    }

    public ZoneOffset getOffset() {
        return this.offset;
    }

    public ZoneId getZone() {
        return this.zone;
    }

    public LocalTime toLocalTime() {
        return this.dateTime.toLocalTime();
    }

    public long toEpochSecond() {
        return this.dateTime.toEpochSecond(this.offset);
    }

    private ChronoZonedDateTime con(ChronoLocalDateTime nuevo) {
        if (nuevo == this.dateTime) {
            return this;
        }
        return of(nuevo, this.zone);
    }

    public ChronoZonedDateTime withEarlierOffsetAtOverlap() {
        return this;
    }

    public ChronoZonedDateTime withLaterOffsetAtOverlap() {
        return this;
    }

    /** Otra zona, la misma fecha y hora escritas. Es otro instante. */
    public ChronoZonedDateTime withZoneSameLocal(ZoneId zone) {
        return of(this.dateTime, zone);
    }

    /** Otra zona, el mismo instante: la fecha y la hora se corrigen. */
    public ChronoZonedDateTime withZoneSameInstant(ZoneId zone) {
        if (zone.equals(this.zone)) {
            return this;
        }
        ChronoLocalDate fecha = this.dateTime.toLocalDate();
        Chronology chrono = fecha.getChronology();
        return ofInstant(chrono, this.toInstant(), zone);
    }

    public boolean isSupported(TemporalField field) {
        if (field instanceof ChronoField) {
            return true;
        }
        return field != null && field.isSupportedBy(this);
    }

    public ValueRange range(TemporalField field) {
        if (field == ChronoField.INSTANT_SECONDS || field == ChronoField.OFFSET_SECONDS) {
            return field.range();
        }
        return this.dateTime.range(field);
    }

    public long getLong(TemporalField field) {
        if (field == ChronoField.INSTANT_SECONDS) {
            return this.toEpochSecond();
        }
        if (field == ChronoField.OFFSET_SECONDS) {
            return (long) this.offset.getTotalSeconds();
        }
        return this.dateTime.getLong(field);
    }

    public int get(TemporalField field) {
        if (field == ChronoField.INSTANT_SECONDS) {
            throw new java.time.temporal.UnsupportedTemporalTypeException(
                    "Invalid field 'InstantSeconds' for get() method, use getLong() instead");
        }
        if (field == ChronoField.OFFSET_SECONDS) {
            return this.offset.getTotalSeconds();
        }
        return this.dateTime.get(field);
    }

    public ChronoZonedDateTime with(TemporalField field, long newValue) {
        if (field == ChronoField.INSTANT_SECONDS) {
            ChronoLocalDate fecha = this.dateTime.toLocalDate();
            Chronology chrono = fecha.getChronology();
            LocalTime hora = this.dateTime.toLocalTime();
            return ofInstant(chrono, Instant.ofEpochSecond(newValue, (long) hora.getNano()), this.zone);
        }
        if (field == ChronoField.OFFSET_SECONDS) {
            // Con zonas de desplazamiento fijo, cambiar el desplazamiento es cambiar la zona.
            long valido = ChronoField.OFFSET_SECONDS.checkValidValue(newValue);
            return of(this.dateTime, ZoneOffset.ofTotalSeconds((int) valido));
        }
        return this.con(this.dateTime.with(field, newValue));
    }

    public ChronoZonedDateTime plus(long amountToAdd, TemporalUnit unit) {
        return this.con(this.dateTime.plus(amountToAdd, unit));
    }

    public ChronoZonedDateTime with(TemporalAdjuster adjuster) {
        if (adjuster instanceof ChronoZonedDateTime) {
            return (ChronoZonedDateTime) adjuster;
        }
        return this.con(this.dateTime.with(adjuster));
    }

    /** Cuantas `unit` hay hasta `endExclusive`, llevandolo antes a **esta** zona. */
    public long until(Temporal endExclusive, TemporalUnit unit) {
        ChronoZonedDateTime fin = (ChronoZonedDateTime) endExclusive;
        ChronoZonedDateTime enMiZona = fin.withZoneSameInstant(this.zone);
        ChronoLocalDateTime local = enMiZona.toLocalDateTime();
        return this.dateTime.until(local, unit);
    }

    public boolean isSupported(TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            return unit != ChronoUnit.FOREVER;
        }
        return unit != null && unit.isSupportedBy(this);
    }

    public <R> R query(TemporalQuery<R> query) {
        if (query == java.time.temporal.TemporalQueries.zoneId()
                || query == java.time.temporal.TemporalQueries.zone()) {
            return (R) this.zone;
        }
        if (query == java.time.temporal.TemporalQueries.offset()) {
            return (R) this.offset;
        }
        if (query == java.time.temporal.TemporalQueries.localTime()) {
            return (R) this.dateTime.toLocalTime();
        }
        if (query == java.time.temporal.TemporalQueries.chronology()) {
            ChronoLocalDate fecha = this.dateTime.toLocalDate();
            return (R) fecha.getChronology();
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
        if (obj instanceof ChronoZonedDateTime) {
            ChronoZonedDateTime otro = (ChronoZonedDateTime) obj;
            ChronoLocalDateTime suLocal = otro.toLocalDateTime();
            ZoneId suZona = otro.getZone();
            return this.dateTime.equals(suLocal) && this.zone.equals(suZona);
        }
        return false;
    }

    public int hashCode() {
        return this.dateTime.hashCode() ^ this.zone.hashCode();
    }

    public String toString() {
        String s = this.dateTime.toString() + this.offset.toString();
        if (!this.zone.equals(this.offset)) {
            s = s + "[" + this.zone.getId() + "]";
        }
        return s;
    }
}
