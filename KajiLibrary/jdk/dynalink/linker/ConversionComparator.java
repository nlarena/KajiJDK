package jdk.dynalink.linker;

/**
 * Breaks the tie between two possible conversions when neither is obviously better.
 *
 * <h2>What problem it solves</h2>
 *
 * <p>A dynamic language calls `f(x)` and the target class has `f(int)` and `f(String)`. If `x` is a
 * `double`, both overloads are reachable: the language knows how to convert a number to `int` and
 * also how to convert it to `String`. Java has no rule for choosing, because the set of conversions
 * is not Java's — it is contributed by the language's linker.
 *
 * <p>This interface is where that linker says which one it prefers. A {@link GuardingDynamicLinker}
 * that also implements it takes part in the tie-break; one that does not implement it simply has no
 * opinion.
 *
 * <h2>Why it may not know</h2>
 *
 * <p>Through {@link Comparison#INDETERMINATE}, which is not an error but the honest answer of one
 * who has no preference between those two targets. If every comparator answers that, the ambiguity
 * is left unresolved and the caller decides by its own rules.
 *
 * @since 9
 */
public interface ConversionComparator {

    /** The preference between two target types. */
    enum Comparison {
        /** No preference: this comparator does not tell the two targets apart. */
        INDETERMINATE,
        /** The first target is better. */
        TYPE_1_BETTER,
        /** The second target is better. */
        TYPE_2_BETTER
    }

    /**
     * Which of the two targets suits a value of {@code sourceType}.
     *
     * @param sourceType the type of the value to convert
     * @param targetType1 the first candidate target
     * @param targetType2 the second candidate target
     * @return the preference, or {@link Comparison#INDETERMINATE} if there is none
     */
    Comparison compareConversion(Class<?> sourceType, Class<?> targetType1, Class<?> targetType2);
}
