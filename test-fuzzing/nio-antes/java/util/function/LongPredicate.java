package java.util.function;

import java.util.Objects;

// KajiLibrary's java.util.function.LongPredicate — a boolean-valued test on a primitive long.
public interface LongPredicate {

    boolean test(long value);

    default LongPredicate and(LongPredicate other) {
        Objects.requireNonNull(other);
        return (long value) -> this.test(value) && other.test(value);
    }

    default LongPredicate negate() {
        return (long value) -> !this.test(value);
    }

    default LongPredicate or(LongPredicate other) {
        Objects.requireNonNull(other);
        return (long value) -> this.test(value) || other.test(value);
    }
}
