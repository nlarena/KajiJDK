package java.util.function;

import java.util.Objects;
// Same-package import works around the frozen javac's finder (finding #4).
import java.util.function.Function;

// KajiLibrary's java.util.function.BiFunction<T,U,R> — a two-argument function: given a T
// and a U, produce an R. `andThen` feeds the result through a further Function.
public interface BiFunction<T, U, R> {

    R apply(T t, U u);

    // Apply this, then run `after` on the result.
    default <V> BiFunction<T, U, V> andThen(Function<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        return (T t, U u) -> after.apply(this.apply(t, u));
    }
}
