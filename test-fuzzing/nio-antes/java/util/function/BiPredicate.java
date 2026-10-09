package java.util.function;

import java.util.Objects;

// KajiLibrary's java.util.function.BiPredicate<T,U> — a boolean-valued test on a pair of
// arguments (does this key/value entry qualify?). Its single abstract method is `test`;
// the default `and`/`or`/`negate` build compound tests out of lambdas, exactly as
// Predicate does, and inherit the same short-circuiting from `&&` and `||`.
public interface BiPredicate<T, U> {

    boolean test(T t, U u);

    // Short-circuiting AND of this and `other`.
    default BiPredicate<T, U> and(BiPredicate<? super T, ? super U> other) {
        Objects.requireNonNull(other);
        return (T t, U u) -> this.test(t, u) && other.test(t, u);
    }

    // The logical negation of this predicate.
    default BiPredicate<T, U> negate() {
        return (T t, U u) -> !this.test(t, u);
    }

    // Short-circuiting OR of this and `other`.
    default BiPredicate<T, U> or(BiPredicate<? super T, ? super U> other) {
        Objects.requireNonNull(other);
        return (T t, U u) -> this.test(t, u) || other.test(t, u);
    }
}
