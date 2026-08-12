package java.util.function;

import java.util.Objects;

// KajiLibrary's java.util.function.IntUnaryOperator — an operation on a single int yielding an
// int (int -> int). Its single abstract method is `applyAsInt`; compose/andThen chain two of
// them and identity returns its argument unchanged.
public interface IntUnaryOperator {

    int applyAsInt(int operand);

    // First apply `before`, then this.
    default IntUnaryOperator compose(IntUnaryOperator before) {
        Objects.requireNonNull(before);
        return (int v) -> this.applyAsInt(before.applyAsInt(v));
    }

    // First apply this, then `after`.
    default IntUnaryOperator andThen(IntUnaryOperator after) {
        Objects.requireNonNull(after);
        return (int v) -> after.applyAsInt(this.applyAsInt(v));
    }

    static IntUnaryOperator identity() {
        return v -> v;
    }
}
