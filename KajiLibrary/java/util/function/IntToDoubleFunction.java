package java.util.function;

// KajiLibrary's java.util.function.IntToDoubleFunction — int in, double out. SAM:
// `applyAsDouble`. The shape IntStream.mapToDouble binds to; see IntToLongFunction for why
// the cross-primitive family cannot be folded into the *UnaryOperator interfaces.
public interface IntToDoubleFunction {

    double applyAsDouble(int value);
}
