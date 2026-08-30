package java.util.function;

import java.util.Objects;

// KajiLibrary's java.util.function.Predicate<T> — a boolean-valued test on a T (the shape
// filters and conditionals bind to). Its single abstract method is `test`; the default
// `and`/`or`/`negate` build compound predicates from lambdas.
public interface Predicate<T> {

    boolean test(T t);

    // Short-circuiting AND of this and `other`.
    default Predicate<T> and(Predicate<? super T> other) {
        Objects.requireNonNull(other);
        return (T t) -> this.test(t) && other.test(t);
    }

    // The logical negation of this predicate.
    default Predicate<T> negate() {
        return (T t) -> !this.test(t);
    }

    // Short-circuiting OR of this and `other`.
    default Predicate<T> or(Predicate<? super T> other) {
        Objects.requireNonNull(other);
        return (T t) -> this.test(t) || other.test(t);
    }

    // A predicate testing whether two arguments are equal by Objects.equals. Declared `static`
    // WITHOUT an explicit `public` on purpose: the frozen javac drops `public static` interface
    // methods but emits bare `static` ones (which are public by JLS all the same).
    static <T> Predicate<T> isEqual(Object targetRef) {
        if (targetRef == null) {
            return t -> t == null;
        }
        return t -> targetRef.equals(t);
    }

    // The negation of the supplied predicate.
    static <T> Predicate<T> not(Predicate<? super T> target) {
        Objects.requireNonNull(target);
        return (Predicate<T>) target.negate();
    }
}
