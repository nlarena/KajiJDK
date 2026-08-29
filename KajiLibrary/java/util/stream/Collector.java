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
// This is the interface only, and a subset. Still deferred, and NOT for the reason the previous
// pass recorded:
//
//   - finding #12 (an enum nested in an *interface* came out with no constants, no values/valueOf
//     and no static initialiser) no longer reproduces on the frozen javac — a nested
//     `enum Characteristics` compiles correctly today. What blocks `characteristics()` now is
//     that a nested type's *identity* does not survive being imported into another compilation
//     unit: with `Set<Characteristics> characteristics()` declared here and CollectorImpl (in
//     Collectors.java) implementing it via `import java.util.stream.Collector.Characteristics`,
//     the override check rejects it with "Set no es un subtipo de Set" — the imported nested type
//     is a different type object from the one the interface declared. Since every concrete
//     Collector we ship lives in Collectors.java, the method cannot be implemented. Repro in the
//     defect notes;
//   - the two static `of(...)` factories take `Characteristics...`, and this javac does not set
//     ACC_VARARGS (finding #200), so the emitted descriptor would be `Characteristics[]` and the
//     member would not match the JDK's even if the enum were usable.
public interface Collector<T, A, R> {

    Supplier<A> supplier();

    BiConsumer<A, T> accumulator();

    BinaryOperator<A> combiner();

    Function<A, R> finisher();
}
