package java.util.function;

// KajiLibrary's java.util.function.Supplier<T> — a source of values: takes nothing and
// produces a T on each call (a factory / deferred computation). Its single abstract
// method is `get`. Supplier has no default methods.
public interface Supplier<T> {

    T get();
}
