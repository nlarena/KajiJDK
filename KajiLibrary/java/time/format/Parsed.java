package java.time.format;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.Chronology;
import java.time.chrono.IsoChronology;
import java.time.temporal.ChronoField;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQueries;
import java.time.temporal.TemporalQuery;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.time.temporal.ValueRange;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

// What is left of a parse: the fields the text brought, already resolved.
//
// It is neither a date nor a time: it is the bag in between, and it exists because whoever parses
// does not know --and has no business knowing-- which class the fields will go into.
// `LocalDate.from(this)`, `Year.from(this)` and `OffsetTime.from(this)` each take their own out of
// the same bag.
//
// The **raw** fields are kept alongside the resolved ones, and that matters: `Year.from` reads
// `YEAR` directly, so if resolving consumed them a `"yyyy"` pattern would not give a `Year`.
//
// **Resolving is where what the text means gets decided**, and that is why `ResolverStyle` is here
// and not in the reader. The reader only knows it read `02` where the month goes; whether
// `2023-02-30` is an error (`STRICT`), the 28th of February (`SMART`) or the 2nd of March
// (`LENIENT`) is a later question, and the chronology answers it, not the pattern.
final class Parsed implements TemporalAccessor {

    private final Map<TemporalField, Long> fields;
    private final ZoneOffset offset;
    private final ZoneId zone;
    private final Chronology chronology;
    private final LocalDate date;
    private final LocalTime time;
    private final Period excess;
    private final Boolean leapSecond;

    Parsed(ParseContext ctx, ResolverStyle style, Set<TemporalField> onlyThese, ZoneId defaultZone,
            Chronology defaultChronology) {
        Map<TemporalField, Long> m = ctx.fields;
        if (onlyThese != null) {
            // `withResolverFields`: the fields that are not in the set are taken out **before**
            // resolving. It is the only way to break a tie in a text that brings redundant and
            // contradictory information --year+day-of-year against year+month+day-- by saying which
            // of the two holds.
            Map<TemporalField, Long> filtered = new HashMap<TemporalField, Long>();
            Iterator<Map.Entry<TemporalField, Long>> it = m.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<TemporalField, Long> e = it.next();
                if (onlyThese.contains(e.getKey())) {
                    filtered.put(e.getKey(), e.getValue());
                }
            }
            m = filtered;
        }
        this.fields = m;
        this.offset = ctx.offset;
        // With no zone written but an offset present, the zone **is** the offset: it is the only
        // thing known about the place, and it is true. And if the formatter carried a zone from
        // `withZone`, that is the one that holds when the text said nothing.
        ZoneId z = ctx.zone;
        if (z == null) {
            z = ctx.offset;
        }
        if (z == null) {
            z = defaultZone;
        }
        this.zone = z;
        Chronology c = ctx.chronology;
        if (c == null) {
            c = defaultChronology;
        }
        if (c == null) {
            c = IsoChronology.INSTANCE;
        }
        this.chronology = c;

        // `y` leaves `YEAR_OF_ERA`, and nearly everything that comes after wants `YEAR`:
        // `Year.from`, `YearMonth.from` and resolving the date. The conversion happens **first**
        // and is written into the map, not into the copy the chronology consumes, because a
        // `"yyyy-MM"` pattern never forms a date and still has to give a `YearMonth`.
        //
        // **Except in strict mode with no era written**, where the year of the era is not enough:
        // `2021` without saying whether it belongs to this era or the previous one does not name a
        // year, and assuming the current one is exactly the kind of assumption `STRICT` exists not
        // to make. It is the reason `uuuu` --the proleptic year-- is the one to use with `STRICT`,
        // and not `yyyy`.
        boolean yearWithoutEra = style == ResolverStyle.STRICT
                && this.fields.containsKey(ChronoField.YEAR_OF_ERA)
                && !this.fields.containsKey(ChronoField.ERA)
                && !this.fields.containsKey(ChronoField.YEAR);
        if (!yearWithoutEra && !this.fields.containsKey(ChronoField.YEAR)
                && this.fields.containsKey(ChronoField.YEAR_OF_ERA)) {
            long yoe = this.fields.get(ChronoField.YEAR_OF_ERA).longValue();
            Long era = this.fields.get(ChronoField.ERA);
            java.time.chrono.Era e;
            if (era != null) {
                e = this.chronology.eraOf((int) era.longValue());
            } else {
                // With no era written, the current one: it is what whoever writes a bare year
                // means.
                java.util.List<java.time.chrono.Era> eras = this.chronology.eras();
                e = eras.get(eras.size() - 1);
            }
            this.fields.put(ChronoField.YEAR,
                    Long.valueOf((long) this.chronology.prolepticYear(e, (int) yoe)));
        }

        int[] excessDays = new int[1];
        this.time = resolveTime(this.fields, style, ctx.excessDay, excessDays);
        this.excess = excessDays[0] == 0 ? Period.ZERO : Period.ofDays(excessDays[0]);
        this.leapSecond = Boolean.valueOf(ctx.leapSecond);
        ChronoLocalDate raw = resolveDateFields(this.fields, style, this.chronology, yearWithoutEra);
        // It is **normalized through the epoch day** before being looked at. This library's
        // `LocalDate.of` does not validate the day against the month's length: `LocalDate.of(2023,
        // 2, 30)` returns an object that prints `2023-02-30` and whose `toEpochDay()` is the 2nd of
        // March's. Going through the epoch day turns that object into the date it really names,
        // which is the only one it makes sense to check anything about.
        ChronoLocalDate resolved = raw == null ? null
                : this.chronology.dateEpochDay(raw.toEpochDay());
        this.date = resolved == null ? null : LocalDate.ofEpochDay(resolved.toEpochDay());

        if (resolved != null && style == ResolverStyle.STRICT) {
            // **The cross-check.** In strict mode, the resolved date has to say the same as the
            // text said. It exists because the chronology's strict path ends in `LocalDate.of(year,
            // month, day)`, and this library's `LocalDate.of` **does not validate the day against
            // the month's length**: `LocalDate.of(2023, 2, 30)` returns the 2nd of March instead of
            // throwing. Without this round trip, `ISO_LOCAL_DATE.parse("2023-02-30")` would give a
            // date --wrong and silent-- where the JDK gives an error. Written down: the underlying
            // fix belongs in `LocalDate.of`, and the day it lands this can go.
            this.crossCheck(resolved, ChronoField.YEAR);
            this.crossCheck(resolved, ChronoField.MONTH_OF_YEAR);
            this.crossCheck(resolved, ChronoField.DAY_OF_MONTH);
            this.crossCheck(resolved, ChronoField.DAY_OF_YEAR);
            this.crossCheck(resolved, ChronoField.DAY_OF_WEEK);
        }
        if (resolved != null) {
            // The fields the resolved date implies are deposited back. Without this, a `"yyyy-DDD"`
            // pattern --year and day of year-- would give a `LocalDate` but not a `YearMonth`, and
            // the text brought what was needed for both.
            this.fields.put(ChronoField.EPOCH_DAY, Long.valueOf(resolved.toEpochDay()));
            this.noteField(resolved, ChronoField.YEAR);
            this.noteField(resolved, ChronoField.MONTH_OF_YEAR);
            this.noteField(resolved, ChronoField.DAY_OF_MONTH);
            this.noteField(resolved, ChronoField.DAY_OF_YEAR);
            this.noteField(resolved, ChronoField.DAY_OF_WEEK);
        }
        if (this.time != null) {
            this.fields.put(ChronoField.NANO_OF_DAY, Long.valueOf(this.time.toNanoOfDay()));
            this.fields.put(ChronoField.HOUR_OF_DAY, Long.valueOf((long) this.time.getHour()));
            this.fields.put(ChronoField.MINUTE_OF_HOUR, Long.valueOf((long) this.time.getMinute()));
            this.fields.put(ChronoField.SECOND_OF_MINUTE, Long.valueOf((long) this.time.getSecond()));
            this.fields.put(ChronoField.NANO_OF_SECOND, Long.valueOf((long) this.time.getNano()));
        }
        if (this.offset != null) {
            this.fields.put(ChronoField.OFFSET_SECONDS,
                    Long.valueOf((long) this.offset.getTotalSeconds()));
        }
        // An instant that was read --`ISO_INSTANT`-- brings only `INSTANT_SECONDS`. Unfolding it
        // into a local date and time is what lets the same text also give an `OffsetDateTime`, and
        // it is an exact deduction: with the offset at hand there is no choice to make.
        Long instant = this.fields.get(ChronoField.INSTANT_SECONDS);
        if (instant != null && resolved == null && this.offset != null) {
            LocalDateTime ldt = LocalDateTime.ofEpochSecond(instant.longValue(), 0, this.offset);
            Long nano = this.fields.get(ChronoField.NANO_OF_SECOND);
            this.fields.put(ChronoField.EPOCH_DAY, Long.valueOf(ldt.toLocalDate().toEpochDay()));
            this.fields.put(ChronoField.NANO_OF_DAY, Long.valueOf(ldt.toLocalTime().toNanoOfDay()
                    + (nano == null ? 0L : nano.longValue())));
        }
    }

    private void crossCheck(ChronoLocalDate d, ChronoField field) {
        Long read = this.fields.get(field);
        if (read != null && d.isSupported(field) && d.getLong(field) != read.longValue()) {
            throw new DateTimeException("Conflict found: " + field + " " + read
                    + " differs from " + field + " " + d.getLong(field) + " derived from " + d);
        }
    }

    private void noteField(ChronoLocalDate d, ChronoField field) {
        if (d.isSupported(field)) {
            this.fields.put(field, Long.valueOf(d.getLong(field)));
        }
    }

    LocalDate date() {
        return this.date;
    }

    LocalTime time() {
        return this.time;
    }

    Period excess() {
        return this.excess;
    }

    Boolean leapSecond() {
        return this.leapSecond;
    }

    private static Long take(Map<TemporalField, Long> fields, TemporalField field) {
        return fields.remove(field);
    }

    // The time fields collapse towards `HOUR_OF_DAY`/`MINUTE_OF_HOUR`/`SECOND_OF_MINUTE`/
    // `NANO_OF_SECOND`, which are the four a `LocalTime` understands, and only then is the time
    // built. The order matters: `NANO_OF_DAY` wins over the loose ones because it is more specific.
    private static LocalTime resolveTime(Map<TemporalField, Long> fields, ResolverStyle style,
            boolean readerExcess, int[] excessDays) {
        Map<TemporalField, Long> t = new HashMap<TemporalField, Long>(fields);
        boolean lenient = style == ResolverStyle.LENIENT;

        Long nanoOfDay = take(t, ChronoField.NANO_OF_DAY);
        if (nanoOfDay != null) {
            long v = nanoOfDay.longValue();
            t.put(ChronoField.HOUR_OF_DAY, Long.valueOf(v / 3600000000000L));
            t.put(ChronoField.MINUTE_OF_HOUR, Long.valueOf(v / 60000000000L % 60L));
            t.put(ChronoField.SECOND_OF_MINUTE, Long.valueOf(v / 1000000000L % 60L));
            t.put(ChronoField.NANO_OF_SECOND, Long.valueOf(v % 1000000000L));
        }
        Long microOfDay = take(t, ChronoField.MICRO_OF_DAY);
        if (microOfDay != null && !t.containsKey(ChronoField.HOUR_OF_DAY)) {
            long v = microOfDay.longValue();
            t.put(ChronoField.HOUR_OF_DAY, Long.valueOf(v / 3600000000L));
            t.put(ChronoField.MINUTE_OF_HOUR, Long.valueOf(v / 60000000L % 60L));
            t.put(ChronoField.SECOND_OF_MINUTE, Long.valueOf(v / 1000000L % 60L));
            t.put(ChronoField.NANO_OF_SECOND, Long.valueOf(v % 1000000L * 1000L));
        }
        Long milliOfDay = take(t, ChronoField.MILLI_OF_DAY);
        if (milliOfDay != null && !t.containsKey(ChronoField.HOUR_OF_DAY)) {
            long v = milliOfDay.longValue();
            t.put(ChronoField.HOUR_OF_DAY, Long.valueOf(v / 3600000L));
            t.put(ChronoField.MINUTE_OF_HOUR, Long.valueOf(v / 60000L % 60L));
            t.put(ChronoField.SECOND_OF_MINUTE, Long.valueOf(v / 1000L % 60L));
            t.put(ChronoField.NANO_OF_SECOND, Long.valueOf(v % 1000L * 1000000L));
        }
        Long secondOfDay = take(t, ChronoField.SECOND_OF_DAY);
        if (secondOfDay != null && !t.containsKey(ChronoField.HOUR_OF_DAY)) {
            long v = secondOfDay.longValue();
            t.put(ChronoField.HOUR_OF_DAY, Long.valueOf(v / 3600L));
            t.put(ChronoField.MINUTE_OF_HOUR, Long.valueOf(v / 60L % 60L));
            t.put(ChronoField.SECOND_OF_MINUTE, Long.valueOf(v % 60L));
        }
        Long minuteOfDay = take(t, ChronoField.MINUTE_OF_DAY);
        if (minuteOfDay != null && !t.containsKey(ChronoField.HOUR_OF_DAY)) {
            long v = minuteOfDay.longValue();
            t.put(ChronoField.HOUR_OF_DAY, Long.valueOf(v / 60L));
            t.put(ChronoField.MINUTE_OF_HOUR, Long.valueOf(v % 60L));
        }
        Long micro = take(t, ChronoField.MICRO_OF_SECOND);
        if (micro != null && !t.containsKey(ChronoField.NANO_OF_SECOND)) {
            t.put(ChronoField.NANO_OF_SECOND, Long.valueOf(micro.longValue() * 1000L));
        }
        Long milli = take(t, ChronoField.MILLI_OF_SECOND);
        if (milli != null && !t.containsKey(ChronoField.NANO_OF_SECOND)) {
            t.put(ChronoField.NANO_OF_SECOND, Long.valueOf(milli.longValue() * 1000000L));
        }

        // The twelve-hour clock. `12 AM` is hour 0 and `12 PM` is hour 12: the conversion goes
        // through `HOUR_OF_AMPM` --which runs 0..11-- precisely so as not to have to treat the 12
        // apart twice.
        Long clockOfDay = take(t, ChronoField.CLOCK_HOUR_OF_DAY);
        if (clockOfDay != null) {
            long v = clockOfDay.longValue();
            if (style == ResolverStyle.STRICT && (v < 1L || v > 24L)) {
                throw new DateTimeException("Invalid value for CLOCK_HOUR_OF_DAY: " + v);
            }
            t.put(ChronoField.HOUR_OF_DAY, Long.valueOf(v == 24L ? 0L : v));
        }
        Long clockOfAmpm = take(t, ChronoField.CLOCK_HOUR_OF_AMPM);
        if (clockOfAmpm != null) {
            long v = clockOfAmpm.longValue();
            if (style == ResolverStyle.STRICT && (v < 1L || v > 12L)) {
                throw new DateTimeException("Invalid value for CLOCK_HOUR_OF_AMPM: " + v);
            }
            t.put(ChronoField.HOUR_OF_AMPM, Long.valueOf(v == 12L ? 0L : v));
        }
        Long ampm = take(t, ChronoField.AMPM_OF_DAY);
        Long hourOfAmpm = take(t, ChronoField.HOUR_OF_AMPM);
        if (ampm != null && hourOfAmpm != null) {
            t.put(ChronoField.HOUR_OF_DAY,
                    Long.valueOf(ampm.longValue() * 12L + hourOfAmpm.longValue()));
        } else if (hourOfAmpm != null && !t.containsKey(ChronoField.HOUR_OF_DAY)) {
            t.put(ChronoField.HOUR_OF_DAY, hourOfAmpm);
        }

        Long h = t.get(ChronoField.HOUR_OF_DAY);
        if (h == null) {
            return null;
        }
        long time = h.longValue();
        long minute = fieldValue(t, ChronoField.MINUTE_OF_HOUR);
        long second = fieldValue(t, ChronoField.SECOND_OF_MINUTE);
        long nano = fieldValue(t, ChronoField.NANO_OF_SECOND);

        // `24:00:00` is not a time: it is the next day's midnight. It is kept as `00:00` plus one
        // excess day --which `parsedExcessDays` publishes-- instead of being refused, because
        // ISO-8601 writes it and refusing it would make a valid text unreadable.
        int days = readerExcess ? 1 : 0;
        if (time == 24L && minute == 0L && second == 0L && nano == 0L
                && style != ResolverStyle.STRICT) {
            time = 0L;
            days = days + 1;
        }
        if (lenient) {
            // In lenient mode the overflows accumulate upwards instead of being an error.
            long totalNanos = time * 3600000000000L + minute * 60000000000L
                    + second * 1000000000L + nano;
            long wholeDays = Math.floorDiv(totalNanos, 86400000000000L);
            long remainder = Math.floorMod(totalNanos, 86400000000000L);
            days = days + (int) wholeDays;
            excessDays[0] = days;
            return LocalTime.ofNanoOfDay(remainder);
        }
        // The leap second exists in the text and not on the clock: it is read as `:59` and written
        // down.
        if (second == 60L && style != ResolverStyle.STRICT) {
            second = 59L;
        }
        excessDays[0] = days;
        return LocalTime.of((int) time, (int) minute, (int) second, (int) nano);
    }

    private static long fieldValue(Map<TemporalField, Long> t, TemporalField field) {
        Long v = t.get(field);
        return v == null ? 0L : v.longValue();
    }

    // It resolves over a **copy**: the resolver consumes the map it is handed, and the raw fields
    // have to stay for whoever reads them directly.
    private static ChronoLocalDate resolveDateFields(Map<TemporalField, Long> fields,
            ResolverStyle style, Chronology chronology, boolean yearWithoutEra) {
        Map<TemporalField, Long> copy = new HashMap<TemporalField, Long>(fields);
        if (yearWithoutEra) {
            // The chronology would assume the current era just as above; the field is taken away so
            // that it does not.
            copy.remove(ChronoField.YEAR_OF_ERA);
        }
        ChronoLocalDate d = chronology.resolveDate(copy, style);
        if (d == null) {
            d = resolveIsoWeek(fields);
        }
        return d;
    }

    // `ISO_WEEK_DATE`'s date: `2024-W07-3`.
    //
    // It lives here and not in `IsoFields` because `TemporalField`'s `resolve` methods are not
    // implemented in `java.time.temporal` --the package is closed at 100 % of its API and is not
    // touched in this round-- and without this `ISO_WEEK_DATE` would write a text it could not read
    // back. Written down as what has to move once `IsoField.resolve` exists.
    private static LocalDate resolveIsoWeek(Map<TemporalField, Long> fields) {
        Long year = fields.get(IsoFields.WEEK_BASED_YEAR);
        Long week = fields.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        Long day = fields.get(ChronoField.DAY_OF_WEEK);
        if (year == null || week == null || day == null) {
            return null;
        }
        // The 4th of January always falls in week 1 --that is the ISO definition-- so week 1's
        // Monday is the 4th of January walked back to the Monday.
        LocalDate fourthOfJanuary = LocalDate.of((int) year.longValue(), 1, 4);
        LocalDate mondayOfWeekOne = fourthOfJanuary.minusDays(
                (long) (fourthOfJanuary.getDayOfWeek().getValue() - 1));
        return mondayOfWeekOne.plusDays((week.longValue() - 1L) * 7L + day.longValue() - 1L);
    }

    public boolean isSupported(TemporalField field) {
        return field != null && this.fields.containsKey(field);
    }

    public long getLong(TemporalField field) {
        Long v = this.fields.get(field);
        if (v == null) {
            throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return v.longValue();
    }

    public int get(TemporalField field) {
        long v = this.getLong(field);
        ValueRange range = field.range();
        return (int) range.checkValidIntValue(v, field);
    }

    public <R> R query(TemporalQuery<R> query) {
        if (query == TemporalQueries.localDate()) {
            return (R) this.date;
        }
        if (query == TemporalQueries.localTime()) {
            return (R) this.time;
        }
        if (query == TemporalQueries.offset()) {
            return (R) this.offset;
        }
        if (query == TemporalQueries.zone() || query == TemporalQueries.zoneId()) {
            return (R) this.zone;
        }
        if (query == TemporalQueries.chronology()) {
            return (R) this.chronology;
        }
        return query.queryFrom(this);
    }

    public String toString() {
        return this.fields.toString();
    }
}

// `DateTimeFormatter.parsedExcessDays()`: the day left over from a `24:00`.
//
// It is a class and not a lambda because the query has to be **the same object always**: this
// library's `query` calls are compared by identity, and a fresh lambda on every call would never
// match the one `Parsed` recognizes.
final class ExcessQuery implements TemporalQuery<Period> {

    public Period queryFrom(TemporalAccessor temporal) {
        if (temporal instanceof Parsed) {
            return ((Parsed) temporal).excess();
        }
        return Period.ZERO;
    }
}

// `DateTimeFormatter.parsedLeapSecond()`: whether the text said `:60`.
final class LeapSecondQuery implements TemporalQuery<Boolean> {

    public Boolean queryFrom(TemporalAccessor temporal) {
        if (temporal instanceof Parsed) {
            return ((Parsed) temporal).leapSecond();
        }
        return Boolean.FALSE;
    }
}
