package java.util;

// Same-package import works around the frozen javac's finder (finding #4).
import java.util.NoSuchElementException;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.stream.IntStream;

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

    // El valor, o NoSuchElementException. Ver la nota de `Optional.orElseThrow`.
    public int orElseThrow() {
        if (!this.isPresent) {
            throw new NoSuchElementException("No value present");
        }
        return this.value;
    }

    public <X extends Throwable> int orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        if (!this.isPresent) {
            throw exceptionSupplier.get();
        }
        return this.value;
    }

    // El valor, o el que calcule el proveedor. Se calcula **solo** si no hay valor.
    public int orElseGet(IntSupplier supplier) {
        if (this.isPresent) {
            return this.value;
        }
        return supplier.getAsInt();
    }

    public void ifPresentOrElse(IntConsumer action, Runnable emptyAction) {
        if (this.isPresent) {
            action.accept(this.value);
        } else {
            emptyAction.run();
        }
    }

    // Un IntStream de cero o un elemento.
    public IntStream stream() {
        if (!this.isPresent) {
            return IntStream.of(new int[0]);
        }
        int[] uno = new int[1];
        uno[0] = this.value;
        return IntStream.of(uno);
    }
}
