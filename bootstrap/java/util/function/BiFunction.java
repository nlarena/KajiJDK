package java.util.function;

// A function of two arguments: (T, U) -> R. Used by CompletableFuture.thenCombine to merge the
// results of two futures into one.
public interface BiFunction<T, U, R> {
    R apply(T t, U u);
}
