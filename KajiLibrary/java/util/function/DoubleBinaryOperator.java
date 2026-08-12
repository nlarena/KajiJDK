package java.util.function;

// KajiLibrary's java.util.function.DoubleBinaryOperator — an operation on two doubles yielding a
// double ((double, double) -> double), the shape of double reductions. SAM: `applyAsDouble`.
public interface DoubleBinaryOperator {

    double applyAsDouble(double left, double right);
}
