package java.util.function;

// KajiLibrary's java.util.function.LongToDoubleFunction — long in, double out. SAM:
// `applyAsDouble`. The shape LongStream.mapToDouble binds to. Widening here is lossy past
// 2^53, which is a property of the conversion, not of this interface.
public interface LongToDoubleFunction {

    double applyAsDouble(long value);
}
