package java.util.function;

// KajiLibrary's java.util.function.ObjLongConsumer<T> — takes a reference and a long and
// returns nothing. SAM: `accept`. The long-valued accumulator shape: the T is the
// container being mutated, the long is the element being folded into it.
public interface ObjLongConsumer<T> {

    void accept(T t, long value);
}
