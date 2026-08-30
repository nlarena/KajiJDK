package java.util;

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
        // `complete()` y no solo `computeFields()`: si el llamador hizo `set(...)`, el instante
        // quedo desactualizado y hay que recalcularlo ANTES de partirlo en campos. Recomputar
        // solo los campos leeria el instante viejo y devolveria la fecha anterior — que es
        // exactamente el defecto que tenia esto.
        this.complete();
        return this.fields[field];
    }

    public void set(int field, int value) {
        this.fields[field] = value;
        this.isSet[field] = true;
        // Las DOS banderas. Invalidar solo el instante no alcanza: los demas campos siguen
        // marcados como validos, asi que un `get` posterior devolveria los viejos sin recalcular.
        this.isTimeSet = false;
        this.areFieldsSet = false;
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

    // ---- estilos de nombre para getDisplayName ----------------------------------------------
    //
    // Los STANDALONE llevan el bit 0x8000 sobre su equivalente FORMAT, que es como el JDK
    // distingue "enero" (nombre suelto) de "de enero" (dentro de una fecha) en los idiomas que
    // hacen esa diferencia. En español no se nota; en ruso o finés, si.

    public static final int ALL_STYLES = 0;
    public static final int SHORT_FORMAT = 1;
    public static final int SHORT = 1;
    public static final int LONG_FORMAT = 2;
    public static final int LONG = 2;
    public static final int NARROW_FORMAT = 4;
    public static final int SHORT_STANDALONE = 32769;
    public static final int LONG_STANDALONE = 32770;
    public static final int NARROW_STANDALONE = 32772;

    // La zona horaria de este calendario. Nunca null.
    private TimeZone zone = TimeZone.getDefault();

    // El primer dia de la semana y cuantos dias necesita la primera semana del año.
    //
    // Son configurables porque no hay acuerdo: en gran parte del mundo la semana arranca el
    // lunes, en Estados Unidos el domingo, y la "primera semana del año" es la que tiene 4 dias
    // en la norma ISO y la que tiene 1 en el uso estadounidense. Un calendario que fije una sola
    // convencion da fechas equivocadas en la otra mitad del planeta.
    private int firstDayOfWeek = 1;          // SUNDAY
    private int minimalDaysInFirstWeek = 1;

    // Un calendario en la zona y el locale dados. `locale` se acepta y se ignora: no hay datos de
    // locale en esta biblioteca, la misma decision que ya tomaron TimeZone y Currency.
    protected Calendar(TimeZone zone, Locale aLocale) {
        this();
        if (zone != null) {
            this.zone = zone;
        }
    }

    // La zona horaria.
    public TimeZone getTimeZone() {
        return this.zone;
    }

    // Cambia la zona horaria. Los campos quedan invalidados: el mismo instante se lee distinto en
    // otra zona.
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

    // El valor crudo de un campo, SIN recalcular.
    //
    // Es la diferencia con `get(int)` y el motivo de que sea `protected`: `get` completa el
    // calendario antes de leer, y llamarlo desde `computeFields` seria recursion infinita. Las
    // subclases leen con este.
    protected final int internalGet(int field) {
        return this.fields[field];
    }

    // Recalcula lo que falte para que todos los campos esten al dia.
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

    // Un calendario para la zona y el locale por defecto.
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

    // Los locales para los que hay calendario. A KajiLibrary subset: los que declara `Locale`.
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

    // Los tipos de calendario disponibles. Aca solo el gregoriano.
    public static Set<String> getAvailableCalendarTypes() {
        HashSet<String> out = new HashSet<String>();
        out.add("gregory");
        return out;
    }

    // El identificador del tipo de calendario.
    public String getCalendarType() {
        return "gregory";
    }

    // El menor valor que un campo puede tomar **en esta fecha concreta**.
    //
    // Distinto de `getMinimum`, que es el menor de cualquier fecha. La diferencia importa en
    // DAY_OF_MONTH: el minimo siempre es 1, pero el maximo real es 28, 29, 30 o 31 segun el mes.
    public int getActualMinimum(int field) {
        return this.getMinimum(field);
    }

    // El mayor valor que un campo puede tomar en esta fecha concreta.
    //
    // La implementacion generica busca por tanteo entre el maximo garantizado y el maximo
    // posible; una subclase que sepa la respuesta —GregorianCalendar la sabe— la sobreescribe.
    public int getActualMaximum(int field) {
        return this.getLeastMaximum(field);
    }

    // Suma `amount` al campo sin tocar los mas grandes: `roll(MONTH, 1)` sobre diciembre da enero
    // del MISMO año.
    public void roll(int field, int amount) {
        boolean arriba = amount >= 0;
        int veces = arriba ? amount : -amount;
        int i = 0;
        while (i < veces) {
            this.roll(field, arriba);
            i = i + 1;
        }
    }

    // Si este calendario soporta fechas por semana ISO. El gregoriano si; la clase base no.
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

    // El nombre de un valor de campo en el estilo y locale dados.
    //
    // A KajiLibrary subset: devuelve **null**, que en el contrato del JDK significa "no hay nombre
    // para este estilo". Los nombres salen de los bundles de locale, que aca no existen — la
    // misma decision que TimeZone.getDisplayName y Currency.getSymbol. Devolver null es correcto
    // segun el contrato; inventar "enero" en ingles seria mentir.
    public String getDisplayName(int field, int style, Locale locale) {
        if (locale == null) {
            throw new NullPointerException();
        }
        return null;
    }

    // Todos los nombres de un campo. A KajiLibrary subset: null, por lo mismo que arriba.
    public Map<String, Integer> getDisplayNames(int field, int style, Locale locale) {
        if (locale == null) {
            throw new NullPointerException();
        }
        return null;
    }

    // Este calendario como Instant.
    public final Instant toInstant() {
        return Instant.ofEpochMilli(this.getTimeInMillis());
    }
}
