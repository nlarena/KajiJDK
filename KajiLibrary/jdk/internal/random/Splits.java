package jdk.internal.random;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator.SplittableGenerator;
import java.util.stream.Stream;

// KajiLibrary's jdk.internal.random.Splits -- the common part of `splits(...)`, shared by the
// eight LXM generators.
//
// It exists because in the JDK this lives in a hierarchy of abstract classes
// (`AbstractSpliteratorGenerator` -> `AbstractSplittableGenerator` ->
// `AbstractSplittableWithBrineGenerator`) that this library does not have: here each generator is
// `final` and implements the interface directly. The logic still has to be in one single place, so
// it is here.
//
// **One difference with the JDK, and it is the usual one in this library**: the stream is *eager*.
// The JDK returns a lazy `Stream` over a spliterator that goes on splitting as it is consumed, and
// it also uses a **salted brine** --4-bit digits that guarantee that two children of different
// branches of the tree of splitting never share `a`--. Here the `streamSize` children are built in
// one go and the brine of each one comes from the `source`, which gives the same guarantee for one
// single level of splitting but not for a tree.
//
// It is documented instead of pretended: whoever splits at one single level --which is what 99 % of
// the code does-- obtains independent generators; whoever builds a deep tree has less guarantee
// than in the JDK.
final class Splits {

    private Splits() {
    }

    static Stream<SplittableGenerator> de(SplittableGenerator parent, long streamSize,
            SplittableGenerator source) {
        if (streamSize < 0L) {
            throw new IllegalArgumentException("size must be non-negative");
        }
        if (source == null) {
            throw new NullPointerException("source");
        }
        List<SplittableGenerator> children = new ArrayList<SplittableGenerator>();
        long i = 0L;
        while (i < streamSize) {
            children.add(parent.split(source));
            i = i + 1L;
        }
        return children.stream();
    }
}
