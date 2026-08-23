package java.lang.reflect;

/**
 * Something that can declare type parameters: a class, a method, or a constructor.
 *
 * <p>Those three are the only generic declarations the language has, and they have no other common
 * supertype — {@link Class} is not a {@link Member} and {@code Method} is not a {@code Class} — so
 * this interface exists to give {@link TypeVariable#getGenericDeclaration} something to return.
 *
 * <p>That is also why {@code TypeVariable} is parameterized on {@code D extends GenericDeclaration}
 * rather than just returning this interface: a variable read off a {@code Method} comes back as a
 * {@code TypeVariable<Method>}, so {@code getGenericDeclaration()} hands back a {@code Method}
 * without a cast. The two types are mutually recursive by design.
 *
 * <h2>KajiLibrary status</h2>
 *
 * <p>Complete. Note that nothing in KajiLibrary <em>implements</em> it yet: {@code Class} would have
 * to, and {@code Class} is owned by the VM session. {@link Executable} does implement it, which is
 * what puts {@code Method} and {@code Constructor} on the right side of the hierarchy.
 */
public interface GenericDeclaration extends AnnotatedElement {

    /**
     * Returns the type parameters declared here, in declaration order.
     *
     * @return the type variables; empty if this declaration is not generic
     */
    TypeVariable<?>[] getTypeParameters();
}
