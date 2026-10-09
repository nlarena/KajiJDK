package java.util.function;

// KajiLibrary's java.util.function.ToLongFunction<T> — a function from a T to a primitive long
// (avoids boxing the result). SAM: `applyAsLong`. The shape Stream.mapToLong binds to.
public interface ToLongFunction<T> {

    long applyAsLong(T value);
}
