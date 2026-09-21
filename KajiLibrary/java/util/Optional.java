package java.util;

// Same-package import works around the frozen javac's finder (finding #4).
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

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

    /**
     * The value, or `NoSuchElementException` if there is none.
     *
     * <p>It is `get()` under another name, and the name is the point: `get` does not say it can fail,
     * and in an `Optional` **everything** can fail. Java 10 added this one so that one could be
     * discouraged without breaking anything.
     */
    public T orElseThrow() {
        if (this.value == null) {
            throw new NoSuchElementException("No value present");
        }
        return this.value;
    }

    // The value, or whatever exception the supplier builds. It is the way of not losing the context:
    // the caller knows why they expected a value, and this class does not.
    public <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        if (this.value == null) {
            throw exceptionSupplier.get();
        }
        return this.value;
    }

    /**
     * Like `map`, but for a function that **already returns** an Optional.
     *
     * <p>Without it, chaining two lookups that can fail gives an `Optional<Optional<T>>`, which is of
     * no use at all. `flatMap` flattens that extra level, and it is what makes chained lookups read
     * as one.
     */
    public <U> Optional<U> flatMap(Function<? super T, ? extends Optional<? extends U>> mapper) {
        if (this.value == null) {
            return empty();
        }
        Optional<U> r = (Optional<U>) mapper.apply(this.value);
        if (r == null) {
            throw new NullPointerException();
        }
        return r;
    }

    // This one if there is a value, or whatever the supplier builds if not. The alternative is
    // computed **only** if it is needed, which is the whole difference from writing an `orElse` with
    // the lookup inside.
    public Optional<T> or(Supplier<? extends Optional<? extends T>> supplier) {
        if (this.value != null) {
            return this;
        }
        Optional<T> r = (Optional<T>) supplier.get();
        if (r == null) {
            throw new NullPointerException();
        }
        return r;
    }

    // The action if there is a value, the other one if not. `ifPresent`'s missing partner.
    public void ifPresentOrElse(Consumer<? super T> action, Runnable emptyAction) {
        if (this.value != null) {
            action.accept(this.value);
        } else {
            emptyAction.run();
        }
    }

    /**
     * A Stream of zero or one element.
     *
     * <p>It looks ornamental until one sees what it exists for:
     * `stream.map(f).flatMap(Optional::stream)` filters out the absent ones and unwraps the present
     * ones in a single pass, with no `filter` followed by a `map` that repeats the condition.
     */
    public Stream<T> stream() {
        if (this.value == null) {
            Object[] none = new Object[0];
            return (Stream<T>) Stream.of(none);
        }
        Object[] single = new Object[1];
        single[0] = this.value;
        return (Stream<T>) Stream.of(single);
    }
}
