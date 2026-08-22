package java.util.function;

// KajiLibrary's java.util.function.DoubleToLongFunction — double in, long out. SAM:
// `applyAsLong`. A narrowing mapping; the shape DoubleStream.mapToLong binds to.
public interface DoubleToLongFunction {

    long applyAsLong(double value);
}
