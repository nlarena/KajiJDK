package java.util;

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
// What it deliberately does NOT have, and why:
//
//   getInstance()          needs TimeZone, Locale AND a concrete GregorianCalendar. None exist.
//                          A getInstance() that returned null would hand every caller an NPE at a
//                          place that has nothing to do with the cause.
//   getTimeZone()/setTimeZone(), getDisplayName(), getWeekYear(), isWeekDateSupported()
//                          need TimeZone / Locale support that is not modelled.
//
// A missing member is a legal subset; a member that lies is not. The same rule as ClassLoader
// (#205) and ProtectionDomain (#267).
public abstract class Calendar implements Comparable<Calendar> {

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
        if (!this.areFieldsSet) {
            this.computeFields();
            this.areFieldsSet = true;
        }
        return this.fields[field];
    }

    public void set(int field, int value) {
        this.fields[field] = value;
        this.isSet[field] = true;
        this.isTimeSet = false;
    }

    public void set(int year, int month, int date) {
        this.set(YEAR, year);
        this.set(MONTH, month);
        this.set(DATE, date);
    }

    public void set(int year, int month, int date, int hourOfDay, int minute) {
        this.set(year, month, date);
        this.set(HOUR_OF_DAY, hourOfDay);
        this.set(MINUTE, minute);
    }

    public void set(int year, int month, int date, int hourOfDay, int minute, int second) {
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
}
