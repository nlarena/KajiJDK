package java.lang.reflect;

/**
 * The common supertype of every type the Java language knows how to write.
 *
 * <p>This is the root of reflection's <em>generic</em> type model, and it exists because
 * {@link Class} is not enough. A {@code Class} object can say "this is a {@code List}", but it
 * cannot say "this is a {@code List<String>}", "this is a {@code T}", or "this is a
 * {@code ? extends Number}" — those are not classes, and there is no {@code Class} object for
 * them. So the model widens: {@code Class} becomes one of five things a type can be, alongside
 * {@link ParameterizedType}, {@link TypeVariable}, {@link WildcardType} and
 * {@link GenericArrayType}.
 *
 * <p>The interface itself is nearly empty, which is the point: it is a union, not a contract.
 * Callers ask what kind of type they have with {@code instanceof} and then use the specific
 * interface. That is unusual in a library API, and it is what lets the model describe a syntax
 * tree of types rather than a set of runtime classes.
 */
public interface Type {

    /**
     * Returns a string describing this type, including any type arguments.
     *
     * @return an informative string
     */
    default String getTypeName() {
        // `this.toString()` would be the natural spelling, and JLS §9.2 says it is legal: an
        // interface implicitly declares every public method of Object. Our compiler resolves `this`
        // calls only against members declared in the type itself, and the fix for finding #22
        // covered classes but explicitly left interfaces with no superclass — so the call does not
        // resolve. String.valueOf routes through the same Object.toString and needs no inheritance.
        return String.valueOf(this);
    }
}
