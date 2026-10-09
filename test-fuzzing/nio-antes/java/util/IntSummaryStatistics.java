package java.util;

import java.util.function.IntConsumer;

// Count, sum, minimum, maximum and average of a stream of `int`s, in a single pass.
//
// It is an `IntConsumer`, and that is its whole design: values are handed to it with `accept` and the
// state is what is left, with no elements stored. That is why `Collectors.summarizingInt` can
// summarise a collection of any size in constant memory.
//
// The sum is a `long` even though the elements are `int`s: adding two billion middling integers
// overflows an `int` long before the stream runs out, and a summary that overshoots in silence is of
// no use at all.
public class IntSummaryStatistics implements IntConsumer {

    private long count;
    private long sum;
    private int min = 2147483647;   // Integer.MAX_VALUE
    private int max = -2147483648;  // Integer.MIN_VALUE

    // An empty summary: count and sum at zero, minimum at MAX_VALUE and maximum at MIN_VALUE.
    //
    // The extremes start inverted on purpose, so the first `accept` sets them with no special case
    // needed. The consequence is that an empty summary returns MAX_VALUE from `getMin()`, which is
    // what the JDK does and what has to be known when reading it.
    public IntSummaryStatistics() {
    }

    // A summary with values already computed, for rebuilding a stored one.
    public IntSummaryStatistics(long count, int min, int max, long sum) {
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

    // It adds a value to the summary.
    public void accept(int value) {
        this.count = this.count + 1;
        this.sum = this.sum + value;
        this.min = Math.min(this.min, value);
        this.max = Math.max(this.max, value);
    }

    // It absorbs another summary. It serves to join the partials of a split stream.
    public void combine(IntSummaryStatistics other) {
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

    // The minimum, or Integer.MAX_VALUE if nothing was accepted.
    public final int getMin() {
        return this.min;
    }

    // The maximum, or Integer.MIN_VALUE if nothing was accepted.
    public final int getMax() {
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
        args[3] = Integer.valueOf(this.min);
        args[4] = Double.valueOf(this.getAverage());
        args[5] = Integer.valueOf(this.max);
        return String.format("%s{count=%d, sum=%d, min=%d, average=%f, max=%d}", args);
    }
}
