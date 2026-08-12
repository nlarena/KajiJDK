package java.util.function;

import java.util.Objects;

// KajiLibrary's java.util.function.BiConsumer<T,U> — an operation on two arguments that
// returns nothing (side effects only). `andThen` runs this then another on the same pair.
public interface BiConsumer<T, U> {

    void accept(T t, U u);

    // Perform this, then `after`, on the same inputs.
    default BiConsumer<T, U> andThen(BiConsumer<? super T, ? super U> after) {
        Objects.requireNonNull(after);
        return (T t, U u) -> {
            this.accept(t, u);
            after.accept(t, u);
        };
    }
}
