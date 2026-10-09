package java.util.function;

import java.util.Objects;

// KajiLibrary's java.util.function.DoubleUnaryOperator — an operation on a single double yielding
// a double (double -> double). SAM: `applyAsDouble`; compose/andThen chain, identity returns arg.
public interface DoubleUnaryOperator {

    double applyAsDouble(double operand);

    default DoubleUnaryOperator compose(DoubleUnaryOperator before) {
        Objects.requireNonNull(before);
        return (double v) -> this.applyAsDouble(before.applyAsDouble(v));
    }

    default DoubleUnaryOperator andThen(DoubleUnaryOperator after) {
        Objects.requireNonNull(after);
        return (double v) -> after.applyAsDouble(this.applyAsDouble(v));
    }

    static DoubleUnaryOperator identity() {
        return v -> v;
    }
}
