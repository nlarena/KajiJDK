package java.util.function;

// KajiLibrary's java.util.function.ToLongBiFunction<T,U> — two references in, a primitive
// long out. SAM: `applyAsLong`. The two-argument sibling of ToLongFunction.
public interface ToLongBiFunction<T, U> {

    long applyAsLong(T t, U u);
}
