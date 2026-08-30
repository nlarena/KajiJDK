package java.time;

import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.Temporal;
import java.time.temporal.ChronoField;

// KajiLibrary's java.time.MonthDay — a month-and-day, e.g. --08-04, without a year (a recurring
// annual date like a birthday). Immutable. Implements TemporalAccessor (read-only — a MonthDay
// isn't a full Temporal), TemporalAdjuster and Comparable. A KajiLibrary subset.
public final class MonthDay implements TemporalAccessor, TemporalAdjuster, Comparable<MonthDay> {

    private final int month;
    private final int day;

    private MonthDay(int month, int day) {
        this.month = month;
        this.day = day;
    }

    /** El dia y mes de hoy, en la zona por defecto. */
    public static MonthDay now() {
        LocalDate d = LocalDate.now();
        return MonthDay.of(d.getMonthValue(), d.getDayOfMonth());
    }

    /** El que marca `clock`. La forma testeable de `now()`. */
    public static MonthDay now(java.time.Clock clock) {
        if (clock == null) {
            throw new NullPointerException("clock");
        }
        LocalDate d = LocalDate.now(clock);
        return MonthDay.of(d.getMonthValue(), d.getDayOfMonth());
    }

    /** El de esa zona, ahora. */
    public static MonthDay now(ZoneId zone) {
        if (zone == null) {
            throw new NullPointerException("zone");
        }
        LocalDate d = LocalDate.now(zone);
        return MonthDay.of(d.getMonthValue(), d.getDayOfMonth());
    }

    /** El dia y mes que `temporal` tiene. */
    public static MonthDay from(java.time.temporal.TemporalAccessor temporal) {
        if (temporal == null) {
            throw new NullPointerException("temporal");
        }
        if (temporal instanceof MonthDay) {
            return (MonthDay) temporal;
        }
        return MonthDay.of(temporal.get(ChronoField.MONTH_OF_YEAR),
                temporal.get(ChronoField.DAY_OF_MONTH));
    }

    /** Con el mes como enum. */
    public static MonthDay of(Month month, int dayOfMonth) {
        if (month == null) {
            throw new NullPointerException("month");
        }
        return MonthDay.of(month.getValue(), dayOfMonth);
    }

    public static MonthDay of(int month, int dayOfMonth) {
        return new MonthDay(month, dayOfMonth);
    }

    public int getMonthValue() {
        return this.month;
    }

    public Month getMonth() {
        return Month.of(this.month);
    }

    public int getDayOfMonth() {
        return this.day;
    }

    /**
     * Si este dia-y-mes existe en ese año.
     *
     * <p>Solo el 29 de febrero puede no existir, y es justamente el caso por el que `MonthDay`
     * guarda 1..29 para febrero y no 1..28: un 29 de febrero es un dia-y-mes valido, y en que años
     * cae es otra pregunta.
     */
    public boolean isValidYear(int year) {
        return !(this.getDayOfMonth() == 29 && this.getMonthValue() == 2
                && !Year.isLeap((long) year));
    }

    /** Este dia-y-mes con otro mes; si el dia no existe en el nuevo mes, se recorta al ultimo. */
    public MonthDay withMonth(int month) {
        return this.with(Month.of(month));
    }

    public MonthDay with(Month month) {
        if (month == null) {
            throw new NullPointerException("month");
        }
        if (month.getValue() == this.getMonthValue()) {
            return this;
        }
        // Se recorta, no se rechaza: el 31 de enero con el mes puesto en abril es el 30 de abril.
        // Es lo que hace el JDK, y la alternativa --tirar-- volveria inusable `with` sobre cualquier
        // dia mayor a 28.
        int dia = Math.min(this.getDayOfMonth(), month.maxLength());
        return MonthDay.of(month.getValue(), dia);
    }

    /** Con otro dia del mes. */
    public MonthDay withDayOfMonth(int dayOfMonth) {
        if (dayOfMonth == this.getDayOfMonth()) {
            return this;
        }
        return MonthDay.of(this.getMonthValue(), dayOfMonth);
    }

    /** Formateado. */
    public String format(java.time.format.DateTimeFormatter formatter) {
        if (formatter == null) {
            throw new NullPointerException("formatter");
        }
        return formatter.format(this);
    }

    public LocalDate atYear(int year) {
        return LocalDate.of(year, this.month, this.day);
    }

    public boolean isBefore(MonthDay other) {
        return this.compareTo(other) < 0;
    }

    public boolean isAfter(MonthDay other) {
        return this.compareTo(other) > 0;
    }

    public int compareTo(MonthDay other) {
        if (this.month != other.month) {
            return this.month - other.month;
        }
        return this.day - other.day;
    }

    // --- TemporalAccessor / TemporalAdjuster ---

    public boolean isSupported(TemporalField field) {
        return field == ChronoField.MONTH_OF_YEAR || field == ChronoField.DAY_OF_MONTH;
    }

    public long getLong(TemporalField field) {
        if (field == ChronoField.MONTH_OF_YEAR) {
            return this.month;
        }
        if (field == ChronoField.DAY_OF_MONTH) {
            return this.day;
        }
        throw new IllegalArgumentException();
    }

    public Temporal adjustInto(Temporal temporal) {
        return temporal.with(ChronoField.MONTH_OF_YEAR, this.month).with(ChronoField.DAY_OF_MONTH, this.day);
    }

    // ISO-8601: --MM-dd
    public String toString() {
        StringBuilder buf = new StringBuilder("--");
        if (this.month < 10) {
            buf.append("0");
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
        if (obj instanceof MonthDay) {
            MonthDay o = (MonthDay) obj;
            return this.month == o.month && this.day == o.day;
        }
        return false;
    }

    public int hashCode() {
        return (this.month << 6) + this.day;
    }

    // Parses --MM-dd.
    public static MonthDay parse(CharSequence text) {
        String s = text.toString();
        int month = parseDigits(s, 2, 4);
        int day = parseDigits(s, 5, 7);
        return MonthDay.of(month, day);
    }

    private static int parseDigits(String s, int from, int to) {
        int v = 0;
        for (int k = from; k < to; k = k + 1) {
            v = v * 10 + (s.charAt(k) - '0');
        }
        return v;
    }
}
