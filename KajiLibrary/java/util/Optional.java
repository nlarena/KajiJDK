package java.util;

// Same-package import works around the frozen javac's finder (finding #4).
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Consumer;
import java.util.function.Supplier;

// KajiLibrary's java.util.Optional<T> — a container that either holds a value or is empty,
// making "might be absent" explicit in the type instead of leaning on null. Built from
// of/ofNullable/empty, queried with isPresent/get/orElse, transformed with map/filter over
// our own functional interfaces. A KajiLibrary subset (the JDK also has flatMap/or/stream/…).
// (The JDK guards inputs with Objects.requireNonNull; inlined here because the frozen javac
// can't call java.util.Objects — finding #11.)
public final class Optional<T> {

    private static final Optional<?> EMPTY = new Optional<Object>(null);

    // null marks the empty Optional.
    private final T value;

    private Optional(T value) {
        this.value = value;
    }

    public static <T> Optional<T> empty() {
        return (Optional<T>) EMPTY;
    }

    // A present Optional; the value must be non-null.
    public static <T> Optional<T> of(T value) {
        if (value == null) {
            throw new NullPointerException();
        }
        return new Optional<T>(value);
    }

    // Present if `value` is non-null, empty otherwise.
    public static <T> Optional<T> ofNullable(T value) {
        if (value == null) {
            return empty();
        }
        return new Optional<T>(value);
    }

    public T get() {
        if (this.value == null) {
            throw new NoSuchElementException("No value present");
        }
        return this.value;
    }

    public boolean isPresent() {
        return this.value != null;
    }

    public boolean isEmpty() {
        return this.value == null;
    }

    public void ifPresent(Consumer<? super T> action) {
        if (this.value != null) {
            action.accept(this.value);
        }
    }

    public T orElse(T other) {
        if (this.value != null) {
            return this.value;
        }
        return other;
    }

    public T orElseGet(Supplier<? extends T> supplier) {
        if (this.value != null) {
            return this.value;
        }
        return supplier.get();
    }

    // Map the value through `mapper` (empty stays empty). The result is wrapped with
    // ofNullable, so a mapper that returns null yields an empty Optional.
    public <U> Optional<U> map(Function<? super T, ? extends U> mapper) {
        if (this.value == null) {
            return empty();
        }
        return Optional.ofNullable(mapper.apply(this.value));
    }

    // Keep the value only if it matches `predicate`; empty stays empty.
    public Optional<T> filter(Predicate<? super T> predicate) {
        if (this.value == null) {
            return this;
        }
        if (predicate.test(this.value)) {
            return this;
        }
        return empty();
    }
}
