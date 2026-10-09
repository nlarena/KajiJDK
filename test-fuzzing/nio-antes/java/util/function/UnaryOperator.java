package java.util.function;

// Same-package import works around the frozen javac's finder (finding #4).
import java.util.function.Function;

// KajiLibrary's java.util.function.UnaryOperator<T> — a Function whose argument and result
// are the same type (T -> T). Inherits `apply` from Function; adds the identity operator.
public interface UnaryOperator<T> extends Function<T, T> {

    // The operator that returns its argument unchanged.
    static <T> UnaryOperator<T> identity() {
        return t -> t;
    }
}
