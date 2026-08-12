package java.util.stream;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.function.Supplier;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;

// KajiLibrary's java.util.stream.Collectors — factories for the common Collectors that
// Stream.collect uses. Each builds a CollectorImpl (same-file, below) from four functions.
//
// Two frozen-javac workarounds shape the code: (1) every lambda is bound to an explicitly-typed
// local first — the codegen can't infer a lambda's target functional-interface type *through* a
// generic constructor's type parameters (finding #16); (2) the mutable containers are typed as the
// concrete `ArrayList`/`HashSet` (not `List`/`Set`) so `add` resolves directly rather than through
// a generic superinterface (finding #15). The `combiner` is a no-op merge — our eager sequential
// collect never calls it. Compiled with `-cp` so `Collector`/`List`/`Set` bind to our own subset.
public final class Collectors {

    private Collectors() {}

    // Accumulate the elements into a List (an ArrayList).
    public static <T> Collector<T, ?, List<T>> toList() {
        Supplier<ArrayList<T>> supplier = () -> new ArrayList<T>();
        BiConsumer<ArrayList<T>, T> accumulator = (list, item) -> { list.add(item); };
        BinaryOperator<ArrayList<T>> combiner = (a, b) -> a;
        Function<ArrayList<T>, List<T>> finisher = (list) -> list;
        return new CollectorImpl<T, ArrayList<T>, List<T>>(supplier, accumulator, combiner, finisher);
    }

    // Accumulate the elements into a Set (a HashSet), dropping duplicates.
    public static <T> Collector<T, ?, Set<T>> toSet() {
        Supplier<HashSet<T>> supplier = () -> new HashSet<T>();
        BiConsumer<HashSet<T>, T> accumulator = (set, item) -> { set.add(item); };
        BinaryOperator<HashSet<T>> combiner = (a, b) -> a;
        Function<HashSet<T>, Set<T>> finisher = (set) -> set;
        return new CollectorImpl<T, HashSet<T>, Set<T>>(supplier, accumulator, combiner, finisher);
    }

    // Concatenate the elements' characters into one String.
    public static Collector<CharSequence, ?, String> joining() {
        Supplier<StringBuilder> supplier = () -> new StringBuilder();
        BiConsumer<StringBuilder, CharSequence> accumulator = (sb, cs) -> { sb.append(cs); };
        BinaryOperator<StringBuilder> combiner = (a, b) -> a;
        Function<StringBuilder, String> finisher = (sb) -> sb.toString();
        return new CollectorImpl<CharSequence, StringBuilder, String>(supplier, accumulator, combiner, finisher);
    }

    // Count the elements.
    public static <T> Collector<T, ?, Long> counting() {
        Supplier<long[]> supplier = () -> new long[1];
        BiConsumer<long[], T> accumulator = (box, item) -> { box[0] = box[0] + 1L; };
        BinaryOperator<long[]> combiner = (a, b) -> a;
        Function<long[], Long> finisher = (box) -> Long.valueOf(box[0]);
        return new CollectorImpl<T, long[], Long>(supplier, accumulator, combiner, finisher);
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
