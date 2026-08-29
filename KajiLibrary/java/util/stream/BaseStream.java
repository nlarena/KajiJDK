package java.util.stream;

import java.util.Iterator;

// KajiLibrary's java.util.stream.BaseStream — the root of the stream hierarchy: everything
// that is common to Stream<T> and the three primitive streams, independent of element type.
//
// The `S extends BaseStream<T, S>` self-reference (an F-bound) is what lets `sequential()`,
// `parallel()`, `unordered()` and `onClose()` return the *concrete* stream type rather than
// BaseStream: Stream<T> extends BaseStream<T, Stream<T>>, IntStream extends
// BaseStream<Integer, IntStream>, and so on. Each subinterface redeclares those four methods
// with its own type as the return type; that is a plain covariant override, not a new method.
//
// Deliberately left out of this port:
//
//   * `Spliterator<T> spliterator()` — java.util.Spliterator does not exist in KajiLibrary,
//     and the brief for this package forbids creating it (it lives in java.util, not here).
//     Every JDK caller of spliterator() is part of the lazy pipeline machinery, which our
//     eager implementation does not have, so nothing here needs it yet. When java.util grows
//     a Spliterator, adding the method to this interface is a one-line change plus one
//     implementation per *StreamImpl.
//
//   * `close() throws Exception` is *narrowed* to `close()` with no throws clause, exactly as
//     the JDK's BaseStream does — streams never throw a checked exception from close().
public interface BaseStream<T, S extends BaseStream<T, S>> extends AutoCloseable {

    // An Iterator over the elements of this stream. Terminal operation.
    Iterator<T> iterator();

    // Whether this stream would execute in parallel if a terminal op were run now. Our streams
    // are always sequential, so every implementation returns false — but the accessor exists so
    // that code written against the real API keeps compiling.
    boolean isParallel();

    // An equivalent sequential stream (for us: always `this`).
    S sequential();

    // An equivalent parallel stream. We have no fork/join substrate, so this returns a
    // sequential stream — allowed by the JDK contract, which only says the result "may" be
    // parallel, but worth stating: it is a semantic no-op here, not a promise of parallelism.
    S parallel();

    // An equivalent stream with no encounter-order guarantee (for us: always `this`).
    S unordered();

    // Registers a handler to run when close() is called; returns a stream with the handler
    // attached. Handlers run in the order they were registered.
    S onClose(Runnable closeHandler);

    // Runs the registered close handlers. Overrides AutoCloseable.close() to drop the checked
    // `throws Exception`.
    void close();
    /**
     * A spliterator over this stream's elements.
     *
     * <p>A TERMINAL operation, which is what makes it different from every other way of reading
     * a stream: after this the stream is consumed, and the caller owns the traversal.
     */
    java.util.Spliterator<T> spliterator();

}
