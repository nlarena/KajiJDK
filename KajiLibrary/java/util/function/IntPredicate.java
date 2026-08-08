package java.util.function;

import java.util.Objects;

// KajiLibrary's java.util.function.IntPredicate — a boolean-valued test on a primitive int.
// Its single abstract method is `test`; the default and/or/negate build compound predicates.
public interface IntPredicate {

    boolean test(int value);

    default IntPredicate and(IntPredicate other) {
        Objects.requireNonNull(other);
        return (int value) -> this.test(value) && other.test(value);
    }

    default IntPredicate negate() {
        return (int value) -> !this.test(value);
    }

    default IntPredicate or(IntPredicate other) {
        Objects.requireNonNull(other);
        return (int value) -> this.test(value) || other.test(value);
    }
}
