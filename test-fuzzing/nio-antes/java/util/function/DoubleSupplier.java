package java.util.function;

// KajiLibrary's java.util.function.DoubleSupplier — a source of primitive doubles. SAM:
// `getAsDouble`. The unboxed twin of Supplier<Double>; a random-number source feeding a
// numeric pipeline is the motivating case, and Double has no small-value cache at all,
// so every boxed answer there would be a fresh allocation.
public interface DoubleSupplier {

    double getAsDouble();
}
