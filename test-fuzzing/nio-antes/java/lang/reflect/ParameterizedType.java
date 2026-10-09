package java.lang.reflect;

/**
 * A type with type arguments applied to it: {@code List<String>}, {@code Map<K, ? extends V>}.
 *
 * <p>The three accessors are the three parts of such a type, and separating them is what makes the
 * model composable: the RAW type ({@code List}) is a {@link Class}, the ARGUMENTS are themselves
 * {@link Type}s — which is how nesting like {@code List<Map<K, V>>} is expressed — and the OWNER is
 * the enclosing type when the parameterized type is a member of another one.
 *
 * <p>There is no {@code Class} object for {@code List<String>}; erasure removed it from the runtime
 * entirely. This interface is where that information survives, recovered from the
 * {@code Signature} attribute the compiler wrote into the class file.
 */
public interface ParameterizedType extends Type {

    /**
     * Returns the type arguments applied to the raw type.
     *
     * @return the actual type arguments, in declaration order
     */
    Type[] getActualTypeArguments();

    /**
     * Returns the type this one parameterizes — the erasure.
     *
     * @return the raw type
     */
    Type getRawType();

    /**
     * Returns the type this one is a member of, or {@code null} if it is a top-level type.
     *
     * @return the owner type, or {@code null}
     */
    Type getOwnerType();
}
