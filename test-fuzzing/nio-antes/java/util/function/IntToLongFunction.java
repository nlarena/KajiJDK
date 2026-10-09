package java.util.function;

// KajiLibrary's java.util.function.IntToLongFunction — int in, long out. SAM: `applyAsLong`.
// One of the six cross-primitive widening/narrowing shapes (int/long/double, each to the
// other two). They exist because IntUnaryOperator only covers int -> int: the moment a
// mapping changes primitive width there is no operator interface for it, and falling back
// to Function<Integer,Long> would box both ends. IntStream.mapToLong binds to this.
public interface IntToLongFunction {

    long applyAsLong(int value);
}
