package java.util.stream;

import java.util.function.Supplier;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;

// KajiLibrary's java.util.stream.Collector<T,A,R> — the recipe a Stream's terminal `collect`
// follows to fold elements of type T into a result of type R, using a mutable accumulator of
// type A: create one (`supplier`), fold each element in (`accumulator`), merge partials
// (`combiner`), and finish (`finisher`). `characteristics` flags optimisations the pipeline
// may exploit.
//
// This is the interface only, and a subset. Deferred:
//   - the static `of(...)` factories and any concrete Collector need #9/#10 (a concrete
//     `implements Collector` hits the generic-override check #9; a capturing anonymous one #10);
//   - `characteristics()` and its nested `enum Characteristics` need #12 — an enum nested in an
//     *interface* is miscompiled by the frozen javac (the constants/values/valueOf/static-init
//     aren't generated; a `class`-nested enum works fine).
public interface Collector<T, A, R> {

    Supplier<A> supplier();

    BiConsumer<A, T> accumulator();

    BinaryOperator<A> combiner();

    Function<A, R> finisher();
}
