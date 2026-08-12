package java.util.function;

import java.util.Objects;

// KajiLibrary's java.util.function.LongUnaryOperator — an operation on a single long yielding a
// long (long -> long). SAM: `applyAsLong`; compose/andThen chain, identity returns its argument.
public interface LongUnaryOperator {

    long applyAsLong(long operand);

    default LongUnaryOperator compose(LongUnaryOperator before) {
        Objects.requireNonNull(before);
        return (long v) -> this.applyAsLong(before.applyAsLong(v));
    }

    default LongUnaryOperator andThen(LongUnaryOperator after) {
        Objects.requireNonNull(after);
        return (long v) -> after.applyAsLong(this.applyAsLong(v));
    }

    static LongUnaryOperator identity() {
        return v -> v;
    }
}
