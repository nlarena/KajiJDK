package java.util.function;

import java.util.Objects;

// KajiLibrary's java.util.function.DoubleConsumer — an operation on a primitive double returning
// nothing (side effects only). SAM: `accept`.
public interface DoubleConsumer {

    void accept(double value);

    default DoubleConsumer andThen(DoubleConsumer after) {
        Objects.requireNonNull(after);
        return (double value) -> {
            this.accept(value);
            after.accept(value);
        };
    }
}
