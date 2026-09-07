package java.time.format;

import java.time.ZoneId;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.Chronology;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQueries;
import java.time.temporal.TemporalQuery;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.time.temporal.ValueRange;

// The view of the object the formatter sees while writing: the original, plus the fields that are
// **deduced** from it, plus the replacements from `withZone` and `withChronology`.
//
// **WHY DEDUCING IS NECESSARY, AND WHY IT IS HONEST.** This library's `LocalTime` answers four
// fields: hour, minute, second and nano. It does not answer `AMPM_OF_DAY` or `CLOCK_HOUR_OF_AMPM`,
// which is what an `hh:mm a` pattern needs. But those two **are not new information**: they are the
// hour divided by twelve and the hour modulo twelve, two sums the specification fixes and that
// admit no other answer. Deducing them invents nothing; refusing to would leave the twelve-hour
// pattern broken for a reason that has nothing to do with formatting.
//
// (The JDK deduces the same, only inside `LocalTime`. That here it sits on the formatter's side is
// a difference of **where**, not of what: `java.time` is closed at 100 % of its API and is not
// touched in this round, so the deduction lives on the side that can be written. Written down.)
//
// What is **not** deduced is what does not follow from what is there: a `LocalTime` does not give a
// date, and that is why `isSupported(YEAR)` is still `false` and formatting fails instead of
// writing an invented year.
final class DerivedTemporal implements TemporalAccessor {

    private final TemporalAccessor base;
    private final Chronology chronology;
    private final ZoneId zone;
    // The date translated into `withChronology`'s calendar, when there is one and it differs from
    // the original's. The date fields come from here and the time fields from the original:
    // changing calendar moves the year and the month, not the time.
    private final ChronoLocalDate convertedDate;

    DerivedTemporal(TemporalAccessor base, Chronology chronology, ZoneId zone) {
        this.base = base;
        this.chronology = chronology;
        this.zone = zone;
        ChronoLocalDate converted = null;
        if (chronology != null && base.isSupported(ChronoField.EPOCH_DAY)) {
            Chronology current = base.query(TemporalQueries.chronology());
            // It is only translated when it is known **which** calendar it comes from. An object
            // that declares none is left alone: converting it would assume it was ISO, and that
            // assumption is precisely the one that cannot be made.
            if (current != null && !chronology.equals(current)) {
                converted = chronology.dateEpochDay(base.getLong(ChronoField.EPOCH_DAY));
            }
        }
        this.convertedDate = converted;
    }

    public boolean isSupported(TemporalField field) {
        if (field == null) {
            return false;
        }
        if (this.convertedDate != null && field.isDateBased()
                && this.convertedDate.isSupported(field)) {
            return true;
        }
        if (this.base.isSupported(field)) {
            return true;
        }
        if (field instanceof ChronoField) {
            return this.deducible((ChronoField) field);
        }
        // A field that is not a `ChronoField` --`IsoFields`', `JulianFields`'-- can say for itself
        // whether it can be computed. Asking it is what lets `ISO_WEEK_DATE` write a `LocalDate`,
        // which does not know the week-based year but has what it takes.
        return field.isSupportedBy(this.base);
    }

    private boolean deducible(ChronoField field) {
        if (field == ChronoField.ERA || field == ChronoField.YEAR_OF_ERA
                || field == ChronoField.PROLEPTIC_MONTH) {
            return this.base.isSupported(ChronoField.YEAR)
                    && (field != ChronoField.PROLEPTIC_MONTH
                        || this.base.isSupported(ChronoField.MONTH_OF_YEAR));
        }
        if (field == ChronoField.MILLI_OF_SECOND || field == ChronoField.MICRO_OF_SECOND) {
            return this.base.isSupported(ChronoField.NANO_OF_SECOND);
        }
        if (field == ChronoField.INSTANT_SECONDS) {
            // An `Instant` **is** an instant and yet this library's `Instant` answers `false` to
            // `INSTANT_SECONDS` --its `isSupported` only knows `NANO_OF_SECOND`-- so
            // `ISO_INSTANT.format(instant)` would not work. The value is taken from
            // `getEpochSecond()`, which is the same number through the side door. Written down as a
            // bug in `java.time`: the fix belongs in `Instant.isSupported`/`getLong`, not here.
            return this.base instanceof java.time.Instant
                    || (this.base.isSupported(ChronoField.EPOCH_DAY)
                        && this.base.isSupported(ChronoField.HOUR_OF_DAY)
                        && this.base.isSupported(ChronoField.OFFSET_SECONDS));
        }
        if (field == ChronoField.AMPM_OF_DAY || field == ChronoField.HOUR_OF_AMPM
                || field == ChronoField.CLOCK_HOUR_OF_AMPM
                || field == ChronoField.CLOCK_HOUR_OF_DAY) {
            return this.base.isSupported(ChronoField.HOUR_OF_DAY);
        }
        if (field == ChronoField.MINUTE_OF_DAY || field == ChronoField.SECOND_OF_DAY
                || field == ChronoField.NANO_OF_DAY || field == ChronoField.MILLI_OF_DAY
                || field == ChronoField.MICRO_OF_DAY) {
            return this.base.isSupported(ChronoField.HOUR_OF_DAY)
                    && this.base.isSupported(ChronoField.MINUTE_OF_HOUR);
        }
        return false;
    }

    private long fromBase(ChronoField field, long fallback) {
        if (this.base.isSupported(field)) {
            return this.base.getLong(field);
        }
        return fallback;
    }

    public long getLong(TemporalField field) {
        if (this.convertedDate != null && field != null && field.isDateBased()
                && this.convertedDate.isSupported(field)) {
            return this.convertedDate.getLong(field);
        }
        if (this.base.isSupported(field)) {
            try {
                return this.base.getLong(field);
            } catch (java.time.DateTimeException e) {
                // `isSupported` said yes and `getLong` threw. It is not impossible: this library's
                // `OffsetDateTime` answers `true` for **every** `ChronoField` --its condition is
                // `field != INSTANT_SECONDS || true`, which is always true-- and then does not know
                // how to return `YEAR_OF_ERA`. Rather than propagate the error, deducing it is
                // tried, which gives the right value. Written down as a bug in `java.time`.
                if (!(field instanceof ChronoField) || !this.deducible((ChronoField) field)) {
                    throw e;
                }
            }
        }
        if (field instanceof ChronoField && this.deducible((ChronoField) field)) {
            ChronoField c = (ChronoField) field;
            if (c == ChronoField.ERA) {
                return this.base.getLong(ChronoField.YEAR) >= 1L ? 1L : 0L;
            }
            if (c == ChronoField.YEAR_OF_ERA) {
                long y = this.base.getLong(ChronoField.YEAR);
                return y >= 1L ? y : 1L - y;
            }
            if (c == ChronoField.PROLEPTIC_MONTH) {
                return this.base.getLong(ChronoField.YEAR) * 12L
                        + this.base.getLong(ChronoField.MONTH_OF_YEAR) - 1L;
            }
            if (c == ChronoField.MILLI_OF_SECOND) {
                return this.base.getLong(ChronoField.NANO_OF_SECOND) / 1000000L;
            }
            if (c == ChronoField.MICRO_OF_SECOND) {
                return this.base.getLong(ChronoField.NANO_OF_SECOND) / 1000L;
            }
            if (c == ChronoField.INSTANT_SECONDS) {
                if (this.base instanceof java.time.Instant) {
                    return ((java.time.Instant) this.base).getEpochSecond();
                }
                return this.base.getLong(ChronoField.EPOCH_DAY) * 86400L
                        + this.base.getLong(ChronoField.HOUR_OF_DAY) * 3600L
                        + this.fromBase(ChronoField.MINUTE_OF_HOUR, 0L) * 60L
                        + this.fromBase(ChronoField.SECOND_OF_MINUTE, 0L)
                        - this.base.getLong(ChronoField.OFFSET_SECONDS);
            }
            long hour = this.base.getLong(ChronoField.HOUR_OF_DAY);
            if (c == ChronoField.AMPM_OF_DAY) {
                return hour / 12L;
            }
            if (c == ChronoField.HOUR_OF_AMPM) {
                return hour % 12L;
            }
            if (c == ChronoField.CLOCK_HOUR_OF_AMPM) {
                long h = hour % 12L;
                return h == 0L ? 12L : h;
            }
            if (c == ChronoField.CLOCK_HOUR_OF_DAY) {
                return hour == 0L ? 24L : hour;
            }
            long nanoOfDay = hour * 3600000000000L
                    + this.base.getLong(ChronoField.MINUTE_OF_HOUR) * 60000000000L
                    + this.fromBase(ChronoField.SECOND_OF_MINUTE, 0L) * 1000000000L
                    + this.fromBase(ChronoField.NANO_OF_SECOND, 0L);
            if (c == ChronoField.NANO_OF_DAY) {
                return nanoOfDay;
            }
            if (c == ChronoField.MICRO_OF_DAY) {
                return nanoOfDay / 1000L;
            }
            if (c == ChronoField.MILLI_OF_DAY) {
                return nanoOfDay / 1000000L;
            }
            if (c == ChronoField.SECOND_OF_DAY) {
                return nanoOfDay / 1000000000L;
            }
            if (c == ChronoField.MINUTE_OF_DAY) {
                return nanoOfDay / 60000000000L;
            }
        }
        if (field != null && !(field instanceof ChronoField) && field.isSupportedBy(this.base)) {
            return field.getFrom(this.base);
        }
        throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
    }

    public int get(TemporalField field) {
        ValueRange range = field.range();
        return range.checkValidIntValue(this.getLong(field), field);
    }

    public <R> R query(TemporalQuery<R> query) {
        if (this.zone != null && (query == TemporalQueries.zoneId()
                || query == TemporalQueries.zone())) {
            return (R) this.zone;
        }
        if (this.chronology != null && query == TemporalQueries.chronology()) {
            return (R) this.chronology;
        }
        return this.base.query(query);
    }

    public String toString() {
        return this.base.toString();
    }
}
