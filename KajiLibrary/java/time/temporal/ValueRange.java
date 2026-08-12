package java.time.temporal;

import java.time.DateTimeException;

// KajiLibrary's java.time.temporal.ValueRange — the range of valid values for a TemporalField. A
// field's minimum and maximum may themselves vary (e.g. day-of-month runs 1..28 in the shortest
// month and 1..31 in the longest), so a range holds four bounds: the smallest/largest minimum and the
// smallest/largest maximum. Immutable value type.
public final class ValueRange {

    private final long minSmallest;
    private final long minLargest;
    private final long maxSmallest;
    private final long maxLargest;

    private ValueRange(long minSmallest, long minLargest, long maxSmallest, long maxLargest) {
        this.minSmallest = minSmallest;
        this.minLargest = minLargest;
        this.maxSmallest = maxSmallest;
        this.maxLargest = maxLargest;
    }

    public static ValueRange of(long min, long max) {
        return new ValueRange(min, min, max, max);
    }

    public static ValueRange of(long min, long maxSmallest, long maxLargest) {
        return new ValueRange(min, min, maxSmallest, maxLargest);
    }

    public static ValueRange of(long minSmallest, long minLargest, long maxSmallest, long maxLargest) {
        return new ValueRange(minSmallest, minLargest, maxSmallest, maxLargest);
    }

    public long getMinimum() {
        return this.minSmallest;
    }

    public long getLargestMinimum() {
        return this.minLargest;
    }

    public long getSmallestMaximum() {
        return this.maxSmallest;
    }

    public long getMaximum() {
        return this.maxLargest;
    }

    public boolean isFixed() {
        return this.minSmallest == this.minLargest && this.maxSmallest == this.maxLargest;
    }

    public boolean isIntValue() {
        return this.getMinimum() >= -2147483648L && this.getMaximum() <= 2147483647L;
    }

    public boolean isValidValue(long value) {
        return value >= this.getMinimum() && value <= this.getMaximum();
    }

    public boolean isValidIntValue(long value) {
        return this.isIntValue() && this.isValidValue(value);
    }

    public long checkValidValue(long value, TemporalField field) {
        if (!this.isValidValue(value)) {
            throw new DateTimeException(this.invalidMessage(field, value));
        }
        return value;
    }

    public int checkValidIntValue(long value, TemporalField field) {
        if (!this.isValidIntValue(value)) {
            throw new DateTimeException(this.invalidMessage(field, value));
        }
        return (int) value;
    }

    private String invalidMessage(TemporalField field, long value) {
        if (field == null) {
            return "Invalid value (valid values " + this.toString() + "): " + Long.toString(value);
        }
        return "Invalid value for " + field + " (valid values " + this.toString() + "): " + Long.toString(value);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ValueRange) {
            ValueRange other = (ValueRange) obj;
            return this.minSmallest == other.minSmallest && this.minLargest == other.minLargest
                && this.maxSmallest == other.maxSmallest && this.maxLargest == other.maxLargest;
        }
        return false;
    }

    public int hashCode() {
        long hash = this.minSmallest + (this.minLargest * 3) + (this.maxSmallest * 5) + (this.maxLargest * 7);
        return (int) (hash ^ (hash >>> 32));
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(Long.toString(this.minSmallest));
        if (this.minSmallest != this.minLargest) {
            buf.append("/");
            buf.append(Long.toString(this.minLargest));
        }
        buf.append(" - ");
        buf.append(Long.toString(this.maxSmallest));
        if (this.maxSmallest != this.maxLargest) {
            buf.append("/");
            buf.append(Long.toString(this.maxLargest));
        }
        return buf.toString();
    }
}
