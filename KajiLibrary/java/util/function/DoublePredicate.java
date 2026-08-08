package java.util.function;

import java.util.Objects;

// KajiLibrary's java.util.function.DoublePredicate — a boolean-valued test on a primitive double.
public interface DoublePredicate {

    boolean test(double value);

    default DoublePredicate and(DoublePredicate other) {
        Objects.requireNonNull(other);
        return (double value) -> this.test(value) && other.test(value);
    }

    default DoublePredicate negate() {
        return (double value) -> !this.test(value);
    }

    default DoublePredicate or(DoublePredicate other) {
        Objects.requireNonNull(other);
        return (double value) -> this.test(value) || other.test(value);
    }
}
