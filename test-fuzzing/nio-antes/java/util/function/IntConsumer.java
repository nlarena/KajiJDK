package java.util.function;

import java.util.Objects;

// KajiLibrary's java.util.function.IntConsumer — an operation on a primitive int that returns
// nothing (side effects only). Its single abstract method is `accept`.
public interface IntConsumer {

    void accept(int value);

    default IntConsumer andThen(IntConsumer after) {
        Objects.requireNonNull(after);
        return (int value) -> {
            this.accept(value);
            after.accept(value);
        };
    }
}
