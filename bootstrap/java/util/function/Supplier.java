package java.util.function;

// java.util.function.Supplier — a source of a value, taking no input. The unit `CompletableFuture`
// runs for `supplyAsync`.
public interface Supplier<T> {
    T get();
}
