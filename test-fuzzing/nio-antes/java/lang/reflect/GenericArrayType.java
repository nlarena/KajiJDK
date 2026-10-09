package java.lang.reflect;

/**
 * An array type whose component type is itself generic: {@code T[]} or {@code List<String>[]}.
 *
 * <p>It exists because ordinary arrays already have {@link Class} objects — {@code String[].class}
 * is real — but {@code T[]} does not, for the same reason {@code List<String>} does not: erasure
 * deleted it. So the generic model needs its own node for the case where the component type is a
 * type variable or a parameterized type.
 */
public interface GenericArrayType extends Type {

    /**
     * Returns the component type of this array type.
     *
     * @return the generic component type
     */
    Type getGenericComponentType();
}
