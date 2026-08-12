package java.util.function;

// Same-package import works around the frozen javac's finder (finding #4).
import java.util.function.BiFunction;

// KajiLibrary's java.util.function.BinaryOperator<T> — a BiFunction whose two arguments and
// result are all the same type ((T, T) -> T), the shape of reductions like sum/min/max.
// Inherits `apply` from BiFunction. (The JDK adds static minBy/maxBy over a Comparator;
// deferred until we wire that in.)
public interface BinaryOperator<T> extends BiFunction<T, T, T> {
}
