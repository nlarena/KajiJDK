package java.util.function;

// KajiLibrary's java.util.function.DoubleFunction<R> — a function from a primitive double to
// an R (avoids boxing the argument). Its single abstract method is `apply`. The exit ramp
// out of a double-valued pipeline back into the reference world: DoubleStream.mapToObj.
public interface DoubleFunction<R> {

    R apply(double value);
}
