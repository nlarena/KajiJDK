package java.util;

import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

// Count, sum, minimum, maximum and average of a stream of `long`s, in a single pass.
//
// It implements **both** interfaces, `LongConsumer` and `IntConsumer`, and that is no oversight of
// the JDK's: an `int` fits in a `long` losing nothing, so the same summary serves a stream of
// integers without forcing the caller to convert. `accept(int)` delegates to `accept(long)`.
//
// Unlike IntSummaryStatistics, here the sum **can** overflow: it is a `long`, the same as the
// elements. The JDK accepts that limit rather than carry a wider accumulator, and it is
// replicated.
public class LongSummaryStatistics implements LongConsumer, IntConsumer {

    private long count;
    private long sum;
    private long min = 9223372036854775807L;   // Long.MAX_VALUE
    private long max = -9223372036854775808L;  // Long.MIN_VALUE

    // An empty summary, with the extremes inverted so the first `accept` sets them.
    public LongSummaryStatistics() {
    }

    // A summary with values already computed, for rebuilding a stored one.
    public LongSummaryStatistics(long count, long min, long max, long sum) {
        if (count < 0) {
            throw new IllegalArgumentException("Negative count value");
        }
        if (count > 0) {
            if (min > max) {
                throw new IllegalArgumentException("Minimum greater than maximum");
            }
            long average = sum / count;
            if (average < min || average > max) {
                throw new IllegalArgumentException("Average is out of range");
            }
        }
        this.count = count;
        this.sum = sum;
        this.min = min;
        this.max = max;
    }

    // It adds an `int`, widened to a `long`.
    public void accept(int value) {
        this.accept((long) value);
    }

    // It adds a value to the summary.
    public void accept(long value) {
        this.count = this.count + 1;
        this.sum = this.sum + value;
        this.min = Math.min(this.min, value);
        this.max = Math.max(this.max, value);
    }

    // It absorbs another summary.
    public void combine(LongSummaryStatistics other) {
        this.count = this.count + other.count;
        this.sum = this.sum + other.sum;
        this.min = Math.min(this.min, other.min);
        this.max = Math.max(this.max, other.max);
    }

    public final long getCount() {
        return this.count;
    }

    public final long getSum() {
        return this.sum;
    }

    // The minimum, or Long.MAX_VALUE if nothing was accepted.
    public final long getMin() {
        return this.min;
    }

    // The maximum, or Long.MIN_VALUE if nothing was accepted.
    public final long getMax() {
        return this.max;
    }

    // The average, or 0.0 if nothing was accepted.
    public final double getAverage() {
        if (this.count > 0) {
            return (double) this.sum / this.count;
        }
        return 0.0d;
    }

    public String toString() {
        Object[] args = new Object[6];
        args[0] = this.getClass().getSimpleName();
        args[1] = Long.valueOf(this.count);
        args[2] = Long.valueOf(this.sum);
        args[3] = Long.valueOf(this.min);
        args[4] = Double.valueOf(this.getAverage());
        args[5] = Long.valueOf(this.max);
        return String.format("%s{count=%d, sum=%d, min=%d, average=%f, max=%d}", args);
    }
}
