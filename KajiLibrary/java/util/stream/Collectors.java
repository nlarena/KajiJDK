package java.util.stream;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.Iterator;
import java.util.function.Supplier;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.function.ToDoubleFunction;

// KajiLibrary's java.util.stream.Collectors — factories for the common Collectors that
// Stream.collect uses. Each builds a CollectorImpl (same-file, below) from four functions.
//
// The four component functions used to be lambdas. They are now named same-file classes, because
// a lambda reached through a *field* of another object does not execute correctly on our VM: the
// call appears to succeed but the body never runs (repro in the defect notes — take
// `Collectors.toList().accumulator().accept(list, "x")` and observe that the list stays empty;
// with the lambda replaced by a named class the same call works). Named classes also sidestep the
// old finding-#16 dance of binding every lambda to an explicitly-typed local first. Every
// collector added since follows that rule: NO lambdas anywhere in this file.
//
// Two more house rules that come straight out of the defect list:
//
//   * every mutable accumulator container is a plain array (`Object[]`, `int[]`, `long[]`,
//     `double[]`) or a JDK container we already have. Arrays avoid cross-class field access on a
//     same-file helper type, and they are what CountAccumulator already used;
//   * a `BinaryOperator`/`UnaryOperator` is widened to `BiFunction`/`Function` before `apply` is
//     called — `apply` is inherited from a *generic* superinterface, which this javac does not
//     resolve through (finding #15).
//
// The mutable containers stay typed as the concrete `ArrayList`/`HashSet` (not `List`/`Set`) so
// `add` resolves directly rather than through a generic superinterface (finding #15). The
// `combiner` is a no-op merge — our eager sequential collect never calls it. Compiled with `-cp`
// so `Collector`/`List`/`Set` bind to our own subset.
//
// Not here, and why:
//
//   * `summarizingInt/Long/Double` need java.util.IntSummaryStatistics & friends, which
//     KajiLibrary does not have and which live outside this package;
//   * `toConcurrentMap` / `groupingByConcurrent` need java.util.concurrent.ConcurrentMap, idem;
//   * `Collector.of(...)` and `characteristics()` — see Collector.java;
//   * the private JDK internals (`mapMerger`, `castingIdentity`, `CH_ID`, …) are HotSpot's own
//     plumbing for a lazy pipeline we do not have.
public final class Collectors {

    private Collectors() {}

    // ---- into a container ------------------------------------------------------------------

    // Accumulate the elements into a List (an ArrayList).
    public static <T> Collector<T, ?, List<T>> toList() {
        return new CollectorImpl<T, ArrayList<T>, List<T>>(new ListSupplier<T>(), new ListAccumulator<T>(),
                new KeepFirst<ArrayList<T>>(), new ListFinisher<T>());
    }

    // Accumulate the elements into a Set (a HashSet), dropping duplicates.
    public static <T> Collector<T, ?, Set<T>> toSet() {
        return new CollectorImpl<T, HashSet<T>, Set<T>>(new SetSupplier<T>(), new SetAccumulator<T>(),
                new KeepFirst<HashSet<T>>(), new SetFinisher<T>());
    }

    // Accumulate the elements into a caller-chosen Collection.
    public static <T, C extends Collection<T>> Collector<T, ?, C> toCollection(Supplier<C> collectionFactory) {
        Supplier<Collection<T>> sup = new CollSupplier<T, C>(collectionFactory);
        return new CollectorImpl<T, Collection<T>, C>(sup, new CollAccumulator<T>(),
                new KeepFirst<Collection<T>>(), new CollFinisher<T, C>());
    }

    // Accumulate into a List that really refuses mutation. java.util.Collections in KajiLibrary
    // has no `unmodifiableList`, so the immutable view is a same-file class (Unmodifiable*, at
    // the bottom of this file) rather than a wrapper borrowed from java.util. Every mutator
    // throws UnsupportedOperationException, as the JDK's List.of() does.
    public static <T> Collector<T, ?, List<T>> toUnmodifiableList() {
        return new CollectorImpl<T, ArrayList<T>, List<T>>(new ListSupplier<T>(), new ListAccumulator<T>(),
                new KeepFirst<ArrayList<T>>(), new FrozenListFinisher<T>());
    }

    // …and the Set equivalent.
    public static <T> Collector<T, ?, Set<T>> toUnmodifiableSet() {
        return new CollectorImpl<T, HashSet<T>, Set<T>>(new SetSupplier<T>(), new SetAccumulator<T>(),
                new KeepFirst<HashSet<T>>(), new FrozenSetFinisher<T>());
    }

    // ---- joining ---------------------------------------------------------------------------

    // Concatenate the elements' characters into one String.
    public static Collector<CharSequence, ?, String> joining() {
        return new CollectorImpl<CharSequence, StringBuilder, String>(new SbSupplier(), new SbAccumulator(),
                new KeepFirst<StringBuilder>(), new SbFinisher());
    }

    // …separated by `delimiter`.
    public static Collector<CharSequence, ?, String> joining(CharSequence delimiter) {
        return Collectors.joining(delimiter, "", "");
    }

    // …separated by `delimiter`, wrapped in `prefix`/`suffix`.
    public static Collector<CharSequence, ?, String> joining(CharSequence delimiter, CharSequence prefix,
                                                             CharSequence suffix) {
        return new CollectorImpl<CharSequence, StringJoiner, String>(
                new JoinSupplier(delimiter, prefix, suffix), new JoinAccumulator(),
                new KeepFirst<StringJoiner>(), new JoinFinisher());
    }

    // ---- counting, summing, averaging ------------------------------------------------------

    // Count the elements.
    public static <T> Collector<T, ?, Long> counting() {
        return new CollectorImpl<T, long[], Long>(new CountSupplier(), new CountAccumulator<T>(),
                new KeepFirst<long[]>(), new CountFinisher());
    }

    public static <T> Collector<T, ?, Integer> summingInt(ToIntFunction<T> mapper) {
        return new CollectorImpl<T, int[], Integer>(new IntArraySupplier(1), new SumIntAccumulator<T>(mapper),
                new KeepFirst<int[]>(), new SumIntFinisher());
    }

    public static <T> Collector<T, ?, Long> summingLong(ToLongFunction<T> mapper) {
        return new CollectorImpl<T, long[], Long>(new LongArraySupplier(1), new SumLongAccumulator<T>(mapper),
                new KeepFirst<long[]>(), new SumLongFinisher());
    }

    public static <T> Collector<T, ?, Double> summingDouble(ToDoubleFunction<T> mapper) {
        return new CollectorImpl<T, double[], Double>(new DoubleArraySupplier(1),
                new SumDoubleAccumulator<T>(mapper), new KeepFirst<double[]>(), new SumDoubleFinisher());
    }

    // The accumulator for the two integral averages is a long[2]: {sum, count}. An empty stream
    // averages to 0.0, as in the JDK.
    public static <T> Collector<T, ?, Double> averagingInt(ToIntFunction<T> mapper) {
        return new CollectorImpl<T, long[], Double>(new LongArraySupplier(2), new AvgIntAccumulator<T>(mapper),
                new KeepFirst<long[]>(), new AvgLongFinisher());
    }

    public static <T> Collector<T, ?, Double> averagingLong(ToLongFunction<T> mapper) {
        return new CollectorImpl<T, long[], Double>(new LongArraySupplier(2), new AvgLongAccumulator<T>(mapper),
                new KeepFirst<long[]>(), new AvgLongFinisher());
    }

    public static <T> Collector<T, ?, Double> averagingDouble(ToDoubleFunction<T> mapper) {
        return new CollectorImpl<T, double[], Double>(new DoubleArraySupplier(2),
                new AvgDoubleAccumulator<T>(mapper), new KeepFirst<double[]>(), new AvgDoubleFinisher());
    }

    // ---- reduction -------------------------------------------------------------------------

    // The smallest element by `comparator`, or an empty Optional for an empty stream.
    public static <T> Collector<T, ?, Optional<T>> minBy(Comparator<T> comparator) {
        return new CollectorImpl<T, Object[], Optional<T>>(new Box2Supplier(),
                new ExtremumAccumulator<T>(comparator, false), new KeepFirst<Object[]>(),
                new OptionalFinisher<T>());
    }

    // The largest element by `comparator`, or an empty Optional for an empty stream.
    public static <T> Collector<T, ?, Optional<T>> maxBy(Comparator<T> comparator) {
        return new CollectorImpl<T, Object[], Optional<T>>(new Box2Supplier(),
                new ExtremumAccumulator<T>(comparator, true), new KeepFirst<Object[]>(),
                new OptionalFinisher<T>());
    }

    // Fold with no identity: empty stream gives an empty Optional.
    public static <T> Collector<T, ?, Optional<T>> reducing(BinaryOperator<T> op) {
        return new CollectorImpl<T, Object[], Optional<T>>(new Box2Supplier(), new ReduceAccumulator<T>(op),
                new KeepFirst<Object[]>(), new OptionalFinisher<T>());
    }

    // Fold from `identity`.
    public static <T> Collector<T, ?, T> reducing(T identity, BinaryOperator<T> op) {
        return new CollectorImpl<T, Object[], T>(new SeedBoxSupplier(identity), new SeedReduceAccumulator<T>(op),
                new KeepFirst<Object[]>(), new ValueFinisher<T>());
    }

    // Map, then fold from `identity`.
    public static <T, U> Collector<T, ?, U> reducing(U identity, Function<T, U> mapper, BinaryOperator<U> op) {
        return new CollectorImpl<T, Object[], U>(new SeedBoxSupplier(identity),
                new MapReduceAccumulator<T, U>(mapper, op), new KeepFirst<Object[]>(), new ValueFinisher<U>());
    }

    // ---- adapting another collector ---------------------------------------------------------

    // Apply `mapper` to each element before handing it to `downstream`.
    public static <T, U, A, R> Collector<T, A, R> mapping(Function<T, U> mapper, Collector<U, A, R> downstream) {
        // Each accessor result is bound to a local: chaining a call onto the result of a call
        // into a classpath type mis-compiles (see the defect notes). Same in every method below.
        Supplier<A> sup = downstream.supplier();
        BiConsumer<A, U> acc = downstream.accumulator();
        BinaryOperator<A> comb = downstream.combiner();
        Function<A, R> fin = downstream.finisher();
        return new CollectorImpl<T, A, R>(sup, new MappingAccumulator<T, U, A>(mapper, acc), comb, fin);
    }

    // Map each element to a Stream and hand every element of it to `downstream`.
    //
    // The `? extends Stream<? extends U>` is the JDK's own spelling and it is LOAD-BEARING: with
    // the invariant `Function<T, Stream<U>>` the declaration still compiles, but every CALL to it
    // is silently dropped by this javac — `Collectors.flatMapping(m, down)` emits `aload m;
    // aload down; astore <target>` with no invokestatic, so the caller silently gets `down`
    // instead of the flat-mapping collector, and the mapper is left dangling on the operand
    // stack. Wildcarding the nested type argument makes the call resolve. Repro in the notes.
    public static <T, U, A, R> Collector<T, A, R> flatMapping(Function<T, ? extends Stream<? extends U>> mapper,
                                                              Collector<U, A, R> downstream) {
        Supplier<A> sup = downstream.supplier();
        BiConsumer<A, U> acc = downstream.accumulator();
        BinaryOperator<A> comb = downstream.combiner();
        Function<A, R> fin = downstream.finisher();
        Function<T, Stream<U>> m = (Function<T, Stream<U>>) mapper;
        return new CollectorImpl<T, A, R>(sup, new FlatMappingAccumulator<T, U, A>(m, acc), comb, fin);
    }

    // Only hand `downstream` the elements that satisfy `predicate`.
    public static <T, A, R> Collector<T, A, R> filtering(Predicate<T> predicate, Collector<T, A, R> downstream) {
        Supplier<A> sup = downstream.supplier();
        BiConsumer<A, T> acc = downstream.accumulator();
        BinaryOperator<A> comb = downstream.combiner();
        Function<A, R> fin = downstream.finisher();
        return new CollectorImpl<T, A, R>(sup, new FilteringAccumulator<T, A>(predicate, acc), comb, fin);
    }

    // Run `downstream`, then push its result through `finisher`.
    public static <T, A, R, RR> Collector<T, A, RR> collectingAndThen(Collector<T, A, R> downstream,
                                                                      Function<R, RR> finisher) {
        Supplier<A> sup = downstream.supplier();
        BiConsumer<A, T> acc = downstream.accumulator();
        BinaryOperator<A> comb = downstream.combiner();
        Function<A, R> fin = downstream.finisher();
        return new CollectorImpl<T, A, RR>(sup, acc, comb, new AndThenFinisher<A, R, RR>(fin, finisher));
    }

    // ---- into a Map ---------------------------------------------------------------------------

    // Key/value pairs into a HashMap. A duplicate key is an IllegalStateException, as in the JDK.
    public static <T, K, V> Collector<T, ?, Map<K, V>> toMap(Function<T, K> keyMapper, Function<T, V> valueMapper) {
        // The type arguments are written out: this javac cannot INFER them for a call made from
        // inside a generic method (it reports "restricciones de tipo incompatibles" even when the
        // only solution is A := A). Naming them explicitly is the workaround, and it is used at
        // every same-class delegation in this file. Repro in the defect notes.
        BinaryOperator<V> merge = new ThrowingMerger<V>();
        return Collectors.<T, K, V>toMap(keyMapper, valueMapper, merge);
    }

    // …with `mergeFunction` resolving duplicate keys.
    public static <T, K, V> Collector<T, ?, Map<K, V>> toMap(Function<T, K> keyMapper, Function<T, V> valueMapper,
                                                             BinaryOperator<V> mergeFunction) {
        return new CollectorImpl<T, Map<K, V>, Map<K, V>>(new HashMapSupplier<K, V>(),
                new MapAccumulator<T, K, V>(keyMapper, valueMapper, mergeFunction),
                new KeepFirst<Map<K, V>>(), new MapIdentityFinisher<K, V>());
    }

    // …into a caller-chosen Map: `mapFactory` supplies the accumulator itself, so the pairs land
    // straight in the caller's map and the finisher is just the cast back to M.
    public static <T, K, V, M extends Map<K, V>> Collector<T, ?, M> toMap(Function<T, K> keyMapper,
                                                                          Function<T, V> valueMapper,
                                                                          BinaryOperator<V> mergeFunction,
                                                                          Supplier<M> mapFactory) {
        Supplier<Map<K, V>> sup = new MapFactorySupplier<K, V, M>(mapFactory);
        return new CollectorImpl<T, Map<K, V>, M>(sup,
                new MapAccumulator<T, K, V>(keyMapper, valueMapper, mergeFunction),
                new KeepFirst<Map<K, V>>(), new MapCastFinisher<K, V, M>());
    }

    // Key/value pairs into a Map that refuses mutation. A duplicate key is an
    // IllegalStateException, exactly as in the JDK.
    public static <T, K, V> Collector<T, ?, Map<K, V>> toUnmodifiableMap(Function<T, K> keyMapper,
                                                                         Function<T, V> valueMapper) {
        BinaryOperator<V> merge = new ThrowingMerger<V>();
        return Collectors.<T, K, V>toUnmodifiableMap(keyMapper, valueMapper, merge);
    }

    // …with `mergeFunction` resolving duplicate keys.
    public static <T, K, V> Collector<T, ?, Map<K, V>> toUnmodifiableMap(Function<T, K> keyMapper,
                                                                         Function<T, V> valueMapper,
                                                                         BinaryOperator<V> mergeFunction) {
        return new CollectorImpl<T, Map<K, V>, Map<K, V>>(new HashMapSupplier<K, V>(),
                new MapAccumulator<T, K, V>(keyMapper, valueMapper, mergeFunction),
                new KeepFirst<Map<K, V>>(), new FrozenMapFinisher<K, V>());
    }

    // ---- grouping and partitioning ------------------------------------------------------------

    // Group into Map<K, List<T>> by `classifier`.
    public static <T, K> Collector<T, ?, Map<K, List<T>>> groupingBy(Function<T, K> classifier) {
        // toList()'s pieces spelled out instead of `Collectors.toList()`: see the note in toMap.
        Supplier<ArrayList<T>> sup = new ListSupplier<T>();
        BiConsumer<ArrayList<T>, T> acc = new ListAccumulator<T>();
        Function<ArrayList<T>, List<T>> fin = new ListFinisher<T>();
        return new CollectorImpl<T, Object[], Map<K, List<T>>>(new GroupSupplier(),
                new GroupAccumulator<T, K, ArrayList<T>>(classifier, sup, acc), new KeepFirst<Object[]>(),
                new GroupFinisher<K, ArrayList<T>, List<T>>(fin));
    }

    // Group by `classifier`, reducing each group with `downstream`.
    public static <T, K, A, D> Collector<T, ?, Map<K, D>> groupingBy(Function<T, K> classifier,
                                                                     Collector<T, A, D> downstream) {
        Supplier<A> sup = downstream.supplier();
        BiConsumer<A, T> acc = downstream.accumulator();
        Function<A, D> fin = downstream.finisher();
        return new CollectorImpl<T, Object[], Map<K, D>>(new GroupSupplier(),
                new GroupAccumulator<T, K, A>(classifier, sup, acc), new KeepFirst<Object[]>(),
                new GroupFinisher<K, A, D>(fin));
    }

    // …with the result Map coming from `mapFactory` (filled at finish time; see toMap above).
    public static <T, K, A, D, M extends Map<K, D>> Collector<T, ?, M> groupingBy(Function<T, K> classifier,
                                                                                   Supplier<M> mapFactory,
                                                                                   Collector<T, A, D> downstream) {
        Supplier<A> sup = downstream.supplier();
        BiConsumer<A, T> acc = downstream.accumulator();
        Function<A, D> fin = downstream.finisher();
        return new CollectorImpl<T, Object[], M>(new GroupSupplier(),
                new GroupAccumulator<T, K, A>(classifier, sup, acc), new KeepFirst<Object[]>(),
                new GroupIntoFinisher<K, A, D, M>(fin, mapFactory));
    }

    // Split in two by `predicate`: the result Map always has both FALSE and TRUE keys.
    public static <T> Collector<T, ?, Map<Boolean, List<T>>> partitioningBy(Predicate<T> predicate) {
        Supplier<ArrayList<T>> sup = new ListSupplier<T>();
        BiConsumer<ArrayList<T>, T> acc = new ListAccumulator<T>();
        Function<ArrayList<T>, List<T>> fin = new ListFinisher<T>();
        return new CollectorImpl<T, Object[], Map<Boolean, List<T>>>(new PairSupplier<ArrayList<T>>(sup),
                new PartitionAccumulator<T, ArrayList<T>>(predicate, acc), new KeepFirst<Object[]>(),
                new PartitionFinisher<ArrayList<T>, List<T>>(fin));
    }

    // …reducing each half with `downstream`.
    public static <T, A, D> Collector<T, ?, Map<Boolean, D>> partitioningBy(Predicate<T> predicate,
                                                                            Collector<T, A, D> downstream) {
        Supplier<A> sup = downstream.supplier();
        BiConsumer<A, T> acc = downstream.accumulator();
        Function<A, D> fin = downstream.finisher();
        return new CollectorImpl<T, Object[], Map<Boolean, D>>(new PairSupplier<A>(sup),
                new PartitionAccumulator<T, A>(predicate, acc), new KeepFirst<Object[]>(),
                new PartitionFinisher<A, D>(fin));
    }

    // ---- teeing ---------------------------------------------------------------------------------

    // Feed every element to BOTH collectors, then merge their two results. A1/A2 are spelled out
    // where the JDK writes `Collector<T, ?, R1>`: a type variable that only appears behind a `?`
    // cannot be inferred here. The erasure is identical either way.
    public static <T, A1, R1, A2, R2, R> Collector<T, ?, R> teeing(Collector<T, A1, R1> first,
                                                                   Collector<T, A2, R2> second,
                                                                   BiFunction<R1, R2, R> merger) {
        return Collectors.<T, A1, R1, A2, R2, R>teeing0(first, second, merger);
    }

    // Where the assembly happens; the JDK splits it the same way.
    private static <T, A1, R1, A2, R2, R> Collector<T, ?, R> teeing0(Collector<T, A1, R1> first,
                                                                     Collector<T, A2, R2> second,
                                                                     BiFunction<R1, R2, R> merger) {
        Supplier<A1> sup1 = first.supplier();
        BiConsumer<A1, T> acc1 = first.accumulator();
        Function<A1, R1> fin1 = first.finisher();
        Supplier<A2> sup2 = second.supplier();
        BiConsumer<A2, T> acc2 = second.accumulator();
        Function<A2, R2> fin2 = second.finisher();
        return new CollectorImpl<T, Object[], R>(new TeeSupplier<A1, A2>(sup1, sup2),
                new TeeAccumulator<T, A1, A2>(acc1, acc2), new KeepFirst<Object[]>(),
                new TeeFinisher<A1, R1, A2, R2, R>(fin1, fin2, merger));
    }
}

// ---- the component functions, as named classes (see the file header for why) ---------------

// Shared by every collector here: a combiner that keeps the left container. Never invoked, since
// our collect() is sequential and never splits.
final class KeepFirst<A> implements BinaryOperator<A> {
    public A apply(A a, A b) {
        return a;
    }
}

final class ListSupplier<T> implements Supplier<ArrayList<T>> {
    public ArrayList<T> get() {
        return new ArrayList<T>();
    }
}

final class ListAccumulator<T> implements BiConsumer<ArrayList<T>, T> {
    public void accept(ArrayList<T> list, T item) {
        list.add(item);
    }
}

final class ListFinisher<T> implements Function<ArrayList<T>, List<T>> {
    public List<T> apply(ArrayList<T> list) {
        return list;
    }
}

final class SetSupplier<T> implements Supplier<HashSet<T>> {
    public HashSet<T> get() {
        return new HashSet<T>();
    }
}

final class SetAccumulator<T> implements BiConsumer<HashSet<T>, T> {
    public void accept(HashSet<T> set, T item) {
        set.add(item);
    }
}

final class SetFinisher<T> implements Function<HashSet<T>, Set<T>> {
    public Set<T> apply(HashSet<T> set) {
        return set;
    }
}

// toCollection: the caller's factory, adapted to the Collection<T> the accumulator wants.
final class CollSupplier<T, C extends Collection<T>> implements Supplier<Collection<T>> {

    private final Supplier<C> factory;

    CollSupplier(Supplier<C> factory) {
        this.factory = factory;
    }

    public Collection<T> get() {
        return this.factory.get();
    }
}

final class CollAccumulator<T> implements BiConsumer<Collection<T>, T> {
    public void accept(Collection<T> c, T item) {
        c.add(item);
    }
}

final class CollFinisher<T, C extends Collection<T>> implements Function<Collection<T>, C> {
    public C apply(Collection<T> c) {
        return (C) c;
    }
}

final class SbSupplier implements Supplier<StringBuilder> {
    public StringBuilder get() {
        return new StringBuilder();
    }
}

final class SbAccumulator implements BiConsumer<StringBuilder, CharSequence> {
    public void accept(StringBuilder sb, CharSequence cs) {
        sb.append(cs);
    }
}

final class SbFinisher implements Function<StringBuilder, String> {
    public String apply(StringBuilder sb) {
        return sb.toString();
    }
}

final class JoinSupplier implements Supplier<StringJoiner> {

    private final CharSequence delimiter;
    private final CharSequence prefix;
    private final CharSequence suffix;

    JoinSupplier(CharSequence delimiter, CharSequence prefix, CharSequence suffix) {
        this.delimiter = delimiter;
        this.prefix = prefix;
        this.suffix = suffix;
    }

    public StringJoiner get() {
        return new StringJoiner(this.delimiter, this.prefix, this.suffix);
    }
}

final class JoinAccumulator implements BiConsumer<StringJoiner, CharSequence> {
    public void accept(StringJoiner sj, CharSequence cs) {
        sj.add(cs);
    }
}

final class JoinFinisher implements Function<StringJoiner, String> {
    public String apply(StringJoiner sj) {
        return sj.toString();
    }
}

final class CountSupplier implements Supplier<long[]> {
    public long[] get() {
        return new long[1];
    }
}

final class CountAccumulator<T> implements BiConsumer<long[], T> {
    public void accept(long[] box, T item) {
        box[0] = box[0] + 1L;
    }
}

final class CountFinisher implements Function<long[], Long> {
    public Long apply(long[] box) {
        return Long.valueOf(box[0]);
    }
}

// ---- numeric boxes ---------------------------------------------------------------------------

final class IntArraySupplier implements Supplier<int[]> {

    private final int n;

    IntArraySupplier(int n) {
        this.n = n;
    }

    public int[] get() {
        return new int[this.n];
    }
}

final class LongArraySupplier implements Supplier<long[]> {

    private final int n;

    LongArraySupplier(int n) {
        this.n = n;
    }

    public long[] get() {
        return new long[this.n];
    }
}

final class DoubleArraySupplier implements Supplier<double[]> {

    private final int n;

    DoubleArraySupplier(int n) {
        this.n = n;
    }

    public double[] get() {
        return new double[this.n];
    }
}

final class SumIntAccumulator<T> implements BiConsumer<int[], T> {

    private final ToIntFunction<T> mapper;

    SumIntAccumulator(ToIntFunction<T> mapper) {
        this.mapper = mapper;
    }

    public void accept(int[] box, T item) {
        box[0] = box[0] + this.mapper.applyAsInt(item);
    }
}

final class SumIntFinisher implements Function<int[], Integer> {
    public Integer apply(int[] box) {
        return Integer.valueOf(box[0]);
    }
}

final class SumLongAccumulator<T> implements BiConsumer<long[], T> {

    private final ToLongFunction<T> mapper;

    SumLongAccumulator(ToLongFunction<T> mapper) {
        this.mapper = mapper;
    }

    public void accept(long[] box, T item) {
        box[0] = box[0] + this.mapper.applyAsLong(item);
    }
}

final class SumLongFinisher implements Function<long[], Long> {
    public Long apply(long[] box) {
        return Long.valueOf(box[0]);
    }
}

final class SumDoubleAccumulator<T> implements BiConsumer<double[], T> {

    private final ToDoubleFunction<T> mapper;

    SumDoubleAccumulator(ToDoubleFunction<T> mapper) {
        this.mapper = mapper;
    }

    public void accept(double[] box, T item) {
        box[0] = box[0] + this.mapper.applyAsDouble(item);
    }
}

final class SumDoubleFinisher implements Function<double[], Double> {
    public Double apply(double[] box) {
        return Double.valueOf(box[0]);
    }
}

// {sum, count}. The `(long)`/`(double)` casts are explicit because this javac does not insert
// the widening conversion on its own (finding #217).
final class AvgIntAccumulator<T> implements BiConsumer<long[], T> {

    private final ToIntFunction<T> mapper;

    AvgIntAccumulator(ToIntFunction<T> mapper) {
        this.mapper = mapper;
    }

    public void accept(long[] box, T item) {
        int v = this.mapper.applyAsInt(item);
        box[0] = box[0] + (long) v;
        box[1] = box[1] + 1L;
    }
}

final class AvgLongAccumulator<T> implements BiConsumer<long[], T> {

    private final ToLongFunction<T> mapper;

    AvgLongAccumulator(ToLongFunction<T> mapper) {
        this.mapper = mapper;
    }

    public void accept(long[] box, T item) {
        box[0] = box[0] + this.mapper.applyAsLong(item);
        box[1] = box[1] + 1L;
    }
}

final class AvgLongFinisher implements Function<long[], Double> {
    public Double apply(long[] box) {
        if (box[1] == 0L) {
            return Double.valueOf(0.0);
        }
        double sum = (double) box[0];
        double n = (double) box[1];
        return Double.valueOf(sum / n);
    }
}

final class AvgDoubleAccumulator<T> implements BiConsumer<double[], T> {

    private final ToDoubleFunction<T> mapper;

    AvgDoubleAccumulator(ToDoubleFunction<T> mapper) {
        this.mapper = mapper;
    }

    public void accept(double[] box, T item) {
        box[0] = box[0] + this.mapper.applyAsDouble(item);
        box[1] = box[1] + 1.0;
    }
}

final class AvgDoubleFinisher implements Function<double[], Double> {
    public Double apply(double[] box) {
        if (box[1] == 0.0) {
            return Double.valueOf(0.0);
        }
        return Double.valueOf(box[0] / box[1]);
    }
}

// ---- reduction boxes -------------------------------------------------------------------------

// An Object[2] used as {value, present-marker}. Slot 1 is null until the first element arrives.
//
// The marker is `Boolean.valueOf(true)`, never `Boolean.TRUE`: reading the static field
// `java.lang.Boolean.TRUE` (or FALSE) panics our VM with "field_offset: field not found in the
// class or its superclasses", even though Boolean.class does declare both. `valueOf` returns the
// same shared instances, so the behaviour is identical. Repro in the defect notes; the same
// substitution is used in PartitionFinisher below.
final class Box2Supplier implements Supplier<Object[]> {
    public Object[] get() {
        return new Object[2];
    }
}

// The same box, pre-seeded with an identity (slot 1 marked present from the start).
final class SeedBoxSupplier implements Supplier<Object[]> {

    private final Object seed;

    SeedBoxSupplier(Object seed) {
        this.seed = seed;
    }

    public Object[] get() {
        Object[] box = new Object[2];
        box[0] = this.seed;
        box[1] = Boolean.valueOf(true);
        return box;
    }
}

final class ExtremumAccumulator<T> implements BiConsumer<Object[], T> {

    private final Comparator<T> comparator;
    private final boolean wantMax;

    ExtremumAccumulator(Comparator<T> comparator, boolean wantMax) {
        this.comparator = comparator;
        this.wantMax = wantMax;
    }

    public void accept(Object[] box, T item) {
        if (box[1] == null) {
            box[0] = item;
            box[1] = Boolean.valueOf(true);
            return;
        }
        T best = (T) box[0];
        int c = this.comparator.compare(item, best);
        if (this.wantMax) {
            if (c > 0) {
                box[0] = item;
            }
        } else {
            if (c < 0) {
                box[0] = item;
            }
        }
    }
}

final class ReduceAccumulator<T> implements BiConsumer<Object[], T> {

    // A BinaryOperator, widened: `apply` comes from BiFunction, a generic superinterface (#15).
    private final BiFunction<T, T, T> op;

    ReduceAccumulator(BinaryOperator<T> op) {
        this.op = op;
    }

    public void accept(Object[] box, T item) {
        if (box[1] == null) {
            box[0] = item;
            box[1] = Boolean.valueOf(true);
            return;
        }
        T acc = (T) box[0];
        box[0] = this.op.apply(acc, item);
    }
}

final class SeedReduceAccumulator<T> implements BiConsumer<Object[], T> {

    private final BiFunction<T, T, T> op;

    SeedReduceAccumulator(BinaryOperator<T> op) {
        this.op = op;
    }

    public void accept(Object[] box, T item) {
        T acc = (T) box[0];
        box[0] = this.op.apply(acc, item);
    }
}

final class MapReduceAccumulator<T, U> implements BiConsumer<Object[], T> {

    private final Function<T, U> mapper;
    private final BiFunction<U, U, U> op;

    MapReduceAccumulator(Function<T, U> mapper, BinaryOperator<U> op) {
        this.mapper = mapper;
        this.op = op;
    }

    public void accept(Object[] box, T item) {
        U acc = (U) box[0];
        U mapped = this.mapper.apply(item);
        box[0] = this.op.apply(acc, mapped);
    }
}

final class OptionalFinisher<T> implements Function<Object[], Optional<T>> {
    public Optional<T> apply(Object[] box) {
        if (box[1] == null) {
            return Optional.empty();
        }
        T value = (T) box[0];
        return Optional.of(value);
    }
}

final class ValueFinisher<T> implements Function<Object[], T> {
    public T apply(Object[] box) {
        return (T) box[0];
    }
}

// ---- adapters --------------------------------------------------------------------------------

final class MappingAccumulator<T, U, A> implements BiConsumer<A, T> {

    private final Function<T, U> mapper;
    private final BiConsumer<A, U> downstream;

    MappingAccumulator(Function<T, U> mapper, BiConsumer<A, U> downstream) {
        this.mapper = mapper;
        this.downstream = downstream;
    }

    public void accept(A container, T item) {
        U mapped = this.mapper.apply(item);
        this.downstream.accept(container, mapped);
    }
}

final class FlatMappingAccumulator<T, U, A> implements BiConsumer<A, T> {

    private final Function<T, Stream<U>> mapper;
    private final BiConsumer<A, U> downstream;

    FlatMappingAccumulator(Function<T, Stream<U>> mapper, BiConsumer<A, U> downstream) {
        this.mapper = mapper;
        this.downstream = downstream;
    }

    public void accept(A container, T item) {
        // Bound to a local, not chained: `mapper.apply(item).toArray()` mis-compiles (see notes).
        Stream<U> sub = this.mapper.apply(item);
        if (sub == null) {
            return;
        }
        Object[] arr = sub.toArray();
        for (int i = 0; i < arr.length; i++) {
            U e = (U) arr[i];
            this.downstream.accept(container, e);
        }
    }
}

final class FilteringAccumulator<T, A> implements BiConsumer<A, T> {

    private final Predicate<T> predicate;
    private final BiConsumer<A, T> downstream;

    FilteringAccumulator(Predicate<T> predicate, BiConsumer<A, T> downstream) {
        this.predicate = predicate;
        this.downstream = downstream;
    }

    public void accept(A container, T item) {
        if (this.predicate.test(item)) {
            this.downstream.accept(container, item);
        }
    }
}

final class AndThenFinisher<A, R, RR> implements Function<A, RR> {

    private final Function<A, R> downstream;
    private final Function<R, RR> then;

    AndThenFinisher(Function<A, R> downstream, Function<R, RR> then) {
        this.downstream = downstream;
        this.then = then;
    }

    public RR apply(A container) {
        R intermediate = this.downstream.apply(container);
        return this.then.apply(intermediate);
    }
}

// ---- Map collectors ----------------------------------------------------------------------------

final class HashMapSupplier<K, V> implements Supplier<Map<K, V>> {
    public Map<K, V> get() {
        return new HashMap<K, V>();
    }
}

// The default merge for toMap(k, v): a duplicate key is a programming error.
final class ThrowingMerger<V> implements BinaryOperator<V> {
    public V apply(V a, V b) {
        // A constant message: runtime String concatenation is not available on our VM (#226).
        throw new IllegalStateException("duplicate key");
    }
}

final class MapAccumulator<T, K, V> implements BiConsumer<Map<K, V>, T> {

    private final Function<T, K> keyMapper;
    private final Function<T, V> valueMapper;
    private final BiFunction<V, V, V> merge;

    MapAccumulator(Function<T, K> keyMapper, Function<T, V> valueMapper, BinaryOperator<V> merge) {
        this.keyMapper = keyMapper;
        this.valueMapper = valueMapper;
        this.merge = merge;
    }

    public void accept(Map<K, V> map, T item) {
        K key = this.keyMapper.apply(item);
        V value = this.valueMapper.apply(item);
        if (map.containsKey(key)) {
            V old = map.get(key);
            V merged = this.merge.apply(old, value);
            map.put(key, merged);
            return;
        }
        map.put(key, value);
    }
}

final class MapIdentityFinisher<K, V> implements Function<Map<K, V>, Map<K, V>> {
    public Map<K, V> apply(Map<K, V> map) {
        return map;
    }
}

// The caller's map factory, seen as the plain Supplier<Map<K,V>> the accumulator wants. Filling
// the caller's map directly is what lets toMap(…, mapFactory) work without a copy — KajiLibrary's
// java.util.Map has no keySet()/entrySet(), so copying an arbitrary Map is not even possible.
final class MapFactorySupplier<K, V, M extends Map<K, V>> implements Supplier<Map<K, V>> {

    private final Supplier<M> factory;

    MapFactorySupplier(Supplier<M> factory) {
        this.factory = factory;
    }

    public Map<K, V> get() {
        return this.factory.get();
    }
}

final class MapCastFinisher<K, V, M extends Map<K, V>> implements Function<Map<K, V>, M> {
    public M apply(Map<K, V> map) {
        return (M) map;
    }
}

// ---- grouping ----------------------------------------------------------------------------------

// The grouping container is an Object[2]: {Map<K,Object> groups, ArrayList<K> keysInOrder}. The
// key list exists because KajiLibrary's Map has no keySet()/entrySet(), so the finisher would
// otherwise have no way to walk the groups.
final class GroupSupplier implements Supplier<Object[]> {
    public Object[] get() {
        Object[] box = new Object[2];
        box[0] = new HashMap<Object, Object>();
        box[1] = new ArrayList<Object>();
        return box;
    }
}

final class GroupAccumulator<T, K, A> implements BiConsumer<Object[], T> {

    private final Function<T, K> classifier;
    private final Supplier<A> downstreamSupplier;
    private final BiConsumer<A, T> downstreamAccumulator;

    GroupAccumulator(Function<T, K> classifier, Supplier<A> downstreamSupplier,
                     BiConsumer<A, T> downstreamAccumulator) {
        this.classifier = classifier;
        this.downstreamSupplier = downstreamSupplier;
        this.downstreamAccumulator = downstreamAccumulator;
    }

    public void accept(Object[] box, T item) {
        Map<Object, Object> groups = (Map<Object, Object>) box[0];
        ArrayList<Object> keys = (ArrayList<Object>) box[1];
        K key = this.classifier.apply(item);
        Object container = groups.get(key);
        if (container == null) {
            container = this.downstreamSupplier.get();
            groups.put(key, container);
            keys.add(key);
        }
        A typed = (A) container;
        this.downstreamAccumulator.accept(typed, item);
    }
}

final class GroupFinisher<K, A, D> implements Function<Object[], Map<K, D>> {

    private final Function<A, D> downstreamFinisher;

    GroupFinisher(Function<A, D> downstreamFinisher) {
        this.downstreamFinisher = downstreamFinisher;
    }

    public Map<K, D> apply(Object[] box) {
        Map<Object, Object> groups = (Map<Object, Object>) box[0];
        ArrayList<Object> keys = (ArrayList<Object>) box[1];
        Map<K, D> out = new HashMap<K, D>();
        int n = keys.size();
        for (int i = 0; i < n; i++) {
            Object key = keys.get(i);
            Object container = groups.get(key);
            A typed = (A) container;
            D finished = this.downstreamFinisher.apply(typed);
            K typedKey = (K) key;
            out.put(typedKey, finished);
        }
        return out;
    }
}

// Same, but the result map comes from the caller's factory.
final class GroupIntoFinisher<K, A, D, M extends Map<K, D>> implements Function<Object[], M> {

    private final Function<A, D> downstreamFinisher;
    private final Supplier<M> factory;

    GroupIntoFinisher(Function<A, D> downstreamFinisher, Supplier<M> factory) {
        this.downstreamFinisher = downstreamFinisher;
        this.factory = factory;
    }

    public M apply(Object[] box) {
        Map<Object, Object> groups = (Map<Object, Object>) box[0];
        ArrayList<Object> keys = (ArrayList<Object>) box[1];
        M out = this.factory.get();
        Map<K, D> asMap = out;
        int n = keys.size();
        for (int i = 0; i < n; i++) {
            Object key = keys.get(i);
            Object container = groups.get(key);
            A typed = (A) container;
            D finished = this.downstreamFinisher.apply(typed);
            K typedKey = (K) key;
            asMap.put(typedKey, finished);
        }
        return out;
    }
}

// ---- partitioning ---------------------------------------------------------------------------

// An Object[2]: {false-half container, true-half container}. Both halves are created up front so
// that the finisher always produces a two-entry Map, as the JDK guarantees.
final class PairSupplier<A> implements Supplier<Object[]> {

    private final Supplier<A> downstream;

    PairSupplier(Supplier<A> downstream) {
        this.downstream = downstream;
    }

    public Object[] get() {
        Object[] box = new Object[2];
        box[0] = this.downstream.get();
        box[1] = this.downstream.get();
        return box;
    }
}

final class PartitionAccumulator<T, A> implements BiConsumer<Object[], T> {

    private final Predicate<T> predicate;
    private final BiConsumer<A, T> downstream;

    PartitionAccumulator(Predicate<T> predicate, BiConsumer<A, T> downstream) {
        this.predicate = predicate;
        this.downstream = downstream;
    }

    public void accept(Object[] box, T item) {
        int slot = 0;
        if (this.predicate.test(item)) {
            slot = 1;
        }
        A typed = (A) box[slot];
        this.downstream.accept(typed, item);
    }
}

final class PartitionFinisher<A, D> implements Function<Object[], Map<Boolean, D>> {

    private final Function<A, D> downstream;

    PartitionFinisher(Function<A, D> downstream) {
        this.downstream = downstream;
    }

    public Map<Boolean, D> apply(Object[] box) {
        A left = (A) box[0];
        A right = (A) box[1];
        D falseHalf = this.downstream.apply(left);
        D trueHalf = this.downstream.apply(right);
        Map<Boolean, D> out = new HashMap<Boolean, D>();
        out.put(Boolean.valueOf(false), falseHalf);
        out.put(Boolean.valueOf(true), trueHalf);
        return out;
    }
}

// ---- teeing ------------------------------------------------------------------------------------

final class TeeSupplier<A1, A2> implements Supplier<Object[]> {

    private final Supplier<A1> first;
    private final Supplier<A2> second;

    TeeSupplier(Supplier<A1> first, Supplier<A2> second) {
        this.first = first;
        this.second = second;
    }

    public Object[] get() {
        Object[] box = new Object[2];
        box[0] = this.first.get();
        box[1] = this.second.get();
        return box;
    }
}

final class TeeAccumulator<T, A1, A2> implements BiConsumer<Object[], T> {

    private final BiConsumer<A1, T> first;
    private final BiConsumer<A2, T> second;

    TeeAccumulator(BiConsumer<A1, T> first, BiConsumer<A2, T> second) {
        this.first = first;
        this.second = second;
    }

    public void accept(Object[] box, T item) {
        A1 left = (A1) box[0];
        A2 right = (A2) box[1];
        this.first.accept(left, item);
        this.second.accept(right, item);
    }
}

final class TeeFinisher<A1, R1, A2, R2, R> implements Function<Object[], R> {

    private final Function<A1, R1> first;
    private final Function<A2, R2> second;
    private final BiFunction<R1, R2, R> merger;

    TeeFinisher(Function<A1, R1> first, Function<A2, R2> second, BiFunction<R1, R2, R> merger) {
        this.first = first;
        this.second = second;
        this.merger = merger;
    }

    public R apply(Object[] box) {
        A1 left = (A1) box[0];
        A2 right = (A2) box[1];
        R1 a = this.first.apply(left);
        R2 b = this.second.apply(right);
        return this.merger.apply(a, b);
    }
}

// ---- the immutable views handed out by toUnmodifiable* -------------------------------------

final class FrozenListFinisher<T> implements Function<ArrayList<T>, List<T>> {
    public List<T> apply(ArrayList<T> list) {
        return new FrozenList<T>(list);
    }
}

final class FrozenSetFinisher<T> implements Function<HashSet<T>, Set<T>> {
    public Set<T> apply(HashSet<T> set) {
        return new FrozenSet<T>(set);
    }
}

final class FrozenMapFinisher<K, V> implements Function<Map<K, V>, Map<K, V>> {
    public Map<K, V> apply(Map<K, V> map) {
        return new FrozenMap<K, V>(map);
    }
}

// An unmodifiable view over a collection nobody else holds a reference to (the accumulator is
// created by the collector's own supplier and dropped after the finisher runs), so this is a
// genuinely immutable result, not just a read-only window on someone else's mutable state.
// Every mutator throws, exactly like the JDK's List.of()/Set.of()/Map.of().
final class FrozenList<E> extends java.util.AbstractList<E> implements List<E> {

    private final List<E> backing;

    FrozenList(List<E> backing) {
        this.backing = backing;
    }

    public int size() {
        return this.backing.size();
    }

    public boolean isEmpty() {
        return this.backing.isEmpty();
    }

    public boolean contains(Object o) {
        return this.backing.contains(o);
    }

    public E get(int index) {
        return this.backing.get(index);
    }

    public int indexOf(Object o) {
        return this.backing.indexOf(o);
    }

    public Iterator<E> iterator() {
        return this.backing.iterator();
    }

    public boolean add(E e) {
        throw new UnsupportedOperationException();
    }

    public void add(int index, E e) {
        throw new UnsupportedOperationException();
    }

    public E set(int index, E e) {
        throw new UnsupportedOperationException();
    }

    public E remove(int index) {
        throw new UnsupportedOperationException();
    }

    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }
}

final class FrozenSet<E> extends java.util.AbstractSet<E> implements Set<E> {

    private final Set<E> backing;

    FrozenSet(Set<E> backing) {
        this.backing = backing;
    }

    public int size() {
        return this.backing.size();
    }

    public boolean isEmpty() {
        return this.backing.isEmpty();
    }

    public boolean contains(Object o) {
        return this.backing.contains(o);
    }

    public Iterator<E> iterator() {
        return this.backing.iterator();
    }

    public boolean add(E e) {
        throw new UnsupportedOperationException();
    }

    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }
}

final class FrozenMap<K, V> implements Map<K, V> {

    private final Map<K, V> backing;

    FrozenMap(Map<K, V> backing) {
        this.backing = backing;
    }

    public int size() {
        return this.backing.size();
    }

    public boolean isEmpty() {
        return this.backing.isEmpty();
    }

    public boolean containsKey(Object key) {
        return this.backing.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return this.backing.containsValue(value);
    }

    public V get(Object key) {
        return this.backing.get(key);
    }

    // Lectura: delega (finding #205). El Set que devuelve el backing ya es una copia, asi que
    // mutarlo no toca este mapa — que es justo lo que un mapa congelado quiere.
    public Set<K> keySet() {
        return this.backing.keySet();
    }

    // Mutador: tira, como todos los demas de esta clase y como los `Map.of()` del JDK.
    public void putAll(Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException();
    }

    public V put(K key, V value) {
        throw new UnsupportedOperationException();
    }

    public V remove(Object key) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    /**
     * Los valores de este mapa.
     *
     * <p>**Divergencia deliberada**, la misma que ya declara `keySet()`: la del JDK es una *vista*
     * respaldada por el mapa; esta es una copia sacada en el momento. Y a diferencia de `keySet()`
     * es una `Collection` y no un `Set`, porque los valores **si** pueden repetirse.
     */
    public java.util.Collection<V> values() {
        java.util.ArrayList<V> out = new java.util.ArrayList<V>();
        java.util.Iterator<K> it = this.keySet().iterator();
        while (it.hasNext()) {
            out.add(this.get(it.next()));
        }
        return out;
    }

    /**
     * Los pares de este mapa.
     *
     * <p>Misma divergencia que `values()`: copia, no vista. Los pares que devuelve son inmutables,
     * asi que `setValue` sobre uno de ellos lanza en vez de escribir en el mapa — que es lo
     * coherente con que sea una copia: escribir en un par que nadie mira seria peor que negarse.
     */
    public java.util.Set<java.util.Map.Entry<K, V>> entrySet() {
        java.util.HashSet<java.util.Map.Entry<K, V>> out =
            new java.util.HashSet<java.util.Map.Entry<K, V>>();
        java.util.Iterator<K> it = this.keySet().iterator();
        while (it.hasNext()) {
            K k = it.next();
            java.util.Map.Entry<K, V> e = Map.entry(k, this.get(k));   // #285: el
            out.add(e);                                               // local nombra el tipo
        }
        return out;
    }
}

// A Collector assembled from its four component functions.
final class CollectorImpl<T, A, R> implements Collector<T, A, R> {

    private final Supplier<A> supplier;
    private final BiConsumer<A, T> accumulator;
    private final BinaryOperator<A> combiner;
    private final Function<A, R> finisher;

    CollectorImpl(Supplier<A> supplier, BiConsumer<A, T> accumulator, BinaryOperator<A> combiner,
                  Function<A, R> finisher) {
        this.supplier = supplier;
        this.accumulator = accumulator;
        this.combiner = combiner;
        this.finisher = finisher;
    }

    public Supplier<A> supplier() {
        return this.supplier;
    }

    public BiConsumer<A, T> accumulator() {
        return this.accumulator;
    }

    public BinaryOperator<A> combiner() {
        return this.combiner;
    }

    public Function<A, R> finisher() {
        return this.finisher;
    }
}
