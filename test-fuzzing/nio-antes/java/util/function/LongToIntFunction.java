package java.util.function;

// KajiLibrary's java.util.function.LongToIntFunction — long in, int out. SAM: `applyAsInt`.
// A narrowing mapping, so the lambda body has to write the cast itself: nothing here
// promises the long fits. The shape LongStream.mapToInt binds to.
public interface LongToIntFunction {

    int applyAsInt(long value);
}
