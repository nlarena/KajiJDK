package java.util.function;

// KajiLibrary's java.util.function.ToDoubleFunction<T> — a function from a T to a primitive
// double (avoids boxing the result). SAM: `applyAsDouble`. The shape Stream.mapToDouble binds to.
public interface ToDoubleFunction<T> {

    double applyAsDouble(T value);
}
