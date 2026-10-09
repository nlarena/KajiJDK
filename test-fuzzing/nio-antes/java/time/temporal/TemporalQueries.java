package java.time.temporal;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.chrono.Chronology;

// KajiLibrary's java.time.temporal.TemporalQueries -- the standard queries.
//
// **They are singletons, and that is not an optimisation: it is the contract.** A temporal
// recognises what it was asked by comparing `query == TemporalQueries.zone()` by identity --that is
// how it is written in the JDK and how it is written throughout this library-- because a
// `TemporalQuery` has no other datum to identify itself by. If every call returned a fresh object,
// none of those comparisons would ever hit: `query()` would always fall through to the generic
// `queryFrom`, which for the marker queries returns `null`.
//
// That is exactly what was happening, and it is a good example of a mistake that **breaks nothing
// visibly**: everything compiled, `zonedDateTime.query(zone())` returned `null` instead of the zone,
// and the symptom appeared three layers up as an "Unable to obtain ZonedDateTime" when parsing.
// `FmtTest` found it.
//
// `zoneId()` and `zone()` return **the same** instance: in the JDK they are two different queries
// --the second accepts an offset when there is no region zone-- but here the zones are always
// fixed-offset, so the two answers always coincide. The instance is shared instead of having two so
// that a temporal recognising one recognises both.
//
// `localDate`/`localTime` are the only ones that do real work: they take the date or the time out of
// any temporal carrying the fields. The rest are markers: without a `query()` that recognises them,
// the right answer is `null`.
public final class TemporalQueries {

    // Single instances. A `static final` field of reference type reads back correctly at run time;
    // what cannot be done is a primitive `static final` (finding #112).
    private static final ZoneIdQuery ZONE = new ZoneIdQuery();
    private static final ChronologyQuery CHRONOLOGY = new ChronologyQuery();
    private static final PrecisionQuery PRECISION = new PrecisionQuery();
    private static final OffsetQuery OFFSET = new OffsetQuery();
    private static final LocalDateQuery LOCAL_DATE = new LocalDateQuery();
    private static final LocalTimeQuery LOCAL_TIME = new LocalTimeQuery();

    private TemporalQueries() {
    }

    public static TemporalQuery<ZoneId> zoneId() {
        return ZONE;
    }

    public static TemporalQuery<Chronology> chronology() {
        return CHRONOLOGY;
    }

    public static TemporalQuery<TemporalUnit> precision() {
        return PRECISION;
    }

    public static TemporalQuery<ZoneId> zone() {
        return ZONE;
    }

    public static TemporalQuery<ZoneOffset> offset() {
        return OFFSET;
    }

    public static TemporalQuery<LocalDate> localDate() {
        return LOCAL_DATE;
    }

    public static TemporalQuery<LocalTime> localTime() {
        return LOCAL_TIME;
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
        // By `NANO_OF_DAY` and not by the four loose fields: it is the field **any** carrier of a
        // time has, and the one `LocalTime.from` uses. Asking for `NANO_OF_SECOND` left out the ones
        // that carry the whole time in a single number.
        if (temporal.isSupported(ChronoField.NANO_OF_DAY)) {
            return LocalTime.ofNanoOfDay(temporal.getLong(ChronoField.NANO_OF_DAY));
        }
        if (temporal.isSupported(ChronoField.NANO_OF_SECOND)) {
            return LocalTime.of(temporal.get(ChronoField.HOUR_OF_DAY),
                temporal.get(ChronoField.MINUTE_OF_HOUR),
                temporal.get(ChronoField.SECOND_OF_MINUTE),
                temporal.get(ChronoField.NANO_OF_SECOND));
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
