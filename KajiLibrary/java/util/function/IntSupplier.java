package java.util.function;

// KajiLibrary's java.util.function.IntSupplier — a source of primitive ints. SAM: `getAsInt`.
// The unboxed twin of Supplier<Integer>; IntStream.generate and the int-flavoured
// accumulators bind to this shape so that a generator never allocates an Integer.
public interface IntSupplier {

    int getAsInt();
}
