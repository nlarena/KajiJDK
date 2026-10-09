package java.util.function;

// KajiLibrary's java.util.function.ObjDoubleConsumer<T> — takes a reference and a double
// and returns nothing. SAM: `accept`. The double-valued accumulator shape: the T is the
// container being mutated, the double is the element being folded into it.
public interface ObjDoubleConsumer<T> {

    void accept(T t, double value);
}
