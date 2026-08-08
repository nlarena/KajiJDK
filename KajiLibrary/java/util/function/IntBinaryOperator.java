package java.util.function;

// KajiLibrary's java.util.function.IntBinaryOperator — an operation on two ints yielding an int
// ((int, int) -> int), the shape of int reductions like sum/min/max. SAM: `applyAsInt`.
public interface IntBinaryOperator {

    int applyAsInt(int left, int right);
}
