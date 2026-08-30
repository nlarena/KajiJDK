package java.time;

import java.time.temporal.Temporal;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAmount;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;

// KajiLibrary's java.time.Year — a year on the ISO calendar, e.g. 2026. Immutable value type.
// Implements Temporal, TemporalAdjuster and Comparable. A KajiLibrary subset.
public final class Year implements Temporal, TemporalAdjuster, Comparable<Year> {

    private final int year;

    private Year(int year) {
        this.year = year;
    }

    /** El año mas temprano representable. */
    public static final int MIN_VALUE = -999999999;

    /** El mas tardio. */
    public static final int MAX_VALUE = 999999999;

    /** El año que `temporal` tiene. */
    public static Year from(java.time.temporal.TemporalAccessor temporal) {
        if (temporal == null) {
            throw new NullPointerException("temporal");
        }
        if (temporal instanceof Year) {
            return (Year) temporal;
        }
        return Year.of(temporal.get(ChronoField.YEAR));
    }

    /** El año que marca `clock`. La forma testeable de `now()`. */
    public static Year now(java.time.Clock clock) {
        if (clock == null) {
            throw new NullPointerException("clock");
        }
        return Year.of(LocalDate.now(clock).getYear());
    }

    /** El año en esa zona, ahora. */
    public static Year now(ZoneId zone) {
        if (zone == null) {
            throw new NullPointerException("zone");
        }
        return Year.of(LocalDate.now(zone).getYear());
    }

    public static Year of(int isoYear) {
        return new Year(isoYear);
    }

    public static Year now() {
        return Year.of(LocalDate.now().getYear());
    }

    public static boolean isLeap(long year) {
        return ((year & 3L) == 0) && ((year % 100L != 0) || (year % 400L == 0));
    }

    public int getValue() {
        return this.year;
    }

    public boolean isLeap() {
        return Year.isLeap(this.year);
    }

    public int length() {
        return this.isLeap() ? 366 : 365;
    }

    /**
     * Si ese dia-y-mes existe en este año.
     *
     * <p>El unico caso en que no es el 29 de febrero de un año comun -- y es exactamente para eso
     * que el metodo existe.
     */
    public boolean isValidMonthDay(MonthDay monthDay) {
        return monthDay != null && monthDay.isValidYear(this.getValue());
    }

    /**
     * El dia numero `dayOfYear` de este año.
     *
     * @throws java.time.DateTimeException si el dia no existe -- el 366 en un año comun
     */
    public LocalDate atDay(int dayOfYear) {
        return LocalDate.ofYearDay(this.getValue(), dayOfYear);
    }

    /** Este año con ese dia-y-mes. */
    public LocalDate atMonthDay(MonthDay monthDay) {
        if (monthDay == null) {
            throw new NullPointerException("monthDay");
        }
        return monthDay.atYear(this.getValue());
    }

    /** Este año con ese mes. */
    public YearMonth atMonth(int month) {
        return YearMonth.of(this.getValue(), month);
    }

    public YearMonth atMonth(Month month) {
        if (month == null) {
            throw new NullPointerException("month");
        }
        return YearMonth.of(this.getValue(), month.getValue());
    }

    /**
     * Este año formateado.
     *
     * @throws java.time.DateTimeException si no se puede formatear
     */
    public String format(java.time.format.DateTimeFormatter formatter) {
        if (formatter == null) {
            throw new NullPointerException("formatter");
        }
        return formatter.format(this);
    }

    /**
     * Este año mas `amountToAdd` unidades.
     *
     * @throws java.time.DateTimeException si la unidad no es de año
     */
    public Year plus(long amountToAdd, java.time.temporal.TemporalUnit unit) {
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        if (unit == ChronoUnit.YEARS) {
            return this.plusYears(amountToAdd);
        }
        if (unit == ChronoUnit.DECADES) {
            return this.plusYears(amountToAdd * 10L);
        }
        if (unit == ChronoUnit.CENTURIES) {
            return this.plusYears(amountToAdd * 100L);
        }
        if (unit == ChronoUnit.MILLENNIA) {
            return this.plusYears(amountToAdd * 1000L);
        }
        if (unit == ChronoUnit.ERAS) {
            // Una era ISO son todos los años de un signo: sumar una lleva del año `y` al `1-y`.
            long era = this.getLong(ChronoField.ERA);
            return (Year) this.with(ChronoField.ERA, era + amountToAdd);
        }
        throw new java.time.temporal.UnsupportedTemporalTypeException("Unsupported unit: " + unit);
    }

    public Year minus(long amountToSubtract, java.time.temporal.TemporalUnit unit) {
        return this.plus(-amountToSubtract, unit);
    }

    /** Este año con `field` puesto en `newValue`. */
    public Year with(java.time.temporal.TemporalField field, long newValue) {
        if (field == null) {
            throw new NullPointerException("field");
        }
        if (field == ChronoField.YEAR) {
            return Year.of((int) ChronoField.YEAR.checkValidValue(newValue));
        }
        if (field == ChronoField.YEAR_OF_ERA) {
            ChronoField.YEAR_OF_ERA.checkValidValue(newValue);
            return Year.of(this.getValue() < 1 ? (int) (1L - newValue) : (int) newValue);
        }
        if (field == ChronoField.ERA) {
            ChronoField.ERA.checkValidValue(newValue);
            // Cambiar de era refleja el año sobre el 1: el año 5 de la era anterior es el -4.
            long yoe = this.getLong(ChronoField.YEAR_OF_ERA);
            return Year.of(newValue == 0L ? (int) (1L - yoe) : (int) yoe);
        }
        throw new java.time.temporal.UnsupportedTemporalTypeException("Unsupported field: " + field);
    }

    public Year plusYears(long yearsToAdd) {
        return new Year((int) (this.year + yearsToAdd));
    }

    public Year minusYears(long yearsToSubtract) {
        return this.plusYears(-yearsToSubtract);
    }

    public boolean isBefore(Year other) {
        return this.year < other.year;
    }

    public boolean isAfter(Year other) {
        return this.year > other.year;
    }

    public int compareTo(Year other) {
        return this.year - other.year;
    }

    // --- Temporal ---

    public boolean isSupported(TemporalField field) {
        return field == ChronoField.YEAR;
    }

    public long getLong(TemporalField field) {
        if (field == ChronoField.YEAR) {
            return this.year;
        }
        throw new java.time.temporal.UnsupportedTemporalTypeException("Unsupported field: " + field);
    }

    public boolean isSupported(TemporalUnit unit) {
        return unit == ChronoUnit.YEARS || unit == ChronoUnit.DECADES
            || unit == ChronoUnit.CENTURIES || unit == ChronoUnit.MILLENNIA;
    }

    public Temporal with(TemporalField field, long newValue) {
        if (field == ChronoField.YEAR) {
            return new Year((int) newValue);
        }
        throw new java.time.temporal.UnsupportedTemporalTypeException("Unsupported field: " + field);
    }

    public Temporal plus(long amountToAdd, TemporalUnit unit) {
        if (unit == ChronoUnit.YEARS) {
            return this.plusYears(amountToAdd);
        }
        if (unit == ChronoUnit.DECADES) {
            return this.plusYears(amountToAdd * 10L);
        }
        if (unit == ChronoUnit.CENTURIES) {
            return this.plusYears(amountToAdd * 100L);
        }
        if (unit == ChronoUnit.MILLENNIA) {
            return this.plusYears(amountToAdd * 1000L);
        }
        throw new java.time.temporal.UnsupportedTemporalTypeException("Unsupported unit: " + unit);
    }

    public Temporal minus(long amountToSubtract, TemporalUnit unit) {
        return this.plus(-amountToSubtract, unit);
    }

    public long until(Temporal endExclusive, TemporalUnit unit) {
        Year end = (Year) endExclusive;
        long yearsDiff = end.year - this.year;
        if (unit == ChronoUnit.YEARS) {
            return yearsDiff;
        }
        if (unit == ChronoUnit.DECADES) {
            return yearsDiff / 10L;
        }
        if (unit == ChronoUnit.CENTURIES) {
            return yearsDiff / 100L;
        }
        if (unit == ChronoUnit.MILLENNIA) {
            return yearsDiff / 1000L;
        }
        throw new java.time.temporal.UnsupportedTemporalTypeException("Unsupported unit: " + unit);
    }

    public Temporal adjustInto(Temporal temporal) {
        return temporal.with(ChronoField.YEAR, this.year);
    }

    public String toString() {
        return Integer.toString(this.year);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Year) {
            return this.year == ((Year) obj).year;
        }
        return false;
    }

    public int hashCode() {
        return this.year;
    }

    // --- generic Temporal-typed conveniences ---

    public int get(TemporalField field) {
        return (int) this.getLong(field);
    }

    public Year plus(TemporalAmount amount) {
        return (Year) amount.addTo(this);
    }

    public Year minus(TemporalAmount amount) {
        return (Year) amount.subtractFrom(this);
    }

    public Year with(TemporalAdjuster adjuster) {
        return (Year) adjuster.adjustInto(this);
    }

    public static Year parse(CharSequence text) {
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
        int year = 0;
        for (int k = i; k < s.length(); k = k + 1) {
            year = year * 10 + (s.charAt(k) - '0');
        }
        return Year.of(sign * year);
    }

    /**
     * Lee `text` con ese formateador.
     *
     * <p>El que decide que campos hay es el formateador; esta clase solo dice **cual de ellos
     * quiere**, pasando su propio `from`. Por eso un patron que no traiga el anio
     * falla aca y no al usar el resultado.
     *
     * @throws java.time.format.DateTimeParseException si el texto no encaja con el patron, o si lo
     *     que encaja no alcanza para un anio
     */
    public static Year parse(CharSequence text, java.time.format.DateTimeFormatter formatter) {
        if (formatter == null) {
            throw new NullPointerException("formatter");
        }
        // Ligado a una local: encadenar por un intermedio de tipo interfaz se pierde (#108).
        java.time.temporal.TemporalQuery<Year> consulta = Year::from;
        return formatter.parse(text, consulta);
    }
}
