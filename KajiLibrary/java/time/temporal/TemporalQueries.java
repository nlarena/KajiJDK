package java.time.temporal;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.chrono.Chronology;

// KajiLibrary's java.time.temporal.TemporalQueries — the standard TemporalQuery singletons. localDate
// and localTime extract a date/time from any temporal that carries the right fields; the marker
// queries (zoneId/zone/chronology/precision/offset) are the identity keys a temporal recognises in
// its own query() override. A KajiLibrary subset: since the value types don't override query(), the
// marker queries return null (their default), and the queries are backed by the package-private
// classes below instead of lambdas.
public final class TemporalQueries {

    private TemporalQueries() {
    }

    public static TemporalQuery<ZoneId> zoneId() {
        return new ZoneIdQuery();
    }

    public static TemporalQuery<Chronology> chronology() {
        return new ChronologyQuery();
    }

    public static TemporalQuery<TemporalUnit> precision() {
        return new PrecisionQuery();
    }

    public static TemporalQuery<ZoneId> zone() {
        return new ZoneIdQuery();
    }

    public static TemporalQuery<ZoneOffset> offset() {
        return new OffsetQuery();
    }

    public static TemporalQuery<LocalDate> localDate() {
        return new LocalDateQuery();
    }

    public static TemporalQuery<LocalTime> localTime() {
        return new LocalTimeQuery();
    }
}

final class LocalDateQuery implements TemporalQuery<LocalDate> {
    public LocalDate queryFrom(TemporalAccessor temporal) {
        if (temporal.isSupported(ChronoField.EPOCH_DAY)) {
            return LocalDate.ofEpochDay(temporal.getLong(ChronoField.EPOCH_DAY));
        }
        return null;
    }
}

final class LocalTimeQuery implements TemporalQuery<LocalTime> {
    public LocalTime queryFrom(TemporalAccessor temporal) {
        if (temporal.isSupported(ChronoField.NANO_OF_SECOND)) {
            return LocalTime.of(temporal.get(ChronoField.HOUR_OF_DAY), temporal.get(ChronoField.MINUTE_OF_HOUR),
                temporal.get(ChronoField.SECOND_OF_MINUTE), temporal.get(ChronoField.NANO_OF_SECOND));
        }
        return null;
    }
}

final class ZoneIdQuery implements TemporalQuery<ZoneId> {
    public ZoneId queryFrom(TemporalAccessor temporal) {
        return null;
    }
}

final class ChronologyQuery implements TemporalQuery<Chronology> {
    public Chronology queryFrom(TemporalAccessor temporal) {
        return null;
    }
}

final class PrecisionQuery implements TemporalQuery<TemporalUnit> {
    public TemporalUnit queryFrom(TemporalAccessor temporal) {
        return null;
    }
}

final class OffsetQuery implements TemporalQuery<ZoneOffset> {
    public ZoneOffset queryFrom(TemporalAccessor temporal) {
        return null;
    }
}
