package java.util;

// Same-package import works around the frozen javac's finder (finding #4).
import java.util.NoSuchElementException;

// KajiLibrary's java.util.OptionalDouble — the double-specialized Optional: either holds a
// double or is empty, without boxing. Returned e.g. by IntStream.average. A KajiLibrary subset.
// (ifPresent is omitted for now — it needs DoubleConsumer, not yet in java.util.function.)
public final class OptionalDouble {

    private static final OptionalDouble EMPTY = new OptionalDouble(false, 0.0);

    private final boolean isPresent;
    private final double value;

    private OptionalDouble(boolean isPresent, double value) {
        this.isPresent = isPresent;
        this.value = value;
    }

    public static OptionalDouble empty() {
        return EMPTY;
    }

    public static OptionalDouble of(double value) {
        return new OptionalDouble(true, value);
    }

    public double getAsDouble() {
        if (!this.isPresent) {
            throw new NoSuchElementException("No value present");
        }
        return this.value;
    }

    public boolean isPresent() {
        return this.isPresent;
    }

    public boolean isEmpty() {
        return !this.isPresent;
    }

    public double orElse(double other) {
        if (this.isPresent) {
            return this.value;
        }
        return other;
    }
}
