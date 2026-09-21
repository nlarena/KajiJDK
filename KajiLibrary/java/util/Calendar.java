package java.util;

import java.lang.Cloneable;
import java.io.Serializable;
import java.time.Instant;

// KajiLibrary's java.util.Calendar (finding #267).
//
// It exists because the API needs the TYPE: `jakarta.persistence.Query` binds parameters of it in
// three overloads, and without the class the file does not compile.
//
// The shape is the JDK's and not an invention: a `long time` in milliseconds since the epoch, an
// `int[] fields` alongside it, and the four hooks a concrete calendar has to fill in
// (`computeTime`, `computeFields`, `add`, `roll`) plus the four range queries. Keeping that shape
// matters more than keeping methods: it is what lets a subclass written against the JDK's Calendar
// compile here unchanged.
//
// This note used to list as absent: getInstance(), getTimeZone()/setTimeZone(), getDisplayName(),
// getWeekYear() and isWeekDateSupported(), for want of TimeZone, Locale and a concrete
// GregorianCalendar. All three of those exist now and all of those members are declared below.
//
// What survives is narrower and is said at each declaration: getDisplayName and getDisplayNames
// return null, which in the JDK's contract means "there is no name for this style" -- the names come
// out of the locale bundles, and those are not here.
//
// A missing member is a legal subset; a member that lies is not. The same rule as ClassLoader
// (#205) and ProtectionDomain (#267).
public abstract class Calendar implements Comparable<Calendar>, Serializable, Cloneable {

    // --- field numbers (the index into `fields`) ---------------------------------------

    public static final int ERA = 0;
    public static final int YEAR = 1;
    public static final int MONTH = 2;
    public static final int WEEK_OF_YEAR = 3;
    public static final int WEEK_OF_MONTH = 4;
    public static final int DATE = 5;
    /** Same field as {@link #DATE}; both names are the JDK's. */
    public static final int DAY_OF_MONTH = 5;
    public static final int DAY_OF_YEAR = 6;
    public static final int DAY_OF_WEEK = 7;
    public static final int DAY_OF_WEEK_IN_MONTH = 8;
    public static final int AM_PM = 9;
    public static final int HOUR = 10;
    public static final int HOUR_OF_DAY = 11;
    public static final int MINUTE = 12;
    public static final int SECOND = 13;
    public static final int MILLISECOND = 14;
    public static final int ZONE_OFFSET = 15;
    public static final int DST_OFFSET = 16;
    public static final int FIELD_COUNT = 17;

    // --- values a field can take -----------------------------------------------------

    public static final int SUNDAY = 1;
    public static final int MONDAY = 2;
    public static final int TUESDAY = 3;
    public static final int WEDNESDAY = 4;
    public static final int THURSDAY = 5;
    public static final int FRIDAY = 6;
    public static final int SATURDAY = 7;

    public static final int JANUARY = 0;
    public static final int FEBRUARY = 1;
    public static final int MARCH = 2;
    public static final int APRIL = 3;
    public static final int MAY = 4;
    public static final int JUNE = 5;
    public static final int JULY = 6;
    public static final int AUGUST = 7;
    public static final int SEPTEMBER = 8;
    public static final int OCTOBER = 9;
    public static final int NOVEMBER = 10;
    public static final int DECEMBER = 11;
    /** The thirteenth month of a lunisolar year. Zero-length in a Gregorian one. */
    public static final int UNDECIMBER = 12;

    public static final int AM = 0;
    public static final int PM = 1;

    // --- state --------------------------------------------------------------------

    /** The field values. Only meaningful where {@link #isSet} says so. */
    protected int[] fields;

    /** Which entries of {@link #fields} carry a value. */
    protected boolean[] isSet;

    /** The instant, in milliseconds since the epoch. */
    protected long time;

    /** Whether {@link #time} is up to date with {@link #fields}. */
    protected boolean isTimeSet;

    /** Whether {@link #fields} is up to date with {@link #time}. */
    protected boolean areFieldsSet;

    private boolean lenient = true;

    protected Calendar() {
        this.fields = new int[FIELD_COUNT];
        this.isSet = new boolean[FIELD_COUNT];
        this.time = 0L;
        this.isTimeSet = false;
        this.areFieldsSet = false;
    }

    // --- the four hooks a concrete calendar fills in --------------------------------

    /** Recomputes {@link #time} from {@link #fields}. */
    protected abstract void computeTime();

    /** Recomputes {@link #fields} from {@link #time}. */
    protected abstract void computeFields();

    /** Adds {@code amount} to {@code field}, rolling into the larger fields as needed. */
    public abstract void add(int field, int amount);

    /** Adds one to {@code field} WITHOUT touching the larger ones. */
    public abstract void roll(int field, boolean up);

    public abstract int getMinimum(int field);

    public abstract int getMaximum(int field);

    public abstract int getGreatestMinimum(int field);

    public abstract int getLeastMaximum(int field);

    // --- the instant ----------------------------------------------------------------

    public long getTimeInMillis() {
        if (!this.isTimeSet) {
            this.computeTime();
            this.isTimeSet = true;
        }
        return this.time;
    }

    public void setTimeInMillis(long millis) {
        this.time = millis;
        this.isTimeSet = true;
        this.areFieldsSet = false;
    }

    /** The same instant as a {@link Date}. */
    public final Date getTime() {
        return new Date(this.getTimeInMillis());
    }

    public final void setTime(Date date) {
        this.setTimeInMillis(date.getTime());
    }

    // --- the fields -----------------------------------------------------------------

    public int get(int field) {
        // `complete()` and not just `computeFields()`: if the caller did a `set(...)`, the instant
        // is out of date and has to be recomputed BEFORE being split into fields. Recomputing only
        // the fields would read the old instant and return the previous date — which is exactly the
        // defect this had.
        this.complete();
        return this.fields[field];
    }

    public void set(int field, int value) {
        this.fields[field] = value;
        this.isSet[field] = true;
        // BOTH flags. Invalidating only the instant is not enough: the other fields stay marked as
        // valid, so a later `get` would return the old ones without recomputing.
        this.isTimeSet = false;
        this.areFieldsSet = false;
    }

    public final void set(int year, int month, int date) {
        this.set(YEAR, year);
        this.set(MONTH, month);
        this.set(DATE, date);
    }

    public final void set(int year, int month, int date, int hourOfDay, int minute) {
        this.set(year, month, date);
        this.set(HOUR_OF_DAY, hourOfDay);
        this.set(MINUTE, minute);
    }

    public final void set(int year, int month, int date, int hourOfDay, int minute, int second) {
        this.set(year, month, date, hourOfDay, minute);
        this.set(SECOND, second);
    }

    public final boolean isSet(int field) {
        return this.isSet[field];
    }

    public final void clear() {
        int i = 0;
        while (i < FIELD_COUNT) {
            this.fields[i] = 0;
            this.isSet[i] = false;
            i = i + 1;
        }
        this.isTimeSet = false;
        this.areFieldsSet = false;
    }

    public final void clear(int field) {
        this.fields[field] = 0;
        this.isSet[field] = false;
        this.isTimeSet = false;
        this.areFieldsSet = false;
    }

    /**
     * Whether out-of-range field values are normalised instead of rejected ({@code 32 January}
     * becoming {@code 1 February}). The flag is honoured by the subclass that computes, which is
     * where the normalisation happens.
     */
    public void setLenient(boolean lenient) {
        this.lenient = lenient;
    }

    public boolean isLenient() {
        return this.lenient;
    }

    // --- ordering -------------------------------------------------------------------

    public boolean before(Object when) {
        return when instanceof Calendar && this.compareTo((Calendar) when) < 0;
    }

    public boolean after(Object when) {
        return when instanceof Calendar && this.compareTo((Calendar) when) > 0;
    }

    @Override
    public int compareTo(Calendar other) {
        long mine = this.getTimeInMillis();
        long theirs = other.getTimeInMillis();
        if (mine < theirs) {
            return -1;
        }
        if (mine > theirs) {
            return 1;
        }
        return 0;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Calendar)) {
            return false;
        }
        Calendar that = (Calendar) other;
        return this.getTimeInMillis() == that.getTimeInMillis() && this.lenient == that.lenient;
    }

    @Override
    public int hashCode() {
        long millis = this.getTimeInMillis();
        return (int) (millis ^ (millis >>> 32));
    }

    // ---- name styles for getDisplayName ------------------------------------------------------
    //
    // The STANDALONE ones carry the bit 0x8000 over their FORMAT equivalent, which is how the JDK
    // tells "January" (a name on its own) from "of January" (inside a date) in the languages that
    // make that difference. In English it does not show; in Russian or Finnish it does.

    public static final int ALL_STYLES = 0;
    public static final int SHORT_FORMAT = 1;
    public static final int SHORT = 1;
    public static final int LONG_FORMAT = 2;
    public static final int LONG = 2;
    public static final int NARROW_FORMAT = 4;
    public static final int SHORT_STANDALONE = 32769;
    public static final int LONG_STANDALONE = 32770;
    public static final int NARROW_STANDALONE = 32772;

    // This calendar's time zone. Never null.
    private TimeZone zone = TimeZone.getDefault();

    // The first day of the week and how many days the year's first week needs.
    //
    // They are configurable because there is no agreement: in much of the world the week starts on
    // Monday, in the United States on Sunday, and "the year's first week" is the one with 4 days in
    // the ISO standard and the one with 1 in American usage. A calendar that fixes a single
    // convention gives wrong dates in the other half of the planet.
    private int firstDayOfWeek = 1;          // SUNDAY
    private int minimalDaysInFirstWeek = 1;

    // A calendar in the given zone and locale. `locale` is accepted and ignored: there is no locale
    // data in this library, the same decision TimeZone and Currency already took.
    protected Calendar(TimeZone zone, Locale aLocale) {
        this();
        if (zone != null) {
            this.zone = zone;
        }
    }

    // The time zone.
    public TimeZone getTimeZone() {
        return this.zone;
    }

    // It changes the time zone. The fields are invalidated: the same instant reads differently in
    // another zone.
    public void setTimeZone(TimeZone value) {
        if (value == null) {
            throw new NullPointerException();
        }
        this.zone = value;
        this.areFieldsSet = false;
    }

    public void setFirstDayOfWeek(int value) {
        this.firstDayOfWeek = value;
        this.areFieldsSet = false;
    }

    public int getFirstDayOfWeek() {
        return this.firstDayOfWeek;
    }

    public void setMinimalDaysInFirstWeek(int value) {
        this.minimalDaysInFirstWeek = value;
        this.areFieldsSet = false;
    }

    public int getMinimalDaysInFirstWeek() {
        return this.minimalDaysInFirstWeek;
    }

    // A field's raw value, WITHOUT recomputing.
    //
    // It is the difference from `get(int)` and the reason it is `protected`: `get` completes the
    // calendar before reading, and calling it from `computeFields` would be infinite recursion. The
    // subclasses read with this one.
    protected final int internalGet(int field) {
        return this.fields[field];
    }

    // It recomputes whatever is missing so every field is up to date.
    protected void complete() {
        if (!this.isTimeSet) {
            this.computeTime();
            this.isTimeSet = true;
        }
        if (!this.areFieldsSet) {
            this.computeFields();
            this.areFieldsSet = true;
        }
    }

    // A calendar for the default zone and locale.
    public static Calendar getInstance() {
        return new GregorianCalendar(TimeZone.getDefault(), Locale.getDefault());
    }

    public static Calendar getInstance(TimeZone zone) {
        return new GregorianCalendar(zone, Locale.getDefault());
    }

    public static Calendar getInstance(Locale aLocale) {
        return new GregorianCalendar(TimeZone.getDefault(), aLocale);
    }

    public static Calendar getInstance(TimeZone zone, Locale aLocale) {
        return new GregorianCalendar(zone, aLocale);
    }

    // The locales there is a calendar for. A KajiLibrary subset: the ones `Locale` declares.
    public static synchronized Locale[] getAvailableLocales() {
        Locale[] out = new Locale[8];
        out[0] = Locale.ROOT;
        out[1] = Locale.ENGLISH;
        out[2] = Locale.US;
        out[3] = Locale.UK;
        out[4] = Locale.GERMAN;
        out[5] = Locale.GERMANY;
        out[6] = Locale.FRENCH;
        out[7] = Locale.FRANCE;
        return out;
    }

    // The available calendar types. Here only the Gregorian one.
    public static Set<String> getAvailableCalendarTypes() {
        HashSet<String> out = new HashSet<String>();
        out.add("gregory");
        return out;
    }

    // The calendar type's identifier.
    public String getCalendarType() {
        return "gregory";
    }

    // The smallest value a field can take **on this concrete date**.
    //
    // Different from `getMinimum`, which is the smallest over any date. The difference matters for
    // DAY_OF_MONTH: the minimum is always 1, but the inner maximum is 28, 29, 30 or 31 by month.
    public int getActualMinimum(int field) {
        return this.getMinimum(field);
    }

    // The largest value a field can take on this concrete date.
    //
    // The generic implementation searches by trial between the guaranteed maximum and the possible
    // one; a subclass that knows the answer —GregorianCalendar does— overrides it.
    public int getActualMaximum(int field) {
        return this.getLeastMaximum(field);
    }

    // It adds `amount` to the field without touching the larger ones: `roll(MONTH, 1)` on December
    // gives January of the SAME year.
    public void roll(int field, int amount) {
        boolean upwards = amount >= 0;
        int times = upwards ? amount : -amount;
        int i = 0;
        while (i < times) {
            this.roll(field, upwards);
            i = i + 1;
        }
    }

    // Whether this calendar supports ISO week dates. The Gregorian one does; the base class does
    // not.
    public boolean isWeekDateSupported() {
        return false;
    }

    public int getWeekYear() {
        throw new UnsupportedOperationException();
    }

    public void setWeekDate(int weekYear, int weekOfYear, int dayOfWeek) {
        throw new UnsupportedOperationException();
    }

    public int getWeeksInWeekYear() {
        throw new UnsupportedOperationException();
    }

    // The name of a field value in the given style and locale.
    //
    // A KajiLibrary subset: it returns **null**, which in the JDK's contract means "there is no name
    // for this style". The names come out of the locale bundles, which are not here — the same
    // decision as TimeZone.getDisplayName and Currency.getSymbol. Returning null is correct under the
    // contract; inventing "January" would be lying.
    public String getDisplayName(int field, int style, Locale locale) {
        if (locale == null) {
            throw new NullPointerException();
        }
        return null;
    }

    // Every name of a field. A KajiLibrary subset: null, for the same reason as above.
    public Map<String, Integer> getDisplayNames(int field, int style, Locale locale) {
        if (locale == null) {
            throw new NullPointerException();
        }
        return null;
    }

    // This calendar as an Instant.
    public final Instant toInstant() {
        return Instant.ofEpochMilli(this.getTimeInMillis());
    }
}
