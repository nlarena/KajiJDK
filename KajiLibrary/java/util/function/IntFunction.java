package java.util.function;

// KajiLibrary's java.util.function.IntFunction<R> — a function from a primitive int to an R
// (avoids boxing the argument). Its single abstract method is `apply`.
public interface IntFunction<R> {

    R apply(int value);
}
