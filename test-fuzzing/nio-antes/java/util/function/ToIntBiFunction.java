package java.util.function;

// KajiLibrary's java.util.function.ToIntBiFunction<T,U> — two references in, a primitive
// int out. SAM: `applyAsInt`. The two-argument sibling of ToIntFunction; a comparator-like
// scoring of a pair is the typical use, and the result stays unboxed. It has no `andThen`
// because there is no int-in/int-out composition the JDK settled on for the To* family.
public interface ToIntBiFunction<T, U> {

    int applyAsInt(T t, U u);
}
