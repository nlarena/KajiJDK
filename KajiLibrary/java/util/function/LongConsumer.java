package java.util.function;

import java.util.Objects;

// KajiLibrary's java.util.function.LongConsumer — an operation on a primitive long returning
// nothing (side effects only). SAM: `accept`.
public interface LongConsumer {

    void accept(long value);

    default LongConsumer andThen(LongConsumer after) {
        Objects.requireNonNull(after);
        return (long value) -> {
            this.accept(value);
            after.accept(value);
        };
    }
}
