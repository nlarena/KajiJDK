package java.util.function;

import java.util.Objects;

// KajiLibrary's java.util.function.Function<T,R> — a one-argument function: given a T,
// produce an R. The canonical functional interface; a lambda `t -> ...` or a method
// reference targets its single abstract method `apply`. `andThen`/`compose` chain two
// functions (each returns a fresh Function built from a lambda), and the static
// `identity` returns the function that hands its argument straight back.
public interface Function<T, R> {

    R apply(T t);

    // First apply this, then feed the result to `after`.
    default <V> Function<T, V> andThen(Function<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        return (T t) -> after.apply(this.apply(t));
    }

    // First apply `before`, then feed its result to this.
    default <V> Function<V, R> compose(Function<? super V, ? extends T> before) {
        Objects.requireNonNull(before);
        return (V v) -> this.apply(before.apply(v));
    }

    // The identity function: identity().apply(x) == x.
    static <T> Function<T, T> identity() {
        return t -> t;
    }
}
