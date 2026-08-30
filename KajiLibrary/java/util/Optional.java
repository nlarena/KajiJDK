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
     * El valor, o `NoSuchElementException` si no hay.
     *
     * <p>Es `get()` con otro nombre, y el nombre es el punto: `get` no dice que puede fallar, y en
     * un `Optional` **todo** puede fallar. Java 10 agrego este para poder desalentar aquel sin
     * romper nada.
     */
    public T orElseThrow() {
        if (this.value == null) {
            throw new NoSuchElementException("No value present");
        }
        return this.value;
    }

    // El valor, o la excepcion que arme el proveedor. Es la forma de no perder el contexto: quien
    // llama sabe por que esperaba un valor, y esta clase no.
    public <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        if (this.value == null) {
            throw exceptionSupplier.get();
        }
        return this.value;
    }

    /**
     * Como `map`, pero para una funcion que **ya devuelve** un Optional.
     *
     * <p>Sin el, encadenar dos busquedas que pueden fallar da un `Optional<Optional<T>>`, que no
     * sirve para nada. `flatMap` aplana ese nivel de mas, y es lo que hace que las busquedas
     * encadenadas se lean como una sola.
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

    // Este si hay valor, o el que arme el proveedor si no. La alternativa se calcula **solo** si
    // hace falta, que es toda la diferencia con escribir un `orElse` con la busqueda adentro.
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

    // La accion si hay valor, la otra si no. El par que faltaba de `ifPresent`.
    public void ifPresentOrElse(Consumer<? super T> action, Runnable emptyAction) {
        if (this.value != null) {
            action.accept(this.value);
        } else {
            emptyAction.run();
        }
    }

    /**
     * Un Stream de cero o un elemento.
     *
     * <p>Parece de adorno hasta que se ve para que existe: `stream.map(f).flatMap(Optional::stream)`
     * filtra los ausentes y desenvuelve los presentes de una sola pasada, sin un `filter` seguido de
     * un `map` que repita la condicion.
     */
    public Stream<T> stream() {
        if (this.value == null) {
            Object[] nada = new Object[0];
            return (Stream<T>) Stream.of(nada);
        }
        Object[] uno = new Object[1];
        uno[0] = this.value;
        return (Stream<T>) Stream.of(uno);
    }
}
