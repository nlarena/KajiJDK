package java.util;

// Same-package import works around the frozen javac's finder (finding #4).
import java.util.NoSuchElementException;
import java.util.function.LongConsumer;

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
}
