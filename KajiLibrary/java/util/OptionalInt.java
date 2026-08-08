package java.util;

// Same-package import works around the frozen javac's finder (finding #4).
import java.util.NoSuchElementException;
import java.util.function.IntConsumer;

// KajiLibrary's java.util.OptionalInt — the int-specialized Optional: either holds an int or is
// empty, without boxing. Returned by int reductions that may have no result (IntStream.min/max/
// findFirst). A KajiLibrary subset (the JDK also has orElseGet/orElseThrow/stream/…).
public final class OptionalInt {

    private static final OptionalInt EMPTY = new OptionalInt(false, 0);

    private final boolean isPresent;
    private final int value;

    private OptionalInt(boolean isPresent, int value) {
        this.isPresent = isPresent;
        this.value = value;
    }

    public static OptionalInt empty() {
        return EMPTY;
    }

    public static OptionalInt of(int value) {
        return new OptionalInt(true, value);
    }

    public int getAsInt() {
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

    public int orElse(int other) {
        if (this.isPresent) {
            return this.value;
        }
        return other;
    }

    public void ifPresent(IntConsumer action) {
        if (this.isPresent) {
            action.accept(this.value);
        }
    }
}
