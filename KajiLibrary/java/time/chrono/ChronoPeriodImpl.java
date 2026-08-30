package java.time.chrono;

import java.util.List;
import java.util.ArrayList;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.time.temporal.ChronoUnit;

// El `ChronoPeriod` de un calendario que no es el ISO: años, meses y dias mas la cronologia a la que
// pertenecen.
//
// La cronologia no es un adorno. Un periodo de "un mes" solo significa algo dentro de un calendario,
// y sumar un periodo Minguo a una fecha Hijri no tiene sentido: `plus` y `minus` lo rechazan en vez
// de dar un resultado que parece razonable y no lo es.
//
// Los tres campos son independientes, como en `java.time.Period`: "1 mes y 1 dia" no son 31 dias ni
// 32, y cuantos resulten depende de a que fecha se le sumen.
// Queda afuera `writeReplace()`: es el gancho de la serializacion de Java, y esta biblioteca no la
// implementa. Un `writeReplace` que devuelva cualquier cosa sin que exista el mecanismo del otro lado
// seria un miembro con la firma correcta y ningun efecto.
final class ChronoPeriodImpl implements ChronoPeriod {

    private final Chronology chrono;
    private final int years;
    private final int months;
    private final int days;

    ChronoPeriodImpl(Chronology chrono, int years, int months, int days) {
        this.chrono = chrono;
        this.years = years;
        this.months = months;
        this.days = days;
    }

    public Chronology getChronology() {
        return this.chrono;
    }

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
        throw new java.time.temporal.UnsupportedTemporalTypeException("Unsupported unit: " + unit);
    }

    public List<TemporalUnit> getUnits() {
        List<TemporalUnit> out = new ArrayList<TemporalUnit>();
        out.add(ChronoUnit.YEARS);
        out.add(ChronoUnit.MONTHS);
        out.add(ChronoUnit.DAYS);
        return out;
    }

    public boolean isZero() {
        return this.years == 0 && this.months == 0 && this.days == 0;
    }

    public boolean isNegative() {
        return this.years < 0 || this.months < 0 || this.days < 0;
    }

    public ChronoPeriod plus(TemporalAmount amountToAdd) {
        ChronoPeriodImpl otro = this.mismo(amountToAdd);
        return new ChronoPeriodImpl(this.chrono, this.years + otro.years,
                this.months + otro.months, this.days + otro.days);
    }

    public ChronoPeriod minus(TemporalAmount amountToSubtract) {
        ChronoPeriodImpl otro = this.mismo(amountToSubtract);
        return new ChronoPeriodImpl(this.chrono, this.years - otro.years,
                this.months - otro.months, this.days - otro.days);
    }

    // Sumar o restar solo tiene sentido entre periodos del **mismo** calendario: "un mes" mide cosas
    // distintas en cada uno. Rechazarlo es lo unico honesto.
    private ChronoPeriodImpl mismo(TemporalAmount amount) {
        if (amount == null) {
            throw new NullPointerException("amount");
        }
        if (!(amount instanceof ChronoPeriodImpl)) {
            throw new java.time.DateTimeException("Unable to add amount: " + amount);
        }
        ChronoPeriodImpl otro = (ChronoPeriodImpl) amount;
        if (!otro.chrono.getId().equals(this.chrono.getId())) {
            throw new java.time.DateTimeException(
                    "Chronology mismatch, expected: " + this.chrono.getId()
                            + ", actual: " + otro.chrono.getId());
        }
        return otro;
    }

    public ChronoPeriod multipliedBy(int scalar) {
        if (scalar == 1 || this.isZero()) {
            return this;
        }
        return new ChronoPeriodImpl(this.chrono, this.years * scalar, this.months * scalar,
                this.days * scalar);
    }

    /**
     * Los meses sobrantes pasados a años.
     *
     * <p>Solo se puede normalizar si el calendario tiene una cantidad fija de meses por año -- si no,
     * "doce meses" no es "un año". Los tres de esta biblioteca tienen doce.
     */
    public ChronoPeriod normalized() {
        // Los tres calendarios de esta biblioteca --Minguo, ThaiBuddhist, Japanese-- solo renumeran
        // los años sobre el ISO: doce meses, y meses de la misma longitud. `Chronology` todavia no
        // expone `range`, asi que el doce va escrito, con esta nota que dice de donde sale y que un
        // calendario de otra forma necesitaria preguntarselo.
        long mesesPorAnio = 12L;
        long total = this.years * mesesPorAnio + this.months;
        return new ChronoPeriodImpl(this.chrono, (int) (total / mesesPorAnio),
                (int) (total % mesesPorAnio), this.days);
    }

    public Temporal addTo(Temporal temporal) {
        Temporal t = temporal;
        if (this.years != 0) {
            t = t.plus((long) this.years, ChronoUnit.YEARS);
        }
        if (this.months != 0) {
            t = t.plus((long) this.months, ChronoUnit.MONTHS);
        }
        if (this.days != 0) {
            t = t.plus((long) this.days, ChronoUnit.DAYS);
        }
        return t;
    }

    public Temporal subtractFrom(Temporal temporal) {
        Temporal t = temporal;
        if (this.years != 0) {
            t = t.minus((long) this.years, ChronoUnit.YEARS);
        }
        if (this.months != 0) {
            t = t.minus((long) this.months, ChronoUnit.MONTHS);
        }
        if (this.days != 0) {
            t = t.minus((long) this.days, ChronoUnit.DAYS);
        }
        return t;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ChronoPeriodImpl)) {
            return false;
        }
        ChronoPeriodImpl o = (ChronoPeriodImpl) obj;
        return this.years == o.years && this.months == o.months && this.days == o.days
                && this.chrono.getId().equals(o.chrono.getId());
    }

    public int hashCode() {
        return this.years + (this.months << 8) + (this.days << 16) ^ this.chrono.getId().hashCode();
    }

    public String toString() {
        if (this.isZero()) {
            return this.chrono.getId() + " P0D";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(this.chrono.getId()).append(' ').append('P');
        if (this.years != 0) {
            sb.append(this.years).append('Y');
        }
        if (this.months != 0) {
            sb.append(this.months).append('M');
        }
        if (this.days != 0) {
            sb.append(this.days).append('D');
        }
        return sb.toString();
    }
}
