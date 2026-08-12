package java.util.function;

// KajiLibrary's java.util.function.ToIntFunction<T> — a function from a T to a primitive int
// (avoids boxing the result). Its single abstract method is `applyAsInt`.
public interface ToIntFunction<T> {

    int applyAsInt(T value);
}
