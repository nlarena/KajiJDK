package java.time.chrono;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.ValueRange;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

// KajiLibrary's java.time.chrono.Chronology -- a calendar system: ISO-8601 and the four the JDK
// brings (Japanese, Hijrah, Minguo, Thai Buddhist). Ordered by id, so it extends
// Comparable<Chronology>.
//
// The split with `ChronoLocalDate` is the JDK's and is worth having clear: the **date** knows what
// day it is, the **calendar** knows how days are counted. A `MinguoDate` does not know how many
// months a year has; it asks its `MinguoChronology`.
//
// On `getDisplayName(TextStyle, Locale)`: it is here, and it returns the **id**. See its javadoc.
public interface Chronology extends Comparable<Chronology> {

    String getId();

    String getCalendarType();

    boolean isLeapYear(long prolepticYear);

    ChronoLocalDate date(int prolepticYear, int month, int dayOfMonth);

    ChronoLocalDate dateEpochDay(long epochDay);

    Era eraOf(int eraValue);

    int compareTo(Chronology other);

    // ---- what every calendar has to know how to answer --------------------------------------------

    /**
     * The **proleptic** year corresponding to that year within that era.
     *
     * <p>A calendar counts the years by era and starts over: Showa 1 and Heisei 1 are two different
     * years. The proleptic year is the running numbering that does not restart, and it is the only
     * one arithmetic can be done with.
     */
    int prolepticYear(Era era, int yearOfEra);

    /** The date `temporal` holds, read in **this** calendar. */
    ChronoLocalDate date(TemporalAccessor temporal);

    /** The date by year and **day of the year**, without going through the month. */
    ChronoLocalDate dateYearDay(int prolepticYear, int dayOfYear);

    /** The range of values that field allows **in this calendar**. */
    ValueRange range(ChronoField field);

    /** This calendar's eras, from the oldest to the most recent. */
    List<Era> eras();

    // ---- construction, with the era spelled out ---------------------------------------------------

    /** The date by era, year of the era, month and day. */
    default ChronoLocalDate date(Era era, int yearOfEra, int month, int dayOfMonth) {
        return this.date(this.prolepticYear(era, yearOfEra), month, dayOfMonth);
    }

    /** The date by era, year of the era and day of the year. */
    default ChronoLocalDate dateYearDay(Era era, int yearOfEra, int dayOfYear) {
        return this.dateYearDay(this.prolepticYear(era, yearOfEra), dayOfYear);
    }

    /** Today, in the system's default zone. */
    default ChronoLocalDate dateNow() {
        return this.dateNow(Clock.systemDefaultZone());
    }

    /** Today in that zone. */
    default ChronoLocalDate dateNow(ZoneId zone) {
        return this.dateNow(Clock.system(zone));
    }

    /** Today **according to that clock**, the form that can be tested with a `Clock.fixed`. */
    default ChronoLocalDate dateNow(Clock clock) {
        if (clock == null) {
            throw new NullPointerException("clock");
        }
        LocalDate today = LocalDate.now(clock);
        return this.dateEpochDay(today.toEpochDay());
    }

    // ---- the composites ---------------------------------------------------------------------------

    /** The date and time `temporal` holds, in this calendar. */
    default ChronoLocalDateTime localDateTime(TemporalAccessor temporal) {
        if (temporal == null) {
            throw new NullPointerException("temporal");
        }
        ChronoLocalDate date = this.date(temporal);
        LocalTime time = LocalTime.from(temporal);
        return ChronoLocalDateTimeImpl.of(date, time);
    }

    /** The date, time and zone `temporal` holds, in this calendar. */
    default ChronoZonedDateTime zonedDateTime(TemporalAccessor temporal) {
        if (temporal == null) {
            throw new NullPointerException("temporal");
        }
        ZoneId zone = ZoneId.from(temporal);
        if (temporal.isSupported(ChronoField.INSTANT_SECONDS)) {
            Instant instant = Instant.ofEpochSecond(temporal.getLong(ChronoField.INSTANT_SECONDS),
                    temporal.getLong(ChronoField.NANO_OF_SECOND));
            return this.zonedDateTime(instant, zone);
        }
        ChronoLocalDateTime local = this.localDateTime(temporal);
        return ChronoZonedDateTimeImpl.of(local, zone);
    }

    /** That instant seen from that zone, in this calendar. */
    default ChronoZonedDateTime zonedDateTime(Instant instant, ZoneId zone) {
        if (instant == null) {
            throw new NullPointerException("instant");
        }
        return ChronoZonedDateTimeImpl.ofInstant(this, instant, zone);
    }

    /** A period of this calendar. Years, months and days are **not** normalised against each other. */
    default ChronoPeriod period(int years, int months, int days) {
        return new ChronoPeriodImpl(this, years, months, days);
    }

    // ---- the epoch second, without building the date ----------------------------------------------

    /**
     * The seconds since the epoch of that date and time **of this calendar** with that offset.
     *
     * <p>It exists so the intermediate object need not be built when the number is all that is
     * wanted: it is the path databases and binary formats take.
     */
    default long epochSecond(int prolepticYear, int month, int dayOfMonth, int hour, int minute,
            int second, ZoneOffset zoneOffset) {
        if (zoneOffset == null) {
            throw new NullPointerException("zoneOffset");
        }
        ChronoField.HOUR_OF_DAY.checkValidValue((long) hour);
        ChronoField.MINUTE_OF_HOUR.checkValidValue((long) minute);
        ChronoField.SECOND_OF_MINUTE.checkValidValue((long) second);
        ChronoLocalDate date = this.date(prolepticYear, month, dayOfMonth);
        long day = date.toEpochDay();
        long seconds = day * 86400L + (long) (hour * 3600 + minute * 60 + second);
        return seconds - (long) zoneOffset.getTotalSeconds();
    }

    /** The same, with the era and the year of the era instead of the proleptic year. */
    default long epochSecond(Era era, int yearOfEra, int month, int dayOfMonth, int hour, int minute,
            int second, ZoneOffset zoneOffset) {
        return this.epochSecond(this.prolepticYear(era, yearOfEra), month, dayOfMonth, hour, minute,
                second, zoneOffset);
    }

    /**
     * Whether this calendar counts the days the same way ISO does.
     *
     * <p>This library's four non-ISO ones are all shifts of ISO --the same day with another year
     * number-- except Hijrah, which has a month table of its own. What it answers is not "it is ISO"
     * but "I can treat its years and months as ISO's".
     */
    default boolean isIsoBased() {
        return false;
    }

    /**
     * It rebuilds a date out of loose fields, resolving what contradicts itself according to
     * `resolverStyle`.
     *
     * <p>It is what parsing uses: a formatter gathers `ERA`, `YEAR_OF_ERA`, `MONTH_OF_YEAR`... and
     * somebody has to decide which combination wins and what to do with a 31st of February.
     */
    ChronoLocalDate resolveDate(Map<TemporalField, Long> fieldValues, java.time.format.ResolverStyle resolverStyle);

    /**
     * This calendar's name to show to somebody.
     *
     * <p>It returns **the id**, which is what the JDK itself falls back to when it finds no name for
     * the style and locale asked for --its implementation ends in
     * `Objects.requireNonNullElseGet(name, () -> chrono.getId())`--.
     *
     * <p>**This library does not carry the CLDR's text data**, so that branch is taken **always**.
     * For ISO it coincides with the JDK (`"ISO"` in both); for the rest it falls a word short:
     * `"Minguo"` here, `"Minguo Calendar"` there.
     *
     * @throws NullPointerException if `style` or `locale` are `null`
     */
    default String getDisplayName(java.time.format.TextStyle style, java.util.Locale locale) {
        if (style == null) {
            throw new NullPointerException("style");
        }
        if (locale == null) {
            throw new NullPointerException("locale");
        }
        return this.getId();
    }

    // ---- lookup ---------------------------------------------------------------------------------

    /** The calendar `temporal` declares; ISO if it declares none. */
    static Chronology from(TemporalAccessor temporal) {
        if (temporal == null) {
            throw new NullPointerException("temporal");
        }
        Chronology chrono = temporal.query(java.time.temporal.TemporalQueries.chronology());
        if (chrono != null) {
            return chrono;
        }
        return IsoChronology.INSTANCE;
    }

    /**
     * The calendar of that id (`"ISO"`, `"Minguo"`...) or of that CLDR type (`"iso8601"`,
     * `"roc"`...).
     *
     * @throws java.time.DateTimeException if there is none by that name
     */
    static Chronology of(String id) {
        if (id == null) {
            throw new NullPointerException("id");
        }
        for (Chronology c : ChronologyTable.ALL) {
            if (id.equals(c.getId()) || id.equals(c.getCalendarType())) {
                return c;
            }
        }
        throw new java.time.DateTimeException("Unknown chronology: " + id);
    }

    /**
     * The calendar that locale asks for through its Unicode `ca` extension, or ISO if it asks for
     * none.
     *
     * <p>Only the explicit extension is looked at --`th-TH-u-ca-buddhist`--. The JDK also has a map
     * of "which calendar each region uses by default", which again is CLDR data and not code;
     * without that map, `Locale.forLanguageTag("th-TH")` gives ISO here and Buddhist in the JDK. That
     * difference, which is visible and written down, is preferred to inventing a partial map that is
     * right some of the time.
     */
    static Chronology ofLocale(java.util.Locale locale) {
        if (locale == null) {
            throw new NullPointerException("locale");
        }
        String ca = locale.getUnicodeLocaleType("ca");
        if (ca == null || "iso8601".equals(ca)) {
            return IsoChronology.INSTANCE;
        }
        return Chronology.of(ca);
    }

    /** Every available calendar. */
    static Set<Chronology> getAvailableChronologies() {
        return new java.util.HashSet<Chronology>(ChronologyTable.ALL);
    }
}

// The list of calendars, kept apart because an interface cannot have private fields and a `public`
// one would be a member the JDK does not have.
final class ChronologyTable {

    // A `List` and not an array: `getAvailableChronologies` returns a copy and `of` only walks it, so
    // nobody can modify it from outside.
    static final List<Chronology> ALL = build();

    private ChronologyTable() {
    }

    private static List<Chronology> build() {
        List<Chronology> all = new ArrayList<Chronology>();
        all.add(IsoChronology.INSTANCE);
        all.add(HijrahChronology.INSTANCE);
        all.add(JapaneseChronology.INSTANCE);
        all.add(MinguoChronology.INSTANCE);
        all.add(ThaiBuddhistChronology.INSTANCE);
        return all;
    }
}
