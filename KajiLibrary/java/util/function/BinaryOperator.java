package java.util.function;

import java.util.Comparator;
import java.util.Objects;
// Same-package import works around the frozen javac's finder (finding #4).
import java.util.function.BiFunction;

// KajiLibrary's java.util.function.BinaryOperator<T> — a BiFunction whose two arguments and
// result are all the same type ((T, T) -> T), the shape of reductions like sum/min/max.
// Inherits `apply` from BiFunction. The static `minBy`/`maxBy` turn any Comparator into
// exactly that shape, which is how a reduction gets its "keep the smaller/larger of the
// two" step without every caller writing the same three-line lambda.
public interface BinaryOperator<T> extends BiFunction<T, T, T> {

    // The operator that keeps whichever of its two arguments the comparator ranks lower.
    // `<= 0` keeps the FIRST argument on a tie, which is what makes a reduction stable.
    public static <T> BinaryOperator<T> minBy(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator);
        return (T a, T b) -> {
            if (comparator.compare(a, b) <= 0) {
                return a;
            }
            return b;
        };
    }

    // The operator that keeps whichever of its two arguments the comparator ranks higher.
    public static <T> BinaryOperator<T> maxBy(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator);
        return (T a, T b) -> {
            if (comparator.compare(a, b) >= 0) {
                return a;
            }
            return b;
        };
    }
}
