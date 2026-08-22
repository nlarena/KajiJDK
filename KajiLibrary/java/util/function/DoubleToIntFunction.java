package java.util.function;

// KajiLibrary's java.util.function.DoubleToIntFunction — double in, int out. SAM:
// `applyAsInt`. A narrowing mapping; the shape DoubleStream.mapToInt binds to.
public interface DoubleToIntFunction {

    int applyAsInt(double value);
}
