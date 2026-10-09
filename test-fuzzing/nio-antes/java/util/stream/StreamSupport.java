package java.util.stream;

import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Supplier;

/**
 * From a `Spliterator` to a stream: this is where a collection of one's own becomes a `Stream`.
 *
 * <p>It is the other side of `BaseStream.spliterator()`. Every collection in this library that
 * offers `stream()` can write it as `StreamSupport.stream(this.spliterator(), false)`.
 *
 * <p><b>A divergence from the JDK, this package's usual one: here the traversal is EAGER.</b> The
 * JDK wraps the `Spliterator` and walks it only when a terminal operation runs; we drain it whole
 * into an array before returning the stream. For every finite source the result is the same. For
 * an <b>infinite</b> source --a `Spliterator` that never stops yielding elements-- this does not
 * terminate, whereas the JDK would accept it and let a `limit()` cut it.
 *
 * <p>That difference cannot be turned into a refusal like `Stream.generate`'s, and it is worth
 * saying why: `generate` is infinite <em>always</em>, and that is why refusing is the right answer
 * in 100 % of cases; a `Spliterator` is finite almost always, and there is no way of asking it
 * which it is --`estimateSize()` can return `Long.MAX_VALUE` for infinite and for "I do not know"
 * alike. Refusing here would break every legitimate use to catch a rare one. It is documented and
 * not hidden.
 *
 * <p>The `parallel` parameter is accepted and ignored, just as in `BaseStream.parallel()`: there
 * is no fork/join substrate, and the JDK's contract only says the stream <em>may</em> be parallel.
 * The overloads with `characteristics` accept and ignore it too: the characteristics only enable
 * optimisations of the lazy machinery this package does not have, and none of them changes which
 * elements come out or in what order.
 */
public final class StreamSupport {

    private StreamSupport() {
    }

    /**
     * A stream with the elements `spliterator` yields.
     *
     * @param spliterator the source, which is left consumed
     * @param parallel accepted and ignored (see the header)
     * @param <T> the elements' type
     * @return the stream
     * @throws NullPointerException if `spliterator` is null
     */
    public static <T> Stream<T> stream(Spliterator<T> spliterator, boolean parallel) {
        Objects.requireNonNull(spliterator);
        ObjSink<T> sink = new ObjSink<T>();
        // `tryAdvance` in a loop and not `forEachRemaining`: on the primitive `Spliterator.Of*`
        // `forEachRemaining` is overloaded (the primitive form and the boxing one), and choosing
        // between the two is exactly what this javac gets wrong. With `tryAdvance` the right
        // overload is pinned by the sink's static type.
        while (spliterator.tryAdvance(sink)) {
            // tryAdvance does the work
        }
        return sink.toStream();
    }

    /**
     * The same, with the source deferred: `supplier.get()` is called once only.
     *
     * <p>In the JDK the deferral matters --the `Spliterator` is asked for only when the terminal
     * operation starts, so that a collection still being modified up to that moment does not fire
     * `ConcurrentModificationException`. Here the stream is materialised on construction, so the
     * `get()` happens earlier: the window between asking for the stream and walking it simply does
     * not exist.
     *
     * @param supplier where to get the source from
     * @param characteristics accepted and ignored (see the header)
     * @param parallel accepted and ignored
     * @param <T> the elements' type
     * @return the stream
     * @throws NullPointerException if `supplier` is null
     */
    public static <T> Stream<T> stream(Supplier<? extends Spliterator<T>> supplier, int characteristics,
                                       boolean parallel) {
        Objects.requireNonNull(supplier);
        Spliterator<T> spliterator = supplier.get();
        return StreamSupport.<T>stream(spliterator, parallel);
    }

    /**
     * An `IntStream` with the elements `spliterator` yields.
     *
     * @param spliterator the source, which is left consumed
     * @param parallel accepted and ignored
     * @return the stream
     * @throws NullPointerException if `spliterator` is null
     */
    public static IntStream intStream(Spliterator.OfInt spliterator, boolean parallel) {
        Objects.requireNonNull(spliterator);
        IntSink sink = new IntSink();
        while (spliterator.tryAdvance(sink)) {
            // tryAdvance does the work
        }
        return sink.toStream();
    }

    /**
     * The same, with the source deferred.
     *
     * @param supplier where to get the source from
     * @param characteristics accepted and ignored
     * @param parallel accepted and ignored
     * @return the stream
     * @throws NullPointerException if `supplier` is null
     */
    public static IntStream intStream(Supplier<? extends Spliterator.OfInt> supplier, int characteristics,
                                      boolean parallel) {
        Objects.requireNonNull(supplier);
        Spliterator.OfInt spliterator = supplier.get();
        return StreamSupport.intStream(spliterator, parallel);
    }

    /**
     * A `LongStream` with the elements `spliterator` yields.
     *
     * @param spliterator the source, which is left consumed
     * @param parallel accepted and ignored
     * @return the stream
     * @throws NullPointerException if `spliterator` is null
     */
    public static LongStream longStream(Spliterator.OfLong spliterator, boolean parallel) {
        Objects.requireNonNull(spliterator);
        LongSink sink = new LongSink();
        while (spliterator.tryAdvance(sink)) {
            // tryAdvance does the work
        }
        return sink.toStream();
    }

    /**
     * The same, with the source deferred.
     *
     * @param supplier where to get the source from
     * @param characteristics accepted and ignored
     * @param parallel accepted and ignored
     * @return the stream
     * @throws NullPointerException if `supplier` is null
     */
    public static LongStream longStream(Supplier<? extends Spliterator.OfLong> supplier, int characteristics,
                                        boolean parallel) {
        Objects.requireNonNull(supplier);
        Spliterator.OfLong spliterator = supplier.get();
        return StreamSupport.longStream(spliterator, parallel);
    }

    /**
     * A `DoubleStream` with the elements `spliterator` yields.
     *
     * @param spliterator the source, which is left consumed
     * @param parallel accepted and ignored
     * @return the stream
     * @throws NullPointerException if `spliterator` is null
     */
    public static DoubleStream doubleStream(Spliterator.OfDouble spliterator, boolean parallel) {
        Objects.requireNonNull(spliterator);
        DoubleSink sink = new DoubleSink();
        while (spliterator.tryAdvance(sink)) {
            // tryAdvance does the work
        }
        return sink.toStream();
    }

    /**
     * The same, with the source deferred.
     *
     * @param supplier where to get the source from
     * @param characteristics accepted and ignored
     * @param parallel accepted and ignored
     * @return the stream
     * @throws NullPointerException if `supplier` is null
     */
    public static DoubleStream doubleStream(Supplier<? extends Spliterator.OfDouble> supplier, int characteristics,
                                            boolean parallel) {
        Objects.requireNonNull(supplier);
        Spliterator.OfDouble spliterator = supplier.get();
        return StreamSupport.doubleStream(spliterator, parallel);
    }
}
