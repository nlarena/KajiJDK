package java.time;

import java.util.List;
import java.util.ArrayList;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.time.temporal.Temporal;
import java.time.temporal.ChronoUnit;

// KajiLibrary's java.time.Period — a date-based amount of years, months and days (each independent,
// unlike Duration's normalised seconds). Immutable. Implements TemporalAmount so `date.plus(period)`
// works. A KajiLibrary subset (the JDK adds between/parse/toString/multipliedBy).
public final class Period implements TemporalAmount, java.time.chrono.ChronoPeriod {

    private final int years;
    private final int months;
    private final int days;

    private Period(int years, int months, int days) {
        this.years = years;
        this.months = months;
        this.days = days;
    }

    /** El periodo de longitud cero. */
    public static final Period ZERO = new Period(0, 0, 0);

    /**
     * El periodo entre dos fechas, en años, meses y dias.
     *
     * <p>**Los tres campos son independientes y eso es el punto de la clase.** Entre el 31 de enero
     * y el 1 de marzo hay "1 mes y 1 dia", no una cantidad de dias: cuantos dias sean depende de si
     * el año es bisiesto. Por eso `Period` no se puede convertir a `Duration` sin una fecha de
     * referencia, y por eso existen las dos clases.
     *
     * <p>El calculo toma primero los meses completos y despues los dias que sobran, que es lo que
     * hace que valga `start.plus(between(start, end)).equals(end)`. Si se hiciera al reves --dias
     * primero-- esa igualdad se rompe en los meses de distinta longitud.
     */
    public static Period between(LocalDate startDateInclusive, LocalDate endDateExclusive) {
        if (startDateInclusive == null || endDateExclusive == null) {
            throw new NullPointerException();
        }
        long mesesTotales = (long) endDateExclusive.getYear() * 12L
                + (endDateExclusive.getMonthValue() - 1)
                - ((long) startDateInclusive.getYear() * 12L
                        + (startDateInclusive.getMonthValue() - 1));
        int dias = endDateExclusive.getDayOfMonth() - startDateInclusive.getDayOfMonth();
        if (mesesTotales > 0 && dias < 0) {
            // El dia del mes destino quedo antes: el ultimo mes no se completo. Se le resta y los
            // dias se cuentan desde la fecha ya avanzada esos meses.
            mesesTotales = mesesTotales - 1;
            LocalDate avanzada = startDateInclusive.plusMonths(mesesTotales);
            dias = (int) (endDateExclusive.toEpochDay() - avanzada.toEpochDay());
        } else if (mesesTotales < 0 && dias > 0) {
            mesesTotales = mesesTotales + 1;
            dias = dias - endDateExclusive.lengthOfMonth();
        }
        return Period.of((int) (mesesTotales / 12L), (int) (mesesTotales % 12L), dias);
    }

    /**
     * El periodo equivalente a `amount`.
     *
     * @throws java.time.DateTimeException si `amount` usa unidades que no son años, meses o dias
     */
    public static Period from(TemporalAmount amount) {
        if (amount == null) {
            throw new NullPointerException("amount");
        }
        if (amount instanceof Period) {
            return (Period) amount;
        }
        int y = 0;
        int m = 0;
        int d = 0;
        List<TemporalUnit> unidades = amount.getUnits();
        int i = 0;
        while (i < unidades.size()) {
            TemporalUnit u = unidades.get(i);
            long v = amount.get(u);
            if (u == ChronoUnit.YEARS) {
                y = (int) v;
            } else if (u == ChronoUnit.MONTHS) {
                m = (int) v;
            } else if (u == ChronoUnit.DAYS) {
                d = (int) v;
            } else if (v != 0L) {
                throw new java.time.DateTimeException("Unit must be Years, Months or Days, but was " + u);
            }
            i = i + 1;
        }
        return Period.of(y, m, d);
    }

    public static Period of(int years, int months, int days) {
        return new Period(years, months, days);
    }

    public static Period ofYears(int years) {
        return new Period(years, 0, 0);
    }

    public static Period ofMonths(int months) {
        return new Period(0, months, 0);
    }

    public static Period ofWeeks(int weeks) {
        return new Period(0, 0, weeks * 7);
    }

    public static Period ofDays(int days) {
        return new Period(0, 0, days);
    }

    public int getYears() {
        return this.years;
    }

    public int getMonths() {
        return this.months;
    }

    public int getDays() {
        return this.days;
    }

    public boolean isZero() {
        return this.years == 0 && this.months == 0 && this.days == 0;
    }

    public boolean isNegative() {
        return this.years < 0 || this.months < 0 || this.days < 0;
    }

    public Period plusYears(long yearsToAdd) {
        return new Period((int) (this.years + yearsToAdd), this.months, this.days);
    }

    public Period plusMonths(long monthsToAdd) {
        return new Period(this.years, (int) (this.months + monthsToAdd), this.days);
    }

    public Period plusDays(long daysToAdd) {
        return new Period(this.years, this.months, (int) (this.days + daysToAdd));
    }

    public Period minusYears(long yearsToSubtract) {
        return this.plusYears(-yearsToSubtract);
    }

    public Period minusMonths(long monthsToSubtract) {
        return this.plusMonths(-monthsToSubtract);
    }

    public Period minusDays(long daysToSubtract) {
        return this.plusDays(-daysToSubtract);
    }

    // Roll excess months into years (13 months → 1 year, 1 month); days are left alone.
    public Period normalized() {
        long totalMonths = this.years * 12L + this.months;
        return new Period((int) (totalMonths / 12), (int) (totalMonths % 12), this.days);
    }

    /** El calendario de este periodo: el ISO, que es el unico que `Period` modela. */
    public java.time.chrono.IsoChronology getChronology() {
        return java.time.chrono.IsoChronology.INSTANCE;
    }

    /** Este periodo con otros años, dejando meses y dias como estan. */
    public Period withYears(int years) {
        return years == this.years ? this : Period.of(years, this.months, this.days);
    }

    public Period withMonths(int months) {
        return months == this.months ? this : Period.of(this.years, months, this.days);
    }

    public Period withDays(int days) {
        return days == this.days ? this : Period.of(this.years, this.months, days);
    }

    /**
     * Este periodo mas `amountToAdd`, **campo a campo**.
     *
     * <p>Los años se suman a los años y los dias a los dias: no hay conversion entre unidades,
     * porque no existe una. Sumar "1 mes" a "30 dias" da "1 mes y 30 dias", no "60 dias".
     *
     * @throws java.time.DateTimeException si `amountToAdd` usa otras unidades
     */
    public Period plus(TemporalAmount amountToAdd) {
        Period otro = Period.from(amountToAdd);
        return Period.of(this.years + otro.years, this.months + otro.months, this.days + otro.days);
    }

    public Period minus(TemporalAmount amountToSubtract) {
        Period otro = Period.from(amountToSubtract);
        return Period.of(this.years - otro.years, this.months - otro.months, this.days - otro.days);
    }

    /** Cada campo multiplicado por `scalar`. */
    public Period multipliedBy(int scalar) {
        if (scalar == 1 || this.isZero()) {
            return this;
        }
        return Period.of(this.years * scalar, this.months * scalar, this.days * scalar);
    }

    /** Cada campo con el signo cambiado. */
    public Period negated() {
        return this.multipliedBy(-1);
    }

    public long toTotalMonths() {
        return this.years * 12L + this.months;
    }

    // --- TemporalAmount ---

    public long get(TemporalUnit unit) {
        if (unit == ChronoUnit.YEARS) {
            return this.years;
        }
        if (unit == ChronoUnit.MONTHS) {
            return this.months;
        }
        if (unit == ChronoUnit.DAYS) {
            return this.days;
        }
        throw new IllegalArgumentException();
    }

    public List<TemporalUnit> getUnits() {
        List<TemporalUnit> units = new ArrayList<TemporalUnit>();
        units.add(ChronoUnit.YEARS);
        units.add(ChronoUnit.MONTHS);
        units.add(ChronoUnit.DAYS);
        return units;
    }

    public Temporal addTo(Temporal temporal) {
        Temporal result = temporal;
        long totalMonths = this.toTotalMonths();
        if (totalMonths != 0) {
            result = result.plus(totalMonths, ChronoUnit.MONTHS);
        }
        if (this.days != 0) {
            result = result.plus(this.days, ChronoUnit.DAYS);
        }
        return result;
    }

    public Temporal subtractFrom(Temporal temporal) {
        Temporal result = temporal;
        long totalMonths = this.toTotalMonths();
        if (totalMonths != 0) {
            result = result.minus(totalMonths, ChronoUnit.MONTHS);
        }
        if (this.days != 0) {
            result = result.minus(this.days, ChronoUnit.DAYS);
        }
        return result;
    }

    // ISO-8601: PnYnMnD (P0D for zero); omits zero components.
    public String toString() {
        if (this.years == 0 && this.months == 0 && this.days == 0) {
            return "P0D";
        }
        StringBuilder buf = new StringBuilder("P");
        if (this.years != 0) {
            buf.append(Integer.toString(this.years));
            buf.append("Y");
        }
        if (this.months != 0) {
            buf.append(Integer.toString(this.months));
            buf.append("M");
        }
        if (this.days != 0) {
            buf.append(Integer.toString(this.days));
            buf.append("D");
        }
        return buf.toString();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Period) {
            Period o = (Period) obj;
            return this.years == o.years && this.months == o.months && this.days == o.days;
        }
        return false;
    }

    public int hashCode() {
        return this.years + ((this.months << 8) | (this.months >>> 24)) + ((this.days << 16) | (this.days >>> 16));
    }

    // Parses P[nY][nM][nW][nD] (each component optionally signed; weeks fold into days).
    public static Period parse(CharSequence text) {
        String s = text.toString();
        int i = 1;
        int years = 0;
        int months = 0;
        int days = 0;
        while (i < s.length()) {
            int sign = 1;
            if (s.charAt(i) == '-') {
                sign = -1;
                i = i + 1;
            } else if (s.charAt(i) == '+') {
                i = i + 1;
            }
            int nStart = i;
            while (i < s.length() && isDigit(s.charAt(i))) {
                i = i + 1;
            }
            int val = sign * parseDigits(s, nStart, i);
            char u = s.charAt(i);
            i = i + 1;
            if (u == 'Y') {
                years = val;
            } else if (u == 'M') {
                months = val;
            } else if (u == 'W') {
                days = days + val * 7;
            } else if (u == 'D') {
                days = days + val;
            }
        }
        return Period.of(years, months, days);
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static int parseDigits(String s, int from, int to) {
        int v = 0;
        for (int k = from; k < to; k = k + 1) {
            v = v * 10 + (s.charAt(k) - '0');
        }
        return v;
    }
}
