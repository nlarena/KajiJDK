package java.util.function;

// KajiLibrary's java.util.function.ObjIntConsumer<T> — takes a reference and an int and
// returns nothing. SAM: `accept`. This is the accumulator shape of an int-valued fold:
// the T is the mutable container being filled and the int is the next element, so the
// element must stay unboxed even though the container cannot be. Note the asymmetry with
// BiConsumer<T,Integer> — that one is a strictly worse fit, which is why this exists.
// Unlike BiConsumer it has no `andThen`: the JDK never added composition to the Obj*
// family, so neither do we.
public interface ObjIntConsumer<T> {

    void accept(T t, int value);
}
