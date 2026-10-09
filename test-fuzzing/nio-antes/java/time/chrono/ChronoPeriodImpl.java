package java.time.chrono;

import java.util.List;
import java.util.ArrayList;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.time.temporal.ChronoUnit;

// The `ChronoPeriod` of a calendar that is not ISO: years, months and days plus the chronology they
// belong to.
//
// The chronology is not an ornament. A period of "one month" only means something within a calendar,
// and adding a Minguo period to a Hijrah date makes no sense: `plus` and `minus` reject it rather
// than give a result that looks reasonable and is not.
//
// The three fields are independent, as in `java.time.Period`: "1 month and 1 day" is neither 31 days
// nor 32, and how many it turns out to be depends on which date it is added to.
//
// `writeReplace()` is left out: it is Java serialisation's hook, and this library does not implement
// it. A `writeReplace` returning anything at all with no mechanism on the other side would be a
// member with the right signature and no effect.
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
        ChronoPeriodImpl other = this.same(amountToAdd);
        return new ChronoPeriodImpl(this.chrono, this.years + other.years,
                this.months + other.months, this.days + other.days);
    }

    public ChronoPeriod minus(TemporalAmount amountToSubtract) {
        ChronoPeriodImpl other = this.same(amountToSubtract);
        return new ChronoPeriodImpl(this.chrono, this.years - other.years,
                this.months - other.months, this.days - other.days);
    }

    // Adding or subtracting only makes sense between periods of the **same** calendar: "one month"
    // measures different things in each. Rejecting it is the only honest thing.
    private ChronoPeriodImpl same(TemporalAmount amount) {
        if (amount == null) {
            throw new NullPointerException("amount");
        }
        if (!(amount instanceof ChronoPeriodImpl)) {
            throw new java.time.DateTimeException("Unable to add amount: " + amount);
        }
        ChronoPeriodImpl other = (ChronoPeriodImpl) amount;
        if (!other.chrono.getId().equals(this.chrono.getId())) {
            throw new java.time.DateTimeException(
                    "Chronology mismatch, expected: " + this.chrono.getId()
                            + ", actual: " + other.chrono.getId());
        }
        return other;
    }

    public ChronoPeriod multipliedBy(int scalar) {
        if (scalar == 1 || this.isZero()) {
            return this;
        }
        return new ChronoPeriodImpl(this.chrono, this.years * scalar, this.months * scalar,
                this.days * scalar);
    }

    /**
     * The leftover months turned into years.
     *
     * <p>It can only be normalised if the calendar has a fixed number of months per year -- otherwise
     * "twelve months" is not "one year". This library's three have twelve.
     */
    public ChronoPeriod normalized() {
        // This library's three calendars --Minguo, ThaiBuddhist, Japanese-- only renumber the years
        // over ISO: twelve months, and months of the same length. `Chronology` does not expose
        // `range` yet, so the twelve is written out, with this note saying where it comes from and
        // that a calendar of another shape would have to be asked.
        long monthsPerYear = 12L;
        long total = this.years * monthsPerYear + this.months;
        return new ChronoPeriodImpl(this.chrono, (int) (total / monthsPerYear),
                (int) (total % monthsPerYear), this.days);
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
