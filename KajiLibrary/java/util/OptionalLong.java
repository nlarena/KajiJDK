package java.util;

// Same-package import works around the frozen javac's finder (finding #4).
import java.util.NoSuchElementException;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.stream.LongStream;

// KajiLibrary's java.util.OptionalLong — the long-specialized Optional: either holds a long or is
// empty, without boxing. Returned by long reductions that may have no result (LongStream.min/max/
// findFirst). A KajiLibrary subset.
public final class OptionalLong {

    private static final OptionalLong EMPTY = new OptionalLong(false, 0L);

    private final boolean isPresent;
    private final long value;

    private OptionalLong(boolean isPresent, long value) {
        this.isPresent = isPresent;
        this.value = value;
    }

    public static OptionalLong empty() {
        return EMPTY;
    }

    public static OptionalLong of(long value) {
        return new OptionalLong(true, value);
    }

    public long getAsLong() {
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

    public long orElse(long other) {
        if (this.isPresent) {
            return this.value;
        }
        return other;
    }

    public void ifPresent(LongConsumer action) {
        if (this.isPresent) {
            action.accept(this.value);
        }
    }

    public long orElseThrow() {
        if (!this.isPresent) {
            throw new NoSuchElementException("No value present");
        }
        return this.value;
    }

    public <X extends Throwable> long orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        if (!this.isPresent) {
            throw exceptionSupplier.get();
        }
        return this.value;
    }

    public long orElseGet(LongSupplier supplier) {
        if (this.isPresent) {
            return this.value;
        }
        return supplier.getAsLong();
    }

    public void ifPresentOrElse(LongConsumer action, Runnable emptyAction) {
        if (this.isPresent) {
            action.accept(this.value);
        } else {
            emptyAction.run();
        }
    }

    public LongStream stream() {
        if (!this.isPresent) {
            return LongStream.of(new long[0]);
        }
        long[] uno = new long[1];
        uno[0] = this.value;
        return LongStream.of(uno);
    }
}
