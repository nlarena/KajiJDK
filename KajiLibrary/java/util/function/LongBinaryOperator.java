package java.util.function;

// KajiLibrary's java.util.function.LongBinaryOperator — an operation on two longs yielding a long
// ((long, long) -> long), the shape of long reductions. SAM: `applyAsLong`.
public interface LongBinaryOperator {

    long applyAsLong(long left, long right);
}
