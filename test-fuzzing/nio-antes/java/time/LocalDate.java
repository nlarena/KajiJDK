package java.time;

import java.io.Serializable;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAmount;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.IsoChronology;

// KajiLibrary's java.time.LocalDate — a date without time or zone, on the ISO-8601 calendar
// (proleptic Gregorian). Immutable value type. The heart is the epoch-day ↔ (year, month, day)
// conversion (toEpochDay / ofEpochDay), the classic java.time algorithm — everything else is
// layered on it. Implements Temporal, TemporalAdjuster and Comparable. A KajiLibrary subset
// (toString/parse, more fields/units, from(TemporalAccessor) deferred).
// It does NOT declare `Comparable<LocalDate>`: it inherits `Comparable<ChronoLocalDate>` from
// `ChronoLocalDate` (#276), and a class cannot implement two parameterisations of the same
// interface. It is also what the JDK does, and for the same reason.
public final class LocalDate implements Temporal, TemporalAdjuster, ChronoLocalDate, Serializable {

    private static final long DAYS_0000_TO_1970 = 719528L;

    private final int year;
    private final int month;
    private final int day;

    private LocalDate(int year, int month, int day) {
        this.year = year;
        this.month = month;
        this.day = day;
    }

    /**
     * The date of that year, month and day.
     *
     * <p>**It validates the day against the month's length**, and that is not a courtesy: without
     * the check, `LocalDate.of(2023, 2, 30)` returned an object that printed as `2023-02-30` --a
     * date that does not exist-- and whose `toEpochDay()` was the 2nd of March's. Which is to say
     * the arithmetic and the text said different things, and neither of them warned.
     *
     * <p>The leap year counts: the 29th of February is valid in 2024 and not in 2023.
     *
     * @throws java.time.DateTimeException if the month is not in [1, 12], or the day is not in
     *     [1, length of the month]
     */
    public static LocalDate of(int year, int month, int day) {
        if (month < 1 || month > 12) {
            throw new java.time.DateTimeException("Invalid value for MonthOfYear: " + month);
        }
        if (day < 1 || day > 31) {
            throw new java.time.DateTimeException("Invalid value for DayOfMonth: " + day);
        }
        int length = Month.of(month).length(LocalDate.isLeapYear(year));
        if (day > length) {
            // The message tells the two cases apart as the JDK does, because they send you to look
            // at different things: a 29th of February in a non-leap year is usually a miscomputed
            // year, and a 31st of April a miscomputed month.
            if (day == 29 && month == 2) {
                throw new java.time.DateTimeException(
                        "Invalid date 'February 29' as '" + year + "' is not a leap year");
            }
            throw new java.time.DateTimeException(
                    "Invalid date '" + Month.of(month).name() + " " + day + "'");
        }
        return new LocalDate(year, month, day);
    }


    /** 1970-01-01, day zero. */
    public static final LocalDate EPOCH = LocalDate.ofEpochDay(0L);

    /** The earliest representable date, -999999999-01-01. */
    public static final LocalDate MIN = LocalDate.of(-999999999, 1, 1);

    /** The latest, +999999999-12-31. */
    public static final LocalDate MAX = LocalDate.of(999999999, 12, 31);

    /** The date `clock` reads. The testable form of `now()`. */
    public static LocalDate now(java.time.Clock clock) {
        if (clock == null) {
            throw new NullPointerException("clock");
        }
        return LocalDate.ofInstant(clock.instant(), clock.getZone());
    }

    /** The date in that zone, right now. */
    public static LocalDate now(ZoneId zone) {
        if (zone == null) {
            throw new NullPointerException("zone");
        }
        return LocalDate.ofInstant(Instant.now(), zone);
    }

    /**
     * The local date that instant falls on in that zone.
     *
     * <p>The same instant is a different day depending on where it is looked at from: that is why
     * the zone is needed and the instant alone is not enough.
     */
    public static LocalDate ofInstant(Instant instant, ZoneId zone) {
        if (instant == null || zone == null) {
            throw new NullPointerException();
        }
        ZoneOffset offset = zone.getRules().getOffset(instant);
        long localSecs = instant.getEpochSecond() + offset.getTotalSeconds();
        return LocalDate.ofEpochDay(Math.floorDiv(localSecs, 86400L));
    }

    /** The date with that year and month. */
    public static LocalDate of(int year, Month month, int dayOfMonth) {
        if (month == null) {
            throw new NullPointerException("month");
        }
        return LocalDate.of(year, month.getValue(), dayOfMonth);
    }

    /**
     * The date of day `dayOfYear` of `year`.
     *
     * @throws java.time.DateTimeException if the day does not exist in that year -- the 366th in a
     *     common one
     */
    public static LocalDate ofYearDay(int year, int dayOfYear) {
        ChronoField.YEAR.checkValidValue((long) year);
        ChronoField.DAY_OF_YEAR.checkValidValue((long) dayOfYear);
        boolean leap = java.time.chrono.IsoChronology.INSTANCE.isLeapYear((long) year);
        if (dayOfYear == 366 && !leap) {
            throw new java.time.DateTimeException(
                    "Invalid date 'DayOfYear 366' as '" + year + "' is not a leap year");
        }
        Month month = Month.of((dayOfYear - 1) / 31 + 1);
        // The computation above can fall a month short: it steps forward if it has to.
        int endOfMonth = month.firstDayOfYear(leap) + month.length(leap) - 1;
        if (dayOfYear > endOfMonth) {
            month = month.plus(1L);
        }
        int day = dayOfYear - month.firstDayOfYear(leap) + 1;
        return LocalDate.of(year, month.getValue(), day);
    }

    public static LocalDate now() {
        return LocalDate.ofEpochDay(System.currentTimeMillis() / 86400000L);
    }

    // Private helper: the JDK exposes leap-year testing on Year/IsoChronology, not as a static on
    // LocalDate (LocalDate only has the instance isLeapYear() below).
    private static boolean isLeapYear(long year) {
        return ((year & 3L) == 0) && ((year % 100L != 0) || (year % 400L == 0));
    }

    // --- the epoch-day conversion (the algorithmic centrepiece) ---

    public long toEpochDay() {
        long y = this.year;
        long m = this.month;
        long total = 0;
        total = total + 365L * y;
        if (y >= 0) {
            total = total + (y + 3) / 4 - (y + 99) / 100 + (y + 399) / 400;
        } else {
            total = total - (y / -4 - y / -100 + y / -400);
        }
        total = total + (367L * m - 362L) / 12L;
        total = total + (this.day - 1);
        if (m > 2) {
            total = total - 1;
            if (!LocalDate.isLeapYear(this.year)) {
                total = total - 1;
            }
        }
        return total - DAYS_0000_TO_1970;
    }

    /**
     * The date `temporal` holds.
     *
     * <p>It is read through `EPOCH_DAY`, the field that **every** temporal with a date knows how to
     * give --be it a `LocalDate`, a `LocalDateTime` or a `ZonedDateTime`--. Reading year/month/day
     * separately would work too and would be worse: three fields that may come from different
     * calendars, against one that is already absolute.
     *
     * @throws java.time.DateTimeException if `temporal` has no date
     */
    public static LocalDate from(java.time.temporal.TemporalAccessor temporal) {
        if (temporal == null) {
            throw new NullPointerException("temporal");
        }
        if (temporal instanceof LocalDate) {
            return (LocalDate) temporal;
        }
        if (!temporal.isSupported(ChronoField.EPOCH_DAY)) {
            throw new java.time.DateTimeException(
                    "Unable to obtain LocalDate from TemporalAccessor: " + temporal);
        }
        return LocalDate.ofEpochDay(temporal.getLong(ChronoField.EPOCH_DAY));
    }

    public static LocalDate ofEpochDay(long epochDay) {
        long zeroDay = epochDay + DAYS_0000_TO_1970;
        zeroDay = zeroDay - 60;
        long adjust = 0;
        if (zeroDay < 0) {
            long adjustCycles = (zeroDay + 1) / 146097 - 1;
            adjust = adjustCycles * 400;
            zeroDay = zeroDay + (-adjustCycles * 146097);
        }
        long yearEst = (400 * zeroDay + 591) / 146097;
        long doyEst = zeroDay - (365 * yearEst + yearEst / 4 - yearEst / 100 + yearEst / 400);
        if (doyEst < 0) {
            yearEst = yearEst - 1;
            doyEst = zeroDay - (365 * yearEst + yearEst / 4 - yearEst / 100 + yearEst / 400);
        }
        yearEst = yearEst + adjust;
        int marchDoy0 = (int) doyEst;
        int marchMonth0 = (marchDoy0 * 5 + 2) / 153;
        int month = (marchMonth0 + 2) % 12 + 1;
        int dom = marchDoy0 - (marchMonth0 * 306 + 5) / 10 + 1;
        yearEst = yearEst + marchMonth0 / 10;
        return new LocalDate((int) yearEst, month, dom);
    }

    // --- accessors ---

    public int getYear() {
        return this.year;
    }

    public int getMonthValue() {
        return this.month;
    }

    public int getDayOfMonth() {
        return this.day;
    }

    public Month getMonth() {
        return Month.of(this.month);
    }

    public DayOfWeek getDayOfWeek() {
        long r = (this.toEpochDay() + 3) % 7;
        if (r < 0) {
            r = r + 7;
        }
        return DayOfWeek.of((int) r + 1);
    }

    public int getDayOfYear() {
        return (int) (this.toEpochDay() - LocalDate.of(this.year, 1, 1).toEpochDay()) + 1;
    }

    public boolean isLeapYear() {
        return LocalDate.isLeapYear(this.year);
    }

    public int lengthOfMonth() {
        return Month.of(this.month).length(this.isLeapYear());
    }

    public int lengthOfYear() {
        return this.isLeapYear() ? 366 : 365;
    }

    // --- arithmetic ---

    public LocalDate plusDays(long daysToAdd) {
        return LocalDate.ofEpochDay(this.toEpochDay() + daysToAdd);
    }

    public LocalDate minusDays(long daysToSubtract) {
        return this.plusDays(-daysToSubtract);
    }

    public LocalDate plusWeeks(long weeksToAdd) {
        return this.plusDays(weeksToAdd * 7L);
    }

    public LocalDate minusWeeks(long weeksToSubtract) {
        return this.plusDays(-weeksToSubtract * 7L);
    }

    public LocalDate plusMonths(long monthsToAdd) {
        long monthCount = this.year * 12L + (this.month - 1);
        long calcMonths = monthCount + monthsToAdd;
        int newYear = (int) LocalDate.floorDiv(calcMonths, 12);
        int newMonth = (int) LocalDate.floorMod(calcMonths, 12) + 1;
        int newDay = this.day;
        int monthLen = Month.of(newMonth).length(LocalDate.isLeapYear(newYear));
        if (newDay > monthLen) {
            newDay = monthLen;
        }
        return new LocalDate(newYear, newMonth, newDay);
    }

    public LocalDate minusMonths(long monthsToSubtract) {
        return this.plusMonths(-monthsToSubtract);
    }

    public LocalDate plusYears(long yearsToAdd) {
        return this.plusMonths(yearsToAdd * 12L);
    }

    public LocalDate minusYears(long yearsToSubtract) {
        return this.plusMonths(-yearsToSubtract * 12L);
    }

    // --- comparison ---

    /**
     * The natural order. The signature takes {@link ChronoLocalDate} and not {@code LocalDate}
     * because that is what the interface declares -- and what the JDK declares: a class cannot
     * implement {@code Comparable} twice with different parameters.
     *
     * <p>Against another {@code LocalDate} it compares field by field, which is cheaper than going
     * to the epoch day. Against a date of another calendar it falls back to the general order: epoch
     * day and, on a tie, the chronology's id -- the tie-break that keeps two dates that are not
     * equal from comparing 0.
     */
    @Override
    public int compareTo(ChronoLocalDate other) {
        if (other instanceof LocalDate) {
            LocalDate that = (LocalDate) other;
            if (this.year != that.year) {
                return this.year - that.year;
            }
            if (this.month != that.month) {
                return this.month - that.month;
            }
            return this.day - that.day;
        }
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

    /**
     * Whether this date is before `other`, which may belong to **another calendar**.
     *
     * <p>It compares by epoch day and not by year/month/day: it is the only way for a comparison
     * across calendars to mean anything. A Japanese 1st of January and an ISO one are the same day
     * if they fall at the same point on the line, however each of them numbers it.
     */
    public boolean isBefore(java.time.chrono.ChronoLocalDate other) {
        return this.toEpochDay() < other.toEpochDay();
    }

    public boolean isAfter(java.time.chrono.ChronoLocalDate other) {
        return this.toEpochDay() > other.toEpochDay();
    }

    /**
     * Whether they name the **same day**, even if they belong to different calendars.
     *
     * <p>Unlike `equals`, which also demands the same calendar. It is the difference between "it is
     * the same day" and "it is the same date".
     */
    public boolean isEqual(java.time.chrono.ChronoLocalDate other) {
        return this.toEpochDay() == other.toEpochDay();
    }

    /** The ISO era: `CE` for the positive years, `BCE` for the rest. */
    public java.time.chrono.IsoEra getEra() {
        return this.getYear() >= 1 ? java.time.chrono.IsoEra.CE : java.time.chrono.IsoEra.BCE;
    }

    /** This date at midnight. */
    public LocalDateTime atStartOfDay() {
        return LocalDateTime.of(this, LocalTime.MIDNIGHT);
    }

    /**
     * This date at the start of the day in that zone.
     *
     * <p>**It is not always midnight**: on the days daylight saving begins, 00:00 may not exist, and
     * the start of the day is the first time that does. That is why this method is not
     * `atStartOfDay().atZone(zone)`.
     */
    public ZonedDateTime atStartOfDay(ZoneId zone) {
        if (zone == null) {
            throw new NullPointerException("zone");
        }
        return ZonedDateTime.of(this.atStartOfDay(), zone);
    }

    /** This date with that time and that offset. */
    public java.time.OffsetDateTime atTime(java.time.OffsetTime time) {
        if (time == null) {
            throw new NullPointerException("time");
        }
        return java.time.OffsetDateTime.of(LocalDateTime.of(this, time.toLocalTime()),
                time.getOffset());
    }

    /** The period between this date and `endDateExclusive`, in years, months and days. */
    public Period until(java.time.chrono.ChronoLocalDate endDateExclusive) {
        if (endDateExclusive == null) {
            throw new NullPointerException("endDateExclusive");
        }
        return Period.between(this, LocalDate.ofEpochDay(endDateExclusive.toEpochDay()));
    }

    /** The seconds since the epoch of this date at that time and with that offset. */
    public long toEpochSecond(LocalTime time, ZoneOffset offset) {
        if (time == null || offset == null) {
            throw new NullPointerException();
        }
        return this.toEpochDay() * 86400L + time.toSecondOfDay() - offset.getTotalSeconds();
    }

    /**
     * The dates from this one (inclusive) to `endExclusive`, one day at a time.
     *
     * <p>The stream is **eager** in this library --it is materialised whole-- so a huge range costs
     * memory. With an empty or inverted range it returns an empty stream, which is what the JDK
     * does.
     */
    public java.util.stream.Stream<LocalDate> datesUntil(LocalDate endExclusive) {
        return this.datesUntil(endExclusive, Period.ofDays(1));
    }

    /**
     * The same, advancing by `step`.
     *
     * @throws IllegalArgumentException if the step is zero, or its sign does not lead towards the
     *     end
     */
    public java.util.stream.Stream<LocalDate> datesUntil(LocalDate endExclusive, Period step) {
        if (endExclusive == null || step == null) {
            throw new NullPointerException();
        }
        if (step.isZero()) {
            throw new IllegalArgumentException("step is zero");
        }
        boolean forwards = !step.isNegative();
        java.util.List<LocalDate> out = new java.util.ArrayList<LocalDate>();
        LocalDate current = this;
        // The step's sign has to lead towards the end; otherwise the loop would never finish.
        if (forwards && this.toEpochDay() < endExclusive.toEpochDay()) {
            while (current.toEpochDay() < endExclusive.toEpochDay()) {
                out.add(current);
                current = current.plus(step);
            }
        } else if (!forwards && this.toEpochDay() > endExclusive.toEpochDay()) {
            while (current.toEpochDay() > endExclusive.toEpochDay()) {
                out.add(current);
                current = current.plus(step);
            }
        }
        Object[] a = new Object[out.size()];
        int i = 0;
        while (i < out.size()) {
            a[i] = out.get(i);
            i = i + 1;
        }
        return (java.util.stream.Stream<LocalDate>) java.util.stream.Stream.of(a);
    }

    public boolean isBefore(LocalDate other) {
        return this.compareTo(other) < 0;
    }

    public boolean isAfter(LocalDate other) {
        return this.compareTo(other) > 0;
    }

    // --- Temporal ---

    /**
     * The fields a date has: **all** the date ones.
     *
     * <p>It asks about the category instead of enumerating six names. The list drifted out of step
     * with `getLong` --and in fact was: `ERA` and `PROLEPTIC_MONTH` said no-- whereas the category
     * cannot.
     */
    public boolean isSupported(TemporalField field) {
        if (field instanceof ChronoField) {
            return ((ChronoField) field).isDateBased();
        }
        return field != null && field.isSupportedBy(this);
    }

    /**
     * That field's value.
     *
     * <p>Three of them are the state --year, month, day-- and the rest are **derived**. They are here
     * because they are exact functions of those three, with no decision to take: a formatter with `G`
     * or with `yyyy` in a calendar with eras needs `ERA` and `YEAR_OF_ERA`, and used to meet a
     * rejection.
     *
     * <p>The `ALIGNED_*` family is the only one that asks for an explanation. They align the weeks to
     * **day 1** of the month or of the year instead of to Monday: day 1 always begins week 1, the 8th
     * week 2, and so on. That is why they are `(day - 1) / 7 + 1` and `(day - 1) % 7 + 1` and do not
     * depend on what weekday anything fell on.
     *
     * <p>`ERA` is 1 for the dates of positive year and 0 for the rest, and `YEAR_OF_ERA` counts
     * backwards inside the earlier era --proleptic year 0 is 1 BC-- which is what makes the two of
     * them together reconstruct the year.
     */
    public long getLong(TemporalField field) {
        if (field == ChronoField.DAY_OF_MONTH) {
            return (long) this.day;
        }
        if (field == ChronoField.MONTH_OF_YEAR) {
            return (long) this.month;
        }
        if (field == ChronoField.YEAR) {
            return (long) this.year;
        }
        if (field == ChronoField.EPOCH_DAY) {
            return this.toEpochDay();
        }
        if (field == ChronoField.DAY_OF_WEEK) {
            return (long) this.getDayOfWeek().getValue();
        }
        if (field == ChronoField.DAY_OF_YEAR) {
            return (long) this.getDayOfYear();
        }
        if (field == ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH) {
            return (long) ((this.day - 1) % 7 + 1);
        }
        if (field == ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR) {
            return (long) ((this.getDayOfYear() - 1) % 7 + 1);
        }
        if (field == ChronoField.ALIGNED_WEEK_OF_MONTH) {
            return (long) ((this.day - 1) / 7 + 1);
        }
        if (field == ChronoField.ALIGNED_WEEK_OF_YEAR) {
            return (long) ((this.getDayOfYear() - 1) / 7 + 1);
        }
        if (field == ChronoField.PROLEPTIC_MONTH) {
            return (long) this.year * 12L + (long) (this.month - 1);
        }
        if (field == ChronoField.YEAR_OF_ERA) {
            return (long) (this.year >= 1 ? this.year : 1 - this.year);
        }
        if (field == ChronoField.ERA) {
            return (long) (this.year >= 1 ? 1 : 0);
        }
        if (field != null && !(field instanceof ChronoField)) {
            // A third party's field knows how to read itself.
            return field.getFrom(this);
        }
        throw new java.time.temporal.UnsupportedTemporalTypeException("Unsupported field: " + field);
    }

    public boolean isSupported(TemporalUnit unit) {
        return unit == ChronoUnit.DAYS || unit == ChronoUnit.WEEKS
            || unit == ChronoUnit.MONTHS || unit == ChronoUnit.YEARS;
    }

    // The return narrowed to `LocalDate`, as in the JDK (a covariant override, §8.4.8.3).
    public LocalDate with(TemporalField field, long newValue) {
        if (field == ChronoField.DAY_OF_MONTH) {
            return new LocalDate(this.year, this.month, (int) newValue);
        }
        if (field == ChronoField.MONTH_OF_YEAR) {
            return new LocalDate(this.year, (int) newValue, this.day);
        }
        if (field == ChronoField.YEAR) {
            return new LocalDate((int) newValue, this.month, this.day);
        }
        if (field == ChronoField.EPOCH_DAY) {
            return LocalDate.ofEpochDay(newValue);
        }
        throw new java.time.temporal.UnsupportedTemporalTypeException("Unsupported field: " + field);
    }

    public LocalDate plus(long amountToAdd, TemporalUnit unit) {
        if (unit == ChronoUnit.DAYS) {
            return this.plusDays(amountToAdd);
        }
        if (unit == ChronoUnit.WEEKS) {
            return this.plusWeeks(amountToAdd);
        }
        if (unit == ChronoUnit.MONTHS) {
            return this.plusMonths(amountToAdd);
        }
        if (unit == ChronoUnit.YEARS) {
            return this.plusYears(amountToAdd);
        }
        throw new java.time.temporal.UnsupportedTemporalTypeException("Unsupported unit: " + unit);
    }

    public LocalDate minus(long amountToSubtract, TemporalUnit unit) {
        return this.plus(-amountToSubtract, unit);
    }

    public long until(Temporal endExclusive, TemporalUnit unit) {
        LocalDate end = (LocalDate) endExclusive;
        long daysDiff = end.toEpochDay() - this.toEpochDay();
        if (unit == ChronoUnit.DAYS) {
            return daysDiff;
        }
        if (unit == ChronoUnit.WEEKS) {
            return daysDiff / 7L;
        }
        throw new java.time.temporal.UnsupportedTemporalTypeException("Unsupported unit: " + unit);
    }

    // --- TemporalAdjuster ---

    public Temporal adjustInto(Temporal temporal) {
        return temporal.with(ChronoField.EPOCH_DAY, this.toEpochDay());
    }

    // --- floor division/modulo (no java.lang.Math.floorDiv/floorMod in KajiLibrary yet) ---

    private static long floorDiv(long a, long b) {
        long q = a / b;
        if ((a % b != 0) && ((a ^ b) < 0)) {
            q = q - 1;
        }
        return q;
    }

    private static long floorMod(long a, long b) {
        return a - LocalDate.floorDiv(a, b) * b;
    }

    // --- value-type methods (ISO-8601) ---

    // ISO-8601: uuuu-MM-dd, with the year padded to at least 4 digits (a '+' prefix past 9999,
    // '-' when negative) — the same layout java.time uses.
    public String toString() {
        StringBuilder buf = new StringBuilder();
        int absYear;
        if (this.year < 0) {
            absYear = -this.year;
        } else {
            absYear = this.year;
        }
        if (absYear < 1000) {
            if (this.year < 0) {
                String t = Integer.toString(this.year - 10000);
                buf.append("-");
                buf.append(t.substring(2, t.length()));
            } else {
                String t = Integer.toString(this.year + 10000);
                buf.append(t.substring(1, t.length()));
            }
        } else {
            if (this.year > 9999) {
                buf.append("+");
            }
            buf.append(Integer.toString(this.year));
        }
        if (this.month < 10) {
            buf.append("-0");
        } else {
            buf.append("-");
        }
        buf.append(Integer.toString(this.month));
        if (this.day < 10) {
            buf.append("-0");
        } else {
            buf.append("-");
        }
        buf.append(Integer.toString(this.day));
        return buf.toString();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof LocalDate) {
            LocalDate o = (LocalDate) obj;
            return this.year == o.year && this.month == o.month && this.day == o.day;
        }
        return false;
    }

    public int hashCode() {
        int y = this.year;
        return (y & 0xFFFFF800) ^ ((y << 11) + (this.month << 6) + this.day);
    }

    public LocalDate withYear(int year) {
        return LocalDate.of(year, this.month, this.day);
    }

    public LocalDate withMonth(int month) {
        return LocalDate.of(this.year, month, this.day);
    }

    public LocalDate withDayOfMonth(int dayOfMonth) {
        return LocalDate.of(this.year, this.month, dayOfMonth);
    }

    public LocalDate withDayOfYear(int dayOfYear) {
        return LocalDate.ofEpochDay(LocalDate.of(this.year, 1, 1).toEpochDay() + dayOfYear - 1);
    }

    // --- generic Temporal-typed conveniences ---

    public int get(TemporalField field) {
        return (int) this.getLong(field);
    }

    public LocalDate plus(TemporalAmount amount) {
        return (LocalDate) amount.addTo(this);
    }

    public LocalDate minus(TemporalAmount amount) {
        return (LocalDate) amount.subtractFrom(this);
    }

    public LocalDate with(TemporalAdjuster adjuster) {
        return (LocalDate) adjuster.adjustInto(this);
    }

    // Combines this date with a time to make a LocalDateTime.
    public LocalDateTime atTime(LocalTime time) {
        return LocalDateTime.of(this, time);
    }

    public LocalDateTime atTime(int hour, int minute) {
        return LocalDateTime.of(this, LocalTime.of(hour, minute));
    }

    public LocalDateTime atTime(int hour, int minute, int second) {
        return LocalDateTime.of(this, LocalTime.of(hour, minute, second));
    }

    public LocalDateTime atTime(int hour, int minute, int second, int nanoOfSecond) {
        return LocalDateTime.of(this, LocalTime.of(hour, minute, second, nanoOfSecond));
    }

    public boolean isEqual(LocalDate other) {
        return this.compareTo(other) == 0;
    }

    // The Period between this date (inclusive) and `end` (exclusive), the java.time algorithm:
    // whole months first, then the remaining days, normalising the sign.
    public Period until(LocalDate end) {
        long totalMonths = (end.year * 12L + (end.month - 1)) - (this.year * 12L + (this.month - 1));
        int days = end.day - this.day;
        if (totalMonths > 0 && days < 0) {
            totalMonths = totalMonths - 1;
            LocalDate calcDate = this.plusMonths(totalMonths);
            days = (int) (end.toEpochDay() - calcDate.toEpochDay());
        } else if (totalMonths < 0 && days > 0) {
            totalMonths = totalMonths + 1;
            days = days - end.lengthOfMonth();
        }
        long years = totalMonths / 12;
        int months = (int) (totalMonths % 12);
        return Period.of((int) years, months, days);
    }

    // Parses an ISO-8601 date (uuuu-MM-dd, the year optionally signed and wider than 4 digits).
    public static LocalDate parse(CharSequence text) {
        String s = text.toString();
        int i = 0;
        int sign = 1;
        char c0 = s.charAt(0);
        if (c0 == '+') {
            i = 1;
        } else if (c0 == '-') {
            sign = -1;
            i = 1;
        }
        int yStart = i;
        while (i < s.length() && s.charAt(i) != '-') {
            i = i + 1;
        }
        int year = sign * parseDigits(s, yStart, i);
        i = i + 1;
        int month = parseDigits(s, i, i + 2);
        i = i + 3;
        int day = parseDigits(s, i, i + 2);
        return LocalDate.of(year, month, day);
    }

    public String format(DateTimeFormatter formatter) {
        return formatter.format(this);
    }

    public IsoChronology getChronology() {
        return IsoChronology.INSTANCE;
    }

    private static int parseDigits(String s, int from, int to) {
        int v = 0;
        for (int k = from; k < to; k = k + 1) {
            v = v * 10 + (s.charAt(k) - '0');
        }
        return v;
    }

    /**
     * It reads `text` with that formatter.
     *
     * <p>The one that decides which fields are there is the formatter; this class only says **which
     * of them it wants**, by passing its own `from`. That is why a pattern that brings no date fails
     * here and not when the result is used.
     *
     * @throws java.time.format.DateTimeParseException if the text does not fit the pattern, or what
     *     fits is not enough for a date
     */
    public static LocalDate parse(CharSequence text, java.time.format.DateTimeFormatter formatter) {
        if (formatter == null) {
            throw new NullPointerException("formatter");
        }
        // Bound to a local: chaining through an interface-typed intermediate gets lost (#108).
        java.time.temporal.TemporalQuery<LocalDate> queryOf = LocalDate::from;
        return formatter.parse(text, queryOf);
    }
}
