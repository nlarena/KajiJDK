package java.time.temporal;

import java.time.DayOfWeek;

// KajiLibrary's java.time.temporal.TemporalAdjusters — static factories for the common date
// adjusters (first/last day of month/year, and day-of-week relative moves). Each returns a
// TemporalAdjuster backed by one of the package-private strategy classes below. Los ajustadores de
// mes/año caminan con with()/plus()/minus() en vez de apoyarse en `range()`.
public final class TemporalAdjusters {

    private TemporalAdjusters() {
    }

    /**
     * Un ajustador armado a partir de una funcion sobre `LocalDate`.
     *
     * <p>Es la puerta para los ajustes que la biblioteca no trae: se escribe la regla como una
     * funcion de fecha a fecha y esto la envuelve en un `TemporalAdjuster` que `with()` acepta.
     *
     * <p>El envoltorio convierte el `Temporal` que recibe a `LocalDate`, aplica la funcion, y
     * devuelve el temporal original **ajustado** al resultado --no la fecha suelta--, para que
     * ajustar un `LocalDateTime` conserve su hora.
     */
    public static TemporalAdjuster ofDateAdjuster(
            java.util.function.UnaryOperator<java.time.LocalDate> dateBasedAdjuster) {
        if (dateBasedAdjuster == null) {
            throw new NullPointerException("dateBasedAdjuster");
        }
        return new DateAdjuster(dateBasedAdjuster);
    }

    public static TemporalAdjuster firstDayOfMonth() {
        return new FieldAdjuster(0);
    }

    public static TemporalAdjuster lastDayOfMonth() {
        return new FieldAdjuster(1);
    }

    public static TemporalAdjuster firstDayOfNextMonth() {
        return new FieldAdjuster(2);
    }

    public static TemporalAdjuster firstDayOfYear() {
        return new FieldAdjuster(3);
    }

    public static TemporalAdjuster lastDayOfYear() {
        return new FieldAdjuster(4);
    }

    public static TemporalAdjuster firstDayOfNextYear() {
        return new FieldAdjuster(5);
    }

    public static TemporalAdjuster firstInMonth(DayOfWeek dayOfWeek) {
        return new DowInMonthAdjuster(1, dayOfWeek.getValue());
    }

    public static TemporalAdjuster lastInMonth(DayOfWeek dayOfWeek) {
        return new DowInMonthAdjuster(-1, dayOfWeek.getValue());
    }

    public static TemporalAdjuster dayOfWeekInMonth(int ordinal, DayOfWeek dayOfWeek) {
        return new DowInMonthAdjuster(ordinal, dayOfWeek.getValue());
    }

    public static TemporalAdjuster next(DayOfWeek dayOfWeek) {
        return new RelativeDowAdjuster(1, dayOfWeek.getValue());
    }

    public static TemporalAdjuster nextOrSame(DayOfWeek dayOfWeek) {
        return new RelativeDowAdjuster(0, dayOfWeek.getValue());
    }

    public static TemporalAdjuster previous(DayOfWeek dayOfWeek) {
        return new RelativeDowAdjuster(3, dayOfWeek.getValue());
    }

    public static TemporalAdjuster previousOrSame(DayOfWeek dayOfWeek) {
        return new RelativeDowAdjuster(2, dayOfWeek.getValue());
    }
}

// first/last day of month/year via field sets + month/year stepping (types 0..5).
final class FieldAdjuster implements TemporalAdjuster {

    private final int type;

    FieldAdjuster(int type) {
        this.type = type;
    }

    public Temporal adjustInto(Temporal temporal) {
        if (this.type == 0) {
            return temporal.with(ChronoField.DAY_OF_MONTH, 1);
        }
        if (this.type == 1) {
            return temporal.with(ChronoField.DAY_OF_MONTH, 1).plus(1, ChronoUnit.MONTHS).minus(1, ChronoUnit.DAYS);
        }
        if (this.type == 2) {
            return temporal.with(ChronoField.DAY_OF_MONTH, 1).plus(1, ChronoUnit.MONTHS);
        }
        if (this.type == 3) {
            return temporal.with(ChronoField.MONTH_OF_YEAR, 1).with(ChronoField.DAY_OF_MONTH, 1);
        }
        if (this.type == 4) {
            return temporal.with(ChronoField.MONTH_OF_YEAR, 12).with(ChronoField.DAY_OF_MONTH, 31);
        }
        return temporal.with(ChronoField.MONTH_OF_YEAR, 1).with(ChronoField.DAY_OF_MONTH, 1).plus(1, ChronoUnit.YEARS);
    }
}

// the nth (or last, ordinal<0) given-day-of-week within the month.
final class DowInMonthAdjuster implements TemporalAdjuster {

    private final int ordinal;
    private final int dowValue;

    DowInMonthAdjuster(int ordinal, int dowValue) {
        this.ordinal = ordinal;
        this.dowValue = dowValue;
    }

    public Temporal adjustInto(Temporal temporal) {
        if (this.ordinal >= 0) {
            Temporal temp = temporal.with(ChronoField.DAY_OF_MONTH, 1);
            int curDow = temp.get(ChronoField.DAY_OF_WEEK);
            int dowDiff = (this.dowValue - curDow + 7) % 7;
            dowDiff = dowDiff + (this.ordinal - 1) * 7;
            return temp.plus(dowDiff, ChronoUnit.DAYS);
        }
        Temporal temp = temporal.with(ChronoField.DAY_OF_MONTH, 1).plus(1, ChronoUnit.MONTHS).minus(1, ChronoUnit.DAYS);
        int curDow = temp.get(ChronoField.DAY_OF_WEEK);
        int daysDiff = this.dowValue - curDow;
        if (daysDiff > 0) {
            daysDiff = daysDiff - 7;
        }
        daysDiff = daysDiff - ((-this.ordinal - 1) * 7);
        return temp.plus(daysDiff, ChronoUnit.DAYS);
    }
}

// next / nextOrSame / previous / previousOrSame a given day-of-week (relative 1/0/3/2).
final class RelativeDowAdjuster implements TemporalAdjuster {

    private final int relative;
    private final int dowValue;

    RelativeDowAdjuster(int relative, int dowValue) {
        this.relative = relative;
        this.dowValue = dowValue;
    }

    public Temporal adjustInto(Temporal temporal) {
        int calDow = temporal.get(ChronoField.DAY_OF_WEEK);
        if (this.relative == 0) {
            int d = (this.dowValue - calDow + 7) % 7;
            return temporal.plus(d, ChronoUnit.DAYS);
        }
        if (this.relative == 1) {
            int d = (this.dowValue - calDow + 7) % 7;
            if (d == 0) {
                d = 7;
            }
            return temporal.plus(d, ChronoUnit.DAYS);
        }
        if (this.relative == 2) {
            int d = (calDow - this.dowValue + 7) % 7;
            return temporal.minus(d, ChronoUnit.DAYS);
        }
        int d = (calDow - this.dowValue + 7) % 7;
        if (d == 0) {
            d = 7;
        }
        return temporal.minus(d, ChronoUnit.DAYS);
    }
}

// El ajustador que devuelve `ofDateAdjuster`: lleva la funcion y la aplica sobre la fecha del
// temporal, devolviendo el temporal ajustado a la fecha nueva.
final class DateAdjuster implements TemporalAdjuster {

    private final java.util.function.UnaryOperator<java.time.LocalDate> f;

    DateAdjuster(java.util.function.UnaryOperator<java.time.LocalDate> f) {
        this.f = f;
    }

    public Temporal adjustInto(Temporal temporal) {
        java.time.LocalDate actual = java.time.LocalDate.from(temporal);
        java.time.LocalDate nueva = this.f.apply(actual);
        // Se ajusta el temporal recibido en vez de devolver la fecha: asi un `LocalDateTime`
        // conserva su hora, que es lo que el contrato de `with` promete.
        return temporal.with(ChronoField.EPOCH_DAY, nueva.toEpochDay());
    }
}
