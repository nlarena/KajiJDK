package java.util.function;

import java.util.Objects;

// KajiLibrary's java.util.function.Consumer<T> — an operation that takes a T and returns
// nothing, working through side effects (e.g. printing, mutating). Its single abstract
// method is `accept`; the default `andThen` runs this consumer then another on the same
// input.
public interface Consumer<T> {

    void accept(T t);

    // Perform this operation, then `after`, on the same input.
    default Consumer<T> andThen(Consumer<? super T> after) {
        Objects.requireNonNull(after);
        return (T t) -> {
            this.accept(t);
            after.accept(t);
        };
    }
}
