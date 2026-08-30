package java.util;

// Same-package import works around the frozen javac's finder (finding #4).
import java.util.NoSuchElementException;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;

// KajiLibrary's java.util.OptionalDouble — the double-specialized Optional: either holds a
// double or is empty, without boxing. Returned e.g. by IntStream.average.
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

    public double orElseThrow() {
        if (!this.isPresent) {
            throw new NoSuchElementException("No value present");
        }
        return this.value;
    }

    public <X extends Throwable> double orElseThrow(Supplier<? extends X> exceptionSupplier)
            throws X {
        if (!this.isPresent) {
            throw exceptionSupplier.get();
        }
        return this.value;
    }

    public double orElseGet(DoubleSupplier supplier) {
        if (this.isPresent) {
            return this.value;
        }
        return supplier.getAsDouble();
    }

    public void ifPresent(DoubleConsumer action) {
        if (this.isPresent) {
            action.accept(this.value);
        }
    }

    public void ifPresentOrElse(DoubleConsumer action, Runnable emptyAction) {
        if (this.isPresent) {
            action.accept(this.value);
        } else {
            emptyAction.run();
        }
    }

    public DoubleStream stream() {
        if (!this.isPresent) {
            return DoubleStream.of(new double[0]);
        }
        double[] uno = new double[1];
        uno[0] = this.value;
        return DoubleStream.of(uno);
    }
}
