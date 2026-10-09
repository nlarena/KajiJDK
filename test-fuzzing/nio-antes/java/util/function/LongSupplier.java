package java.util.function;

// KajiLibrary's java.util.function.LongSupplier — a source of primitive longs. SAM:
// `getAsLong`. The unboxed twin of Supplier<Long>; a clock or counter read in a tight
// loop hands back the value itself rather than a Long wrapper around it.
public interface LongSupplier {

    long getAsLong();
}
