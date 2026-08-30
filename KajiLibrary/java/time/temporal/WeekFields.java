package java.time.temporal;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.util.Locale;

// KajiLibrary's java.time.temporal.WeekFields — week numbering is NOT universal, and this is the
// class that admits it. Two parameters decide everything:
//
//   firstDayOfWeek          Monday in ISO-8601, Sunday in the US
//   minimalDaysInFirstWeek  how much of a week must fall in the new year for it to count as week 1
//                           (4 in ISO — i.e. the majority; 1 in the US — i.e. any part at all)
//
// With (Monday, 4) these fields reproduce IsoFields exactly. With (Sunday, 1), January 1st is
// always in week 1. Same calendar, different answers, both correct.
//
// The JDK models the fields as constants of a private inner class; ours are package-private
// top-level classes in this file, because a nested type doesn't resolve (finding #101).
public final class WeekFields implements Serializable {

    // `DayOfWeek.MONDAY` would be a cross-class read of a static field, which our compiler emits as
    // `getfield` and the VM cannot resolve (finding #110). The `of(int)` factory is a static
    // *method* call, which does work — same value, and the class can actually initialize.
    public static final WeekFields ISO = new WeekFields(DayOfWeek.of(1), 4);

    public static final WeekFields SUNDAY_START = new WeekFields(DayOfWeek.of(7), 1);

    // The same unit IsoFields exposes: a week-based year is week-based regardless of locale.
    // Built fresh instead of read off IsoFields for the #110 reason above.
    public static final TemporalUnit WEEK_BASED_YEARS = new IsoUnit(0, "WeekBasedYears");

    private final DayOfWeek firstDayOfWeek;
    private final int minimalDays;

    private WeekFields(DayOfWeek firstDayOfWeek, int minimalDaysInFirstWeek) {
        this.firstDayOfWeek = firstDayOfWeek;
        this.minimalDays = minimalDaysInFirstWeek;
    }

    public static WeekFields of(DayOfWeek firstDayOfWeek, int minimalDaysInFirstWeek) {
        if (minimalDaysInFirstWeek < 1 || minimalDaysInFirstWeek > 7) {
            throw new IllegalArgumentException("Minimal number of days is invalid");
        }
        return new WeekFields(firstDayOfWeek, minimalDaysInFirstWeek);
    }

    // A KajiLibrary subset: the JDK reads the first-day-of-week and minimal-days from the CLDR
    // locale data. We carry only the split that actually changes the answer — the countries whose
    // week starts on Sunday with a one-day minimum — and treat everything else as ISO. Widening
    // this needs the CLDR tables, the same data wall as tzdb.
    public static WeekFields of(Locale locale) {
        String country = locale.getCountry();
        WeekFields result = WeekFields.ISO;
        if (WeekFields.isSundayStart(country)) {
            result = WeekFields.SUNDAY_START;
        }
        return result;
    }

    private static boolean isSundayStart(String country) {
        boolean sunday = false;
        if (country.equals("US") || country.equals("CA") || country.equals("JP")
                || country.equals("BR") || country.equals("IL") || country.equals("MX")
                || country.equals("PH") || country.equals("KR") || country.equals("TW")
                || country.equals("ZA")) {
            sunday = true;
        }
        return sunday;
    }

    public DayOfWeek getFirstDayOfWeek() {
        return this.firstDayOfWeek;
    }

    public int getMinimalDaysInFirstWeek() {
        return this.minimalDays;
    }

    public TemporalField dayOfWeek() {
        return new ComputedField(0, "DayOfWeek", this.firstDayOfWeek.getValue(), this.minimalDays);
    }

    public TemporalField weekOfMonth() {
        return new ComputedField(1, "WeekOfMonth", this.firstDayOfWeek.getValue(), this.minimalDays);
    }

    public TemporalField weekOfYear() {
        return new ComputedField(2, "WeekOfYear", this.firstDayOfWeek.getValue(), this.minimalDays);
    }

    public TemporalField weekOfWeekBasedYear() {
        return new ComputedField(3, "WeekOfWeekBasedYear", this.firstDayOfWeek.getValue(), this.minimalDays);
    }

    public TemporalField weekBasedYear() {
        return new ComputedField(4, "WeekBasedYear", this.firstDayOfWeek.getValue(), this.minimalDays);
    }

    public boolean equals(Object object) {
        boolean same = false;
        if (object instanceof WeekFields) {
            WeekFields other = (WeekFields) object;
            same = this.hashCode() == other.hashCode();
        }
        return same;
    }

    public int hashCode() {
        // The JDK's exact formula: the pair (firstDayOfWeek, minimalDays) determines the instance.
        return (this.firstDayOfWeek.getValue() - 1) * 7 + this.minimalDays;
    }

    public String toString() {
        return "WeekFields[" + this.firstDayOfWeek + "," + this.minimalDays + "]";
    }
}

// One localized field. `kind`: 0 = day-of-week, 1 = week-of-month, 2 = week-of-year,
// 3 = week-of-week-based-year, 4 = week-based-year.
//
// The config is copied in as plain ints rather than held as a WeekFields reference: it keeps every
// read one hop away, with no chained call through an intermediate (the shape of finding #108).
final class ComputedField implements TemporalField {

    private final int kind;
    private final String name;
    private final int firstDow;
    private final int minimalDays;

    ComputedField(int kind, String name, int firstDow, int minimalDays) {
        this.kind = kind;
        this.name = name;
        this.firstDow = firstDow;
        this.minimalDays = minimalDays;
    }

    public long getFrom(TemporalAccessor temporal) {
        long result = 0L;
        int dow = this.localizedDayOfWeek(temporal);
        if (this.kind == 0) {
            result = (long) dow;
        } else if (this.kind == 1) {
            int dom = temporal.get(ChronoField.DAY_OF_MONTH);
            result = (long) WeekMath.computeWeek(WeekMath.startOfWeekOffset(dom, dow, this.minimalDays), dom);
        } else if (this.kind == 2) {
            int doy = temporal.get(ChronoField.DAY_OF_YEAR);
            result = (long) WeekMath.computeWeek(WeekMath.startOfWeekOffset(doy, dow, this.minimalDays), doy);
        } else if (this.kind == 3) {
            result = (long) this.weekOfWeekBasedYear(temporal, dow);
        } else {
            result = (long) this.weekBasedYear(temporal, dow);
        }
        return result;
    }

    // Las cuatro descripciones que `TemporalField` pide. Los cinco campos localizados cuentan dias o
    // semanas, dentro de la unidad que su nombre dice.

    public TemporalUnit getBaseUnit() {
        if (this.kind == 0) {
            return ChronoUnit.DAYS;
        }
        if (this.kind == 4) {
            return IsoFields.WEEK_BASED_YEARS;
        }
        return ChronoUnit.WEEKS;
    }

    public TemporalUnit getRangeUnit() {
        if (this.kind == 0) {
            return ChronoUnit.WEEKS;
        }
        if (this.kind == 1) {
            return ChronoUnit.MONTHS;
        }
        if (this.kind == 2) {
            return ChronoUnit.YEARS;
        }
        if (this.kind == 3) {
            return IsoFields.WEEK_BASED_YEARS;
        }
        return ChronoUnit.FOREVER;
    }

    public ValueRange range() {
        if (this.kind == 0) {
            return ValueRange.of(1L, 7L);
        }
        if (this.kind == 1) {
            // La semana 0 existe: los dias iniciales que no llegan al minimo caen en ella.
            return ValueRange.of(0L, 1L, 4L, 6L);
        }
        if (this.kind == 2) {
            return ValueRange.of(0L, 1L, 52L, 54L);
        }
        if (this.kind == 3) {
            return ValueRange.of(1L, 52L, 53L);
        }
        return ChronoField.YEAR.range();
    }

    public ValueRange rangeRefinedBy(TemporalAccessor temporal) {
        if (!this.isSupportedBy(temporal)) {
            throw new UnsupportedTemporalTypeException("Unsupported field: " + this.name);
        }
        return this.range();
    }

    public <R extends Temporal> R adjustInto(R temporal, long newValue) {
        // Por diferencia, como los campos ISO: se calcula cuanto hay que moverse en la unidad base.
        // Poner el campo directamente pediria reconstruir la fecha desde el calendario localizado de
        // semanas, que depende del primer dia y del minimo de dias -- una cuenta aparte.
        long actual = this.getFrom(temporal);
        this.range().checkValidValue(newValue, this);
        return (R) temporal.plus(newValue - actual, this.getBaseUnit());
    }

    // The day of the week counted from this locale's first day: 1 = firstDayOfWeek.
    private int localizedDayOfWeek(TemporalAccessor temporal) {
        int iso = temporal.get(ChronoField.DAY_OF_WEEK);
        return WeekMath.floorMod7(iso - this.firstDow) + 1;
    }

    // Week 0 means the date falls in the tail week of the previous year, which in that year's own
    // numbering is its last week — so it is recomputed as if the year were longer by this year's
    // day-of-year. Weeks past 50 may in turn be week 1 of the NEXT year, which happens exactly when
    // the week reaches the week number that the next January 1st would start at.
    private int weekOfWeekBasedYear(TemporalAccessor temporal, int dow) {
        int doy = temporal.get(ChronoField.DAY_OF_YEAR);
        int offset = WeekMath.startOfWeekOffset(doy, dow, this.minimalDays);
        int week = WeekMath.computeWeek(offset, doy);
        int result = week;
        if (week == 0) {
            long year = temporal.getLong(ChronoField.YEAR);
            int prevLen = WeekMath.lengthOfYear(year - 1L);
            int prevDoy = doy + prevLen;
            int prevOffset = WeekMath.startOfWeekOffset(prevDoy, dow, this.minimalDays);
            result = WeekMath.computeWeek(prevOffset, prevDoy);
        } else if (week > 50) {
            long year = temporal.getLong(ChronoField.YEAR);
            int yearLen = WeekMath.lengthOfYear(year);
            int newYearWeek = WeekMath.computeWeek(offset, yearLen + this.minimalDays);
            if (week >= newYearWeek) {
                result = week - newYearWeek + 1;
            }
        }
        return result;
    }

    private int weekBasedYear(TemporalAccessor temporal, int dow) {
        long year = temporal.getLong(ChronoField.YEAR);
        int doy = temporal.get(ChronoField.DAY_OF_YEAR);
        int offset = WeekMath.startOfWeekOffset(doy, dow, this.minimalDays);
        int week = WeekMath.computeWeek(offset, doy);
        long result = year;
        if (week == 0) {
            result = year - 1L;
        } else if (week > 50) {
            int yearLen = WeekMath.lengthOfYear(year);
            int newYearWeek = WeekMath.computeWeek(offset, yearLen + this.minimalDays);
            if (week >= newYearWeek) {
                result = year + 1L;
            }
        }
        return (int) result;
    }

    public boolean isSupportedBy(TemporalAccessor temporal) {
        boolean ok = temporal.isSupported(ChronoField.DAY_OF_WEEK);
        if (ok && this.kind == 1) {
            ok = temporal.isSupported(ChronoField.DAY_OF_MONTH);
        } else if (ok && this.kind >= 2) {
            ok = temporal.isSupported(ChronoField.DAY_OF_YEAR);
        }
        return ok;
    }

    public boolean isDateBased() {
        return true;
    }

    public boolean isTimeBased() {
        return false;
    }

    public String toString() {
        return this.name;
    }
}

// The week arithmetic, shared by every localized field.
final class WeekMath {

    private WeekMath() {
    }

    // `%` truncates toward zero; week offsets need the floor.
    static int floorMod7(int v) {
        int m = v % 7;
        if (m < 0) {
            m = m + 7;
        }
        return m;
    }

    // How far the start of `day`'s week sits from day 1 of the period — negative if that week
    // started inside the period, positive if the first partial week is too short to count (fewer
    // than `minimalDays` days), in which case week 1 starts a week later.
    static int startOfWeekOffset(int day, int dow, int minimalDays) {
        int weekStart = WeekMath.floorMod7(day - dow);
        int offset = -weekStart;
        if (weekStart + 1 > minimalDays) {
            offset = 7 - weekStart;
        }
        return offset;
    }

    static int computeWeek(int offset, int day) {
        return (7 + offset + (day - 1)) / 7;
    }

    static int lengthOfYear(long year) {
        int len = 365;
        if ((year % 4L == 0L) && ((year % 100L != 0L) || (year % 400L == 0L))) {
            len = 366;
        }
        return len;
    }
}
