package java.util.function;

// java.util.function.Function — a one-argument transform. `CompletableFuture.thenApply` maps a
// result through one.
public interface Function<T, R> {
    R apply(T t);
}
