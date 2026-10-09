package java.util.function;

// KajiLibrary's java.util.function.ToDoubleBiFunction<T,U> — two references in, a primitive
// double out. SAM: `applyAsDouble`. The two-argument sibling of ToDoubleFunction.
public interface ToDoubleBiFunction<T, U> {

    double applyAsDouble(T t, U u);
}
