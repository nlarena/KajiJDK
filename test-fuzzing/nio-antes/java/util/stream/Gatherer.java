package java.util.stream;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;

/**
 * The recipe of a user-defined intermediate operation: what `Stream.gather` runs.
 *
 * <p>A `Gatherer` has four pieces, just like a `Collector`: how to create the private state
 * (`initializer`), what to do with each element (`integrator`), how to merge two partial states
 * (`combiner`) and what to emit at the end (`finisher`).
 *
 * <p><b>Why this type does fit a library of eager streams, and `generate` does not.</b> The
 * difference is in who pushes. A lazy stream <em>pulls</em> from its source, and that is why an
 * endless source can be represented without materialising it; a `Gatherer` <em>pushes</em> towards
 * the `Downstream` it is given. Pushing needs no laziness: we walk the input array once, call the
 * integrator per element and gather whatever comes out. The one trait of the lazy model that has to
 * be emulated by hand is the <b>short-circuit</b>, and it is provided for: if `integrate` returns
 * `false` the traversal stops there, without visiting the rest (see `Stream.gather`).
 *
 * <p><b>Four factories are declared but CANNOT BE CALLED TODAY, and the fault is the compiler's,
 * not the implementation's.</b> They are the four that take a finisher:
 * `of(Integrator, BiConsumer)`, `of(Supplier, Integrator, BinaryOperator, BiConsumer)`,
 * `ofSequential(Integrator, BiConsumer)` and `ofSequential(Supplier, Integrator, BiConsumer)`. This
 * javac holds no method applicable whose <b>parameter</b> puts a type variable inside an
 * <b>invariant</b> type argument --the shape `BiConsumer&lt;A, Downstream&lt;R&gt;&gt;`-- even when
 * the argument has exactly that type written by hand and even when an explicit `witness` is passed.
 * What saves the case is a wildcard <em>in that position</em>
 * (`Supplier&lt;? extends Spliterator&lt;T&gt;&gt;` does resolve, which is why `StreamSupport` can
 * be called in full); these four's `? super R` is <em>inside</em> `Downstream`, not at the nested
 * position, so it does not save it. It is the same family `Stream.mapMulti` already had noted, only
 * that here it comes out as a hard error ("resolved to no method") and not in silence. Repro with
 * the five variants: java/WcLib3.java + java/WcUse3.java.
 * The bodies are correct and the day the compiler resolves the call they work; in the meantime,
 * `Gatherers` builds its `GathererImpl` with the constructor, which does resolve because the
 * class's type arguments are written and not inferred. What CAN be called today is covered by the
 * probe java/GfacProbe.java.
 *
 * <p><b>A divergence from the JDK, and it is the only one.</b> `combiner()` describes how to merge
 * two states of two halves evaluated <em>in parallel</em>. Our `gather` is sequential and never
 * splits the input, so it never calls it. `defaultCombiner()`'s default combiner refuses when
 * invoked, exactly as the JDK's does: a combiner that cannot merge honestly is better than one that
 * returns either side and pretends.
 *
 * @param <T> the type of the elements that come in
 * @param <A> the private state's type (`Void` if none is needed)
 * @param <R> the type of the elements that come out
 */
public interface Gatherer<T, A, R> {

    /**
     * Where a `Gatherer` pushes the elements it produces.
     *
     * <p>`push` returns `false` when what is below wants no more -- because there was a `limit`, or
     * because a composed `Gatherer` further down short-circuited. An integrator that respects that
     * answer is what makes a short-circuit propagate upwards instead of walking the whole input.
     *
     * @param <T> the type of what is pushed
     */
    interface Downstream<T> {

        /**
         * It pushes an element downwards.
         *
         * @param element the element
         * @return `false` if nothing more will be accepted from here on
         */
        boolean push(T element);

        /**
         * Whether it is already known that no later `push` will be accepted.
         *
         * <p>It is a question, not a promise the other way: a `false` does not guarantee the next
         * `push` will be accepted. It serves to abandon an expensive computation that was going to
         * be discarded anyway.
         *
         * @return `true` if it is rejecting
         */
        default boolean isRejecting() {
            return false;
        }
    }

    /**
     * What to do with each input element.
     *
     * <p>The type parameters' order is `A, T, R` --state, input, output-- and not the `T, A, R` of
     * the `Gatherer` that contains it. It is the JDK's order and it is kept as it stands: it is the
     * order they appear in in `integrate`.
     *
     * @param <A> the private state
     * @param <T> the input type
     * @param <R> the output type
     */
    interface Integrator<A, T, R> {

        /**
         * It processes an element, pushing zero or more elements towards `downstream`.
         *
         * @param state the private state
         * @param element the input element
         * @param downstream where to push what is produced
         * @return `false` to ask that no further element be sent
         */
        boolean integrate(A state, T element, Downstream<? super R> downstream);

        /**
         * It returns its argument.
         *
         * <p>It exists only to give a lambda a target type to be written at; in the JDK it carries
         * `@ForceInline` because the `invokestatic` disappears under JIT compilation.
         *
         * @param integrator the integrator
         * @param <A> the state
         * @param <T> the input type
         * @param <R> the output type
         * @return `integrator`
         */
        static <A, T, R> Integrator<A, T, R> of(Integrator<A, T, R> integrator) {
            return integrator;
        }

        /**
         * It returns its argument, typed as `Greedy`.
         *
         * @param greedy the greedy integrator
         * @param <A> the state
         * @param <T> the input type
         * @param <R> the output type
         * @return `greedy`
         */
        static <A, T, R> Integrator.Greedy<A, T, R> ofGreedy(Integrator.Greedy<A, T, R> greedy) {
            return greedy;
        }

        /**
         * An integrator that promises never to short-circuit: its `integrate` always returns
         * `true`.
         *
         * <p>It is a marker interface --it adds no member-- and that promise lets the JDK skip the
         * short-circuit check. Our `gather` looks at the returned value anyway, so here the marker
         * changes no result, it only documents it.
         *
         * @param <A> the state
         * @param <T> the input type
         * @param <R> the output type
         */
        interface Greedy<A, T, R> extends Integrator<A, T, R> {
        }
    }

    /**
     * How to create the private state. By default, one that does not exist (`null`).
     *
     * @return the initial state's supplier
     */
    default Supplier<A> initializer() {
        return Gatherer.<A>defaultInitializer();
    }

    /**
     * What to do with each element. It is the only mandatory piece.
     *
     * @return the integrator
     */
    Integrator<A, T, R> integrator();

    /**
     * How to merge two partial states. By default, it cannot be done: see `defaultCombiner`.
     *
     * @return the combiner
     */
    default BinaryOperator<A> combiner() {
        return Gatherer.<A>defaultCombiner();
    }

    /**
     * What to emit when the input has run out. By default, nothing.
     *
     * @return the finisher
     */
    default BiConsumer<A, Downstream<? super R>> finisher() {
        return Gatherer.<A, R>defaultFinisher();
    }

    /**
     * This `Gatherer` followed by `that`: what this one pushes is what that one receives.
     *
     * <p>The composition carries both states in an `Object[2]` and gives the first an intermediate
     * `Downstream` that feeds the second. The short-circuit travels both ways: if the second
     * rejects, the intermediate's `push` returns `false` and the first finds out; if the first
     * short-circuits, the traversal ends and only the two finishers are left, in order.
     *
     * @param that the `Gatherer` that comes after
     * @param <RR> what comes out of the composition
     * @return the composition
     * @throws NullPointerException if `that` is null
     */
    default <RR> Gatherer<T, ?, RR> andThen(Gatherer<? super R, ?, ? extends RR> that) {
        Objects.requireNonNull(that);
        // Both sides are seen as `Gatherer<..., Object, ...>`: each half's state is opaque to the
        // composition, which only stores it and passes it on. The conversion is unchecked in the
        // generics' sense and exact in erasure's.
        Object self = this;
        Object other = that;
        Gatherer<T, Object, R> first = (Gatherer<T, Object, R>) self;
        Gatherer<R, Object, RR> second = (Gatherer<R, Object, RR>) other;
        return new CompositeGatherer<T, R, RR>(first, second);
    }

    /**
     * The default initializer: there is no state, and `get()` returns `null`.
     *
     * @param <A> the nominal state (in practice `Void`)
     * @return a supplier of `null`
     */
    static <A> Supplier<A> defaultInitializer() {
        return new NoStateInitializer<A>();
    }

    /**
     * The default combiner: it refuses to merge.
     *
     * <p>A `Gatherer` that does not say how to merge two halves cannot be evaluated in parallel, and
     * the JDK expresses that with a combiner that throws `UnsupportedOperationException` instead of
     * one that returns the left-hand side. That decision is copied here, even though our `gather`
     * never calls it: the day somebody reads `combiner()` and invokes it, the honest answer is "it
     * cannot be done", not a silently incomplete result.
     *
     * @param <A> the state
     * @return a combiner that always fails
     */
    static <A> BinaryOperator<A> defaultCombiner() {
        return new NoCombiner<A>();
    }

    /**
     * The default finisher: it emits nothing at the end.
     *
     * @param <A> the state
     * @param <R> the output type
     * @return a finisher that does nothing
     */
    static <A, R> BiConsumer<A, Downstream<? super R>> defaultFinisher() {
        return new NoFinisher<A, R>();
    }

    /**
     * A `Gatherer` with neither state nor finisher, marked sequential.
     *
     * @param integrator the integrator
     * @param <T> the input type
     * @param <R> the output type
     * @return the `Gatherer`
     */
    static <T, R> Gatherer<T, Void, R> ofSequential(Integrator<Void, T, R> integrator) {
        Objects.requireNonNull(integrator);
        Supplier<Void> init = Gatherer.<Void>defaultInitializer();
        BinaryOperator<Void> comb = Gatherer.<Void>defaultCombiner();
        BiConsumer<Void, Downstream<? super R>> end = Gatherer.<Void, R>defaultFinisher();
        return new GathererImpl<T, Void, R>(init, integrator, comb, end);
    }

    /**
     * A `Gatherer` with no state and a finisher, marked sequential.
     *
     * @param integrator the integrator
     * @param finisher the finisher
     * @param <T> the input type
     * @param <R> the output type
     * @return the `Gatherer`
     */
    static <T, R> Gatherer<T, Void, R> ofSequential(Integrator<Void, T, R> integrator,
                                                    BiConsumer<Void, Downstream<? super R>> finisher) {
        Objects.requireNonNull(integrator);
        Objects.requireNonNull(finisher);
        Supplier<Void> init = Gatherer.<Void>defaultInitializer();
        BinaryOperator<Void> comb = Gatherer.<Void>defaultCombiner();
        return new GathererImpl<T, Void, R>(init, integrator, comb, finisher);
    }

    /**
     * A `Gatherer` with state and no finisher, marked sequential.
     *
     * @param initializer how to create the state
     * @param integrator the integrator
     * @param <T> the input type
     * @param <A> the state
     * @param <R> the output type
     * @return the `Gatherer`
     */
    static <T, A, R> Gatherer<T, A, R> ofSequential(Supplier<A> initializer, Integrator<A, T, R> integrator) {
        Objects.requireNonNull(initializer);
        Objects.requireNonNull(integrator);
        BinaryOperator<A> comb = Gatherer.<A>defaultCombiner();
        BiConsumer<A, Downstream<? super R>> end = Gatherer.<A, R>defaultFinisher();
        return new GathererImpl<T, A, R>(initializer, integrator, comb, end);
    }

    /**
     * A `Gatherer` with state and a finisher, marked sequential.
     *
     * @param initializer how to create the state
     * @param integrator the integrator
     * @param finisher the finisher
     * @param <T> the input type
     * @param <A> the state
     * @param <R> the output type
     * @return the `Gatherer`
     */
    static <T, A, R> Gatherer<T, A, R> ofSequential(Supplier<A> initializer, Integrator<A, T, R> integrator,
                                                    BiConsumer<A, Downstream<? super R>> finisher) {
        Objects.requireNonNull(initializer);
        Objects.requireNonNull(integrator);
        Objects.requireNonNull(finisher);
        BinaryOperator<A> comb = Gatherer.<A>defaultCombiner();
        return new GathererImpl<T, A, R>(initializer, integrator, comb, finisher);
    }

    /**
     * A `Gatherer` with neither state nor finisher, fit for parallel use.
     *
     * <p>Its combiner merges two "stateless" states, which is trivial: it returns `null`. It is
     * the one case where the default combiner is <em>not</em> the one that refuses.
     *
     * @param integrator the integrator
     * @param <T> the input type
     * @param <R> the output type
     * @return the `Gatherer`
     */
    static <T, R> Gatherer<T, Void, R> of(Integrator<Void, T, R> integrator) {
        Objects.requireNonNull(integrator);
        Supplier<Void> init = Gatherer.<Void>defaultInitializer();
        BinaryOperator<Void> comb = new StatelessCombiner();
        BiConsumer<Void, Downstream<? super R>> end = Gatherer.<Void, R>defaultFinisher();
        return new GathererImpl<T, Void, R>(init, integrator, comb, end);
    }

    /**
     * A `Gatherer` with no state and a finisher, fit for parallel use.
     *
     * @param integrator the integrator
     * @param finisher the finisher
     * @param <T> the input type
     * @param <R> the output type
     * @return the `Gatherer`
     */
    static <T, R> Gatherer<T, Void, R> of(Integrator<Void, T, R> integrator,
                                          BiConsumer<Void, Downstream<? super R>> finisher) {
        Objects.requireNonNull(integrator);
        Objects.requireNonNull(finisher);
        Supplier<Void> init = Gatherer.<Void>defaultInitializer();
        BinaryOperator<Void> comb = new StatelessCombiner();
        return new GathererImpl<T, Void, R>(init, integrator, comb, finisher);
    }

    /**
     * A `Gatherer` with the four pieces given.
     *
     * @param initializer how to create the state
     * @param integrator the integrator
     * @param combiner the combiner
     * @param finisher the finisher
     * @param <T> the input type
     * @param <A> the state
     * @param <R> the output type
     * @return the `Gatherer`
     * @throws NullPointerException if any of the pieces is null
     */
    static <T, A, R> Gatherer<T, A, R> of(Supplier<A> initializer, Integrator<A, T, R> integrator,
                                          BinaryOperator<A> combiner,
                                          BiConsumer<A, Downstream<? super R>> finisher) {
        Objects.requireNonNull(initializer);
        Objects.requireNonNull(integrator);
        Objects.requireNonNull(combiner);
        Objects.requireNonNull(finisher);
        return new GathererImpl<T, A, R>(initializer, integrator, combiner, finisher);
    }
}

// ---- the default pieces -------------------------------------------------------------------------
//
// Named classes and not lambdas, by the house rule that already governs Collectors.java: a lambda
// reached through a *field* of another object does not execute correctly on our VM. These objects
// live exactly there, in GathererImpl's fields.

final class NoStateInitializer<A> implements Supplier<A> {
    public A get() {
        return null;
    }
}

// The combiner that refuses. See Gatherer.defaultCombiner().
final class NoCombiner<A> implements BinaryOperator<A> {
    public A apply(A left, A right) {
        // A constant message: String concatenation at run time is not
        // available in our VM (#226).
        throw new UnsupportedOperationException("this combiner cannot be used");
    }
}

// Merging two states that do not exist: there is nothing to merge.
final class StatelessCombiner implements BinaryOperator<Void> {
    public Void apply(Void left, Void right) {
        return null;
    }
}

final class NoFinisher<A, R> implements BiConsumer<A, Gatherer.Downstream<? super R>> {
    public void accept(A state, Gatherer.Downstream<? super R> downstream) {
    }
}

// ---- the implementation the factories return ----------------------------------------------------

final class GathererImpl<T, A, R> implements Gatherer<T, A, R> {

    private final Supplier<A> initializer;
    private final Gatherer.Integrator<A, T, R> integrator;
    private final BinaryOperator<A> combiner;
    private final BiConsumer<A, Gatherer.Downstream<? super R>> finisher;

    GathererImpl(Supplier<A> initializer, Gatherer.Integrator<A, T, R> integrator, BinaryOperator<A> combiner,
                 BiConsumer<A, Gatherer.Downstream<? super R>> finisher) {
        this.initializer = initializer;
        this.integrator = integrator;
        this.combiner = combiner;
        this.finisher = finisher;
    }

    public Supplier<A> initializer() {
        return this.initializer;
    }

    public Gatherer.Integrator<A, T, R> integrator() {
        return this.integrator;
    }

    public BinaryOperator<A> combiner() {
        return this.combiner;
    }

    public BiConsumer<A, Gatherer.Downstream<? super R>> finisher() {
        return this.finisher;
    }
}

// ---- the buffer `Stream.gather` pushes into -----------------------------------------------------

// It gathers what the Gatherer pushes. It never rejects: it is the end of the chain and the
// resulting stream is materialised whole, so there is nothing further down that could ask to
// short-circuit.
final class GatherBuffer<R> implements Gatherer.Downstream<R> {

    private Object[] data;
    private int size;

    GatherBuffer() {
        this.data = new Object[16];
        this.size = 0;
    }

    public boolean push(R element) {
        if (this.size == this.data.length) {
            Object[] bigger = new Object[this.data.length * 2];
            for (int i = 0; i < this.size; i++) {
                bigger[i] = this.data[i];
            }
            this.data = bigger;
        }
        this.data[this.size] = element;
        this.size = this.size + 1;
        return true;
    }

    public boolean isRejecting() {
        return false;
    }

    // An exact copy, of the live length.
    Object[] toArray() {
        Object[] out = new Object[this.size];
        for (int i = 0; i < this.size; i++) {
            out[i] = this.data[i];
        }
        return out;
    }
}

// ---- the composition of two Gatherers -----------------------------------------------------------

// The `Downstream` the first of the two sees: each `push` is an `integrate` of the second.
final class MidDownstream<R, RR> implements Gatherer.Downstream<R> {

    private final Gatherer.Integrator<Object, R, RR> integrator;
    // The second's state lives in `state[1]` of the shared Object[2] and not in a field of its
    // own: the composition's finisher needs the same state the integrator saw.
    private final Object[] state;
    private final Gatherer.Downstream<RR> down;
    private boolean rejecting;

    MidDownstream(Gatherer.Integrator<Object, R, RR> integrator, Object[] state,
                  Gatherer.Downstream<RR> down) {
        this.integrator = integrator;
        this.state = state;
        this.down = down;
        this.rejecting = false;
    }

    public boolean push(R element) {
        if (this.rejecting) {
            return false;
        }
        boolean goOn = this.integrator.integrate(this.state[1], element, this.down);
        if (!goOn) {
            this.rejecting = true;
        }
        return goOn;
    }

    public boolean isRejecting() {
        if (this.rejecting) {
            return true;
        }
        return this.down.isRejecting();
    }

    boolean rejected() {
        return this.rejecting;
    }
}

final class CompositeIntegrator<T, R, RR> implements Gatherer.Integrator<Object[], T, RR> {

    private final Gatherer<T, Object, R> first;
    private final Gatherer<R, Object, RR> second;

    CompositeIntegrator(Gatherer<T, Object, R> first, Gatherer<R, Object, RR> second) {
        this.first = first;
        this.second = second;
    }

    public boolean integrate(Object[] state, T element, Gatherer.Downstream<? super RR> downstream) {
        Gatherer.Downstream<RR> down = (Gatherer.Downstream<RR>) downstream;
        Gatherer.Integrator<Object, R, RR> i2 = this.second.integrator();
        MidDownstream<R, RR> mid = new MidDownstream<R, RR>(i2, state, down);
        Gatherer.Integrator<Object, T, R> i1 = this.first.integrator();
        boolean goOn = i1.integrate(state[0], element, mid);
        if (!goOn) {
            return false;
        }
        return !mid.rejected();
    }
}

final class CompositeInitializer<T, R, RR> implements Supplier<Object[]> {

    private final Gatherer<T, Object, R> first;
    private final Gatherer<R, Object, RR> second;

    CompositeInitializer(Gatherer<T, Object, R> first, Gatherer<R, Object, RR> second) {
        this.first = first;
        this.second = second;
    }

    public Object[] get() {
        Supplier<Object> s1 = this.first.initializer();
        Supplier<Object> s2 = this.second.initializer();
        Object[] state = new Object[2];
        state[0] = s1.get();
        state[1] = s2.get();
        return state;
    }
}

// Both finishers, in order: what the first emits still has to go through the second.
final class CompositeFinisher<T, R, RR> implements BiConsumer<Object[], Gatherer.Downstream<? super RR>> {

    private final Gatherer<T, Object, R> first;
    private final Gatherer<R, Object, RR> second;

    CompositeFinisher(Gatherer<T, Object, R> first, Gatherer<R, Object, RR> second) {
        this.first = first;
        this.second = second;
    }

    public void accept(Object[] state, Gatherer.Downstream<? super RR> downstream) {
        Gatherer.Downstream<RR> down = (Gatherer.Downstream<RR>) downstream;
        Gatherer.Integrator<Object, R, RR> i2 = this.second.integrator();
        MidDownstream<R, RR> mid = new MidDownstream<R, RR>(i2, state, down);
        BiConsumer<Object, Gatherer.Downstream<? super R>> f1 = this.first.finisher();
        Gatherer.Downstream<? super R> midTarget = mid;
        f1.accept(state[0], midTarget);
        BiConsumer<Object, Gatherer.Downstream<? super RR>> f2 = this.second.finisher();
        f2.accept(state[1], downstream);
    }
}

final class CompositeGatherer<T, R, RR> implements Gatherer<T, Object[], RR> {

    private final Gatherer<T, Object, R> first;
    private final Gatherer<R, Object, RR> second;

    CompositeGatherer(Gatherer<T, Object, R> first, Gatherer<R, Object, RR> second) {
        this.first = first;
        this.second = second;
    }

    public Supplier<Object[]> initializer() {
        return new CompositeInitializer<T, R, RR>(this.first, this.second);
    }

    public Gatherer.Integrator<Object[], T, RR> integrator() {
        return new CompositeIntegrator<T, R, RR>(this.first, this.second);
    }

    // A composition does not know how to merge: it would take merging each side's two halves, and
    // either one's combiner may be the one that refuses. The composition refuses.
    public BinaryOperator<Object[]> combiner() {
        return new NoCombiner<Object[]>();
    }

    public BiConsumer<Object[], Gatherer.Downstream<? super RR>> finisher() {
        return new CompositeFinisher<T, R, RR>(this.first, this.second);
    }
}
