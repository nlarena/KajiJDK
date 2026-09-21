package java.util.stream;

import java.util.List;
import java.util.ArrayList;
import java.util.function.Supplier;
import java.util.function.Function;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * The stock `Gatherer`s, as `Collectors` is for `Collector`.
 *
 * <p>Just as in Collectors.java, <b>there is not one lambda in this file</b>: every piece of every
 * `Gatherer` is a named class. The reason is the same and is noted over there -- a lambda reached
 * through a field of another object does not execute correctly on our VM, and a `Gatherer`'s pieces
 * live exactly there, in `GathererImpl`'s fields.
 *
 * <p>The state containers are `Object[]`, also by the house rule: it avoids accessing the fields of
 * a helper class from another class in the same file.
 *
 * <p>The three `Gatherer`s with a finisher are built with `new GathererImpl<...>(...)` and not with
 * `Gatherer.ofSequential(init, step, end)`, which would be the natural way: this javac does not
 * resolve a call whose parameter puts a type variable inside an invariant type argument
 * (`BiConsumer&lt;A, Downstream&lt;R&gt;&gt;`). It is explained in Gatherer.java's header, and the
 * repro with the five variants is java/WcLib3.java. The constructor does resolve because the type
 * arguments are written, not inferred.
 *
 * <p><b>A divergence from the JDK, and there is only one: `mapConcurrent` is not concurrent.</b> It
 * is documented in its own javadoc; the result is identical, what there is not is parallelism. It
 * is the same decision --and the same precedent-- as `BaseStream.parallel()`, which returns a
 * sequential stream.
 */
public final class Gatherers {

    private Gatherers() {
    }

    /**
     * Consecutive, non-overlapping windows of `windowSize` elements.
     *
     * <p>The last window may come out shorter: it is emitted when the input ends, with whatever it
     * gathered. Each window is a `List` that refuses to be modified.
     *
     * @param windowSize how many elements per window
     * @param <TR> the elements' type
     * @return the `Gatherer`
     * @throws IllegalArgumentException if `windowSize` is less than 1
     */
    public static <TR> Gatherer<TR, ?, List<TR>> windowFixed(int windowSize) {
        if (windowSize < 1) {
            // A constant message: String concatenation at run time is not
            // available on our VM (#226).
            throw new IllegalArgumentException("windowSize must be greater than zero");
        }
        Supplier<Object[]> init = new WindowSupplier();
        Gatherer.Integrator<Object[], TR, List<TR>> step = new FixedWindowIntegrator<TR>(windowSize);
        BiConsumer<Object[], Gatherer.Downstream<? super List<TR>>> end = new FixedWindowFinisher<TR>();
        return new GathererImpl<TR, Object[], List<TR>>(init, step, new NoCombiner<Object[]>(), end);
    }

    /**
     * Windows of `windowSize` elements advancing one at a time.
     *
     * <p>If the whole input has fewer than `windowSize` elements, <b>one</b> window comes out with
     * all of them, instead of none. It is what the JDK does and it is not a whimsical edge case: the
     * alternative --emitting nothing-- loses the input without saying so.
     *
     * @param windowSize how many elements per window
     * @param <TR> the elements' type
     * @return the `Gatherer`
     * @throws IllegalArgumentException if `windowSize` is less than 1
     */
    public static <TR> Gatherer<TR, ?, List<TR>> windowSliding(int windowSize) {
        if (windowSize < 1) {
            throw new IllegalArgumentException("windowSize must be greater than zero");
        }
        Supplier<Object[]> init = new WindowSupplier();
        Gatherer.Integrator<Object[], TR, List<TR>> step = new SlidingWindowIntegrator<TR>(windowSize);
        BiConsumer<Object[], Gatherer.Downstream<? super List<TR>>> end = new SlidingWindowFinisher<TR>();
        return new GathererImpl<TR, Object[], List<TR>>(init, step, new NoCombiner<Object[]>(), end);
    }

    /**
     * It folds the whole input into a single value and emits it at the end.
     *
     * <p>The difference from `Stream.reduce` is of shape, not of computation: this is an
     * <b>intermediate</b> operation leaving a one-element stream, and that is why it can go on being
     * chained.
     *
     * @param initial where the initial value comes from
     * @param folder how to combine the accumulated value with the next element
     * @param <T> the input type
     * @param <R> the accumulated value's type
     * @return the `Gatherer`
     */
    public static <T, R> Gatherer<T, ?, R> fold(Supplier<R> initial,
                                                BiFunction<? super R, ? super T, ? extends R> folder) {
        Supplier<Object[]> init = new FoldSupplier<R>(initial);
        Gatherer.Integrator<Object[], T, R> step = new FoldIntegrator<T, R>(folder);
        BiConsumer<Object[], Gatherer.Downstream<? super R>> end = new FoldFinisher<R>();
        return new GathererImpl<T, Object[], R>(init, step, new NoCombiner<Object[]>(), end);
    }

    /**
     * It emits the accumulated value <b>after each element</b>: the running total.
     *
     * <p>As many elements come out as went in, unlike `fold`, which emits a single one.
     *
     * @param initial where the initial value comes from
     * @param scanner how to combine the accumulated value with the next element
     * @param <T> the input type
     * @param <R> the accumulated value's type
     * @return the `Gatherer`
     */
    public static <T, R> Gatherer<T, ?, R> scan(Supplier<R> initial,
                                                BiFunction<? super R, ? super T, ? extends R> scanner) {
        Supplier<Object[]> init = new FoldSupplier<R>(initial);
        Gatherer.Integrator<Object[], T, R> step = new ScanIntegrator<T, R>(scanner);
        return Gatherer.ofSequential(init, step);
    }

    /**
     * It applies `mapper` to each element, keeping encounter order.
     *
     * <p><b>A deliberate divergence from the JDK: there is no concurrency here.</b> The JDK launches
     * up to `maxConcurrency` virtual threads and emits in order as they finish; this implementation
     * applies `mapper` to one element at a time, on the same thread.
     *
     * <p>The <b>result</b> is identical --the same elements, in the same order-- because this
     * method's concurrency is a property of performance and not of meaning. What changes is the
     * latency when `mapper` blocks, which is exactly what one would use it for. It is declared all
     * the same, on the criterion by which `BaseStream.parallel()` returns a sequential stream: code
     * written against the real API goes on compiling and giving the right thing.
     *
     * <p>`maxConcurrency` is <b>validated</b> even though it is then unused: a program passing 0 is
     * written wrongly against the real API, and finding out here is better than finding out when
     * porting it.
     *
     * @param maxConcurrency how many simultaneous applications the JDK would allow
     * @param mapper the function to apply
     * @param <T> the input type
     * @param <R> the output type
     * @return the `Gatherer`
     * @throws IllegalArgumentException if `maxConcurrency` is less than 1
     */
    public static <T, R> Gatherer<T, ?, R> mapConcurrent(int maxConcurrency,
                                                         Function<? super T, ? extends R> mapper) {
        if (maxConcurrency < 1) {
            throw new IllegalArgumentException("maxConcurrency must be greater than zero");
        }
        Gatherer.Integrator<Void, T, R> step = new MapIntegrator<T, R>(mapper);
        return Gatherer.ofSequential(step);
    }
}

// ---- windows ------------------------------------------------------------------------------------

// Both windows' state is the same Object[2]: {ArrayList<?> buffer, Boolean stillFirst}.
// `stillFirst` is only looked at by the sliding one, but sharing the supplier saves a class.
final class WindowSupplier implements Supplier<Object[]> {
    public Object[] get() {
        Object[] state = new Object[2];
        state[0] = new ArrayList<Object>();
        state[1] = Boolean.TRUE;
        return state;
    }
}

final class FixedWindowIntegrator<TR> implements Gatherer.Integrator<Object[], TR, List<TR>> {

    private final int windowSize;

    FixedWindowIntegrator(int windowSize) {
        this.windowSize = windowSize;
    }

    public boolean integrate(Object[] state, TR element, Gatherer.Downstream<? super List<TR>> downstream) {
        ArrayList<TR> buffer = (ArrayList<TR>) state[0];
        buffer.add(element);
        if (buffer.size() < this.windowSize) {
            return true;
        }
        // The window emitted is a copy: the buffer goes on being used for the next one.
        List<TR> window = new FrozenList<TR>(new ArrayList<TR>(buffer));
        buffer.clear();
        state[1] = Boolean.FALSE;
        return downstream.push(window);
    }
}

final class FixedWindowFinisher<TR> implements BiConsumer<Object[], Gatherer.Downstream<? super List<TR>>> {
    public void accept(Object[] state, Gatherer.Downstream<? super List<TR>> downstream) {
        ArrayList<TR> buffer = (ArrayList<TR>) state[0];
        if (buffer.isEmpty()) {
            return;
        }
        List<TR> window = new FrozenList<TR>(new ArrayList<TR>(buffer));
        buffer.clear();
        downstream.push(window);
    }
}

final class SlidingWindowIntegrator<TR> implements Gatherer.Integrator<Object[], TR, List<TR>> {

    private final int windowSize;

    SlidingWindowIntegrator(int windowSize) {
        this.windowSize = windowSize;
    }

    public boolean integrate(Object[] state, TR element, Gatherer.Downstream<? super List<TR>> downstream) {
        ArrayList<TR> buffer = (ArrayList<TR>) state[0];
        buffer.add(element);
        if (buffer.size() < this.windowSize) {
            return true;
        }
        List<TR> window = new FrozenList<TR>(new ArrayList<TR>(buffer));
        // The oldest is dropped: the window advances one at a time.
        buffer.remove(0);
        state[1] = Boolean.FALSE;
        return downstream.push(window);
    }
}

final class SlidingWindowFinisher<TR> implements BiConsumer<Object[], Gatherer.Downstream<? super List<TR>>> {
    public void accept(Object[] state, Gatherer.Downstream<? super List<TR>> downstream) {
        // It only emits if a window was NEVER completed: that is the case of an input shorter than
        // the window. If one already came out, what is left in the buffer is the last one's tail and
        // emitting it again would be an incomplete window the JDK does not produce.
        Boolean stillFirst = (Boolean) state[1];
        if (!stillFirst.booleanValue()) {
            return;
        }
        ArrayList<TR> buffer = (ArrayList<TR>) state[0];
        if (buffer.isEmpty()) {
            return;
        }
        List<TR> window = new FrozenList<TR>(new ArrayList<TR>(buffer));
        buffer.clear();
        downstream.push(window);
    }
}

// ---- fold and scan ------------------------------------------------------------------------------

// The state is an Object[1] with the accumulated value. `get()` calls the caller's supplier once
// only, when the `Gatherer` starts running, not when it is built.
final class FoldSupplier<R> implements Supplier<Object[]> {

    private final Supplier<R> initial;

    FoldSupplier(Supplier<R> initial) {
        this.initial = initial;
    }

    public Object[] get() {
        Object[] state = new Object[1];
        state[0] = this.initial.get();
        return state;
    }
}

final class FoldIntegrator<T, R> implements Gatherer.Integrator<Object[], T, R> {

    private final BiFunction<R, T, R> folder;

    FoldIntegrator(BiFunction<? super R, ? super T, ? extends R> folder) {
        // The wildcards are shed with a conversion that is exact in erasure: the body only calls
        // `apply`, and through there an R and a T go in and an R comes out.
        Object f = folder;
        this.folder = (BiFunction<R, T, R>) f;
    }

    public boolean integrate(Object[] state, T element, Gatherer.Downstream<? super R> downstream) {
        R accumulated = (R) state[0];
        state[0] = this.folder.apply(accumulated, element);
        return true;
    }
}

final class FoldFinisher<R> implements BiConsumer<Object[], Gatherer.Downstream<? super R>> {
    public void accept(Object[] state, Gatherer.Downstream<? super R> downstream) {
        R accumulated = (R) state[0];
        downstream.push(accumulated);
    }
}

final class ScanIntegrator<T, R> implements Gatherer.Integrator<Object[], T, R> {

    private final BiFunction<R, T, R> scanner;

    ScanIntegrator(BiFunction<? super R, ? super T, ? extends R> scanner) {
        Object f = scanner;
        this.scanner = (BiFunction<R, T, R>) f;
    }

    public boolean integrate(Object[] state, T element, Gatherer.Downstream<? super R> downstream) {
        R accumulated = (R) state[0];
        R next = this.scanner.apply(accumulated, element);
        state[0] = next;
        return downstream.push(next);
    }
}

// ---- mapConcurrent ------------------------------------------------------------------------------

// Stateless: each element is mapped and pushed. See Gatherers.mapConcurrent's javadoc for why
// there are neither threads nor a queue here.
final class MapIntegrator<T, R> implements Gatherer.Integrator<Void, T, R> {

    private final Function<T, R> mapper;

    MapIntegrator(Function<? super T, ? extends R> mapper) {
        Object f = mapper;
        this.mapper = (Function<T, R>) f;
    }

    public boolean integrate(Void state, T element, Gatherer.Downstream<? super R> downstream) {
        R mapped = this.mapper.apply(element);
        return downstream.push(mapped);
    }
}
