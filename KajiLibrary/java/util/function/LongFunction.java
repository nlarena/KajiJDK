package java.util.function;

// KajiLibrary's java.util.function.LongFunction<R> — a function from a primitive long to an R
// (avoids boxing the argument). Its single abstract method is `apply`. The exit ramp out of
// a long-valued pipeline back into the reference world: LongStream.mapToObj.
public interface LongFunction<R> {

    R apply(long value);
}
