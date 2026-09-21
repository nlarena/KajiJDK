package java.util;

import java.util.function.DoubleConsumer;

// Count, sum, minimum, maximum and average of a stream of `double`s, in a single pass.
//
// The sum is NOT a plain `sum += value`: it uses **Kahan compensated summation**, and that is the
// only part of this class that is not obvious. Adding many values of different magnitudes, each
// floating-point addition loses the low bits of the smaller addend; over a million elements that
// error piles up and the result can be wrong in its leading digits. Kahan keeps what was lost at each
// step off to the side and gives it back to the next one.
//
// The JDK also keeps `simpleSum`, the naive sum, **only** for an edge case: if the compensated one
// gives NaN (which can happen adding infinities of opposite signs) but the naive one gave an
// infinity, the infinity is the correct answer and it is the one returned.
public class DoubleSummaryStatistics implements DoubleConsumer {

    private long count;

    // The compensated sum, and what is left pending compensation.
    private double sum;
    private double sumCompensation;

    // The naive sum, only to break the NaN/infinity tie in `getSum`.
    private double simpleSum;

    private double min = Double.POSITIVE_INFINITY;
    private double max = Double.NEGATIVE_INFINITY;

    // An empty summary. The extremes start at the opposite infinities, for the same reason as in the
    // integer versions: so the first `accept` sets them with no special case.
    public DoubleSummaryStatistics() {
    }

    // A summary with values already computed, for rebuilding a stored one.
    public DoubleSummaryStatistics(long count, double min, double max, double sum) {
        if (count < 0) {
            throw new IllegalArgumentException("Negative count value");
        }
        if (count > 0) {
            if (min > max) {
                throw new IllegalArgumentException("Minimum greater than maximum");
            }
            if (!Double.isNaN(min) && !Double.isNaN(max) && !Double.isNaN(sum)) {
                double average = sum / count;
                if (average < min || average > max) {
                    throw new IllegalArgumentException("Average is out of range");
                }
            }
        }
        this.count = count;
        this.sum = sum;
        this.simpleSum = sum;
        this.sumCompensation = 0.0d;
        this.min = min;
        this.max = max;
    }

    // It adds a value to the summary.
    public void accept(double value) {
        this.count = this.count + 1;
        this.simpleSum = this.simpleSum + value;
        this.compensatedSum(value);
        this.min = Math.min(this.min, value);
        this.max = Math.max(this.max, value);
    }

    // One Kahan step: `sumCompensation` holds what the previous addition could not represent, it is
    // discounted from the new addend, and then how much is left pending this time is recomputed.
    private void compensatedSum(double value) {
        double adjusted = value - this.sumCompensation;
        double fresh = this.sum + adjusted;
        this.sumCompensation = (fresh - this.sum) - adjusted;
        this.sum = fresh;
    }

    // It absorbs another summary.
    //
    // The other's two Kahan parts are added separately —the sum and, with the sign changed, its
    // pending part— so as not to lose the compensation the other was carrying.
    public void combine(DoubleSummaryStatistics other) {
        this.count = this.count + other.count;
        this.simpleSum = this.simpleSum + other.simpleSum;
        this.compensatedSum(other.sum);
        this.compensatedSum(-other.sumCompensation);
        this.min = Math.min(this.min, other.min);
        this.max = Math.max(this.max, other.max);
    }

    public final long getCount() {
        return this.count;
    }

    // The sum, compensated.
    //
    // The tie-break: if the compensated one gave NaN but the naive one gave infinity, the naive one
    // wins. It is the case of adding infinities of opposite signs, where Kahan's correction produces
    // a NaN that does not describe the result.
    public final double getSum() {
        double compensated = this.sum - this.sumCompensation;
        if (Double.isNaN(compensated) && Double.isInfinite(this.simpleSum)) {
            return this.simpleSum;
        }
        return compensated;
    }

    // The minimum, or POSITIVE_INFINITY if nothing was accepted. NaN if any value was.
    public final double getMin() {
        return this.min;
    }

    // The maximum, or NEGATIVE_INFINITY if nothing was accepted. NaN if any value was.
    public final double getMax() {
        return this.max;
    }

    // The average, or 0.0 if nothing was accepted.
    public final double getAverage() {
        if (this.count > 0) {
            return this.getSum() / this.count;
        }
        return 0.0d;
    }

    public String toString() {
        Object[] args = new Object[6];
        args[0] = this.getClass().getSimpleName();
        args[1] = Long.valueOf(this.count);
        args[2] = Double.valueOf(this.getSum());
        args[3] = Double.valueOf(this.getMin());
        args[4] = Double.valueOf(this.getAverage());
        args[5] = Double.valueOf(this.getMax());
        return String.format("%s{count=%d, sum=%f, min=%f, average=%f, max=%f}", args);
    }
}
