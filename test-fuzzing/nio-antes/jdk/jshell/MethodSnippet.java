package jdk.jshell;

/**
 * The declaration of a method.
 *
 * <h2>Why the signature is kept as text</h2>
 *
 * <p>Because a method may be declared before the types it mentions exist: writing a method that
 * returns a class that has not been declared yet is normal in an interactive session. A reflection
 * object could not be built; a string can.
 *
 * <p>{@link #parameterTypes} comes separately because it is what decides whether a later method
 * replaces this one or overloads it: the same name and the same parameter types means replacing.
 *
 * @since 9
 */
public class MethodSnippet extends DeclarationSnippet {

    private final String parameterTypes;
    private final String signature;

    MethodSnippet(String id, String source, SubKind subkind, String name, String parameterTypes,
            String signature) {
        super(id, source, subkind, name);
        this.parameterTypes = parameterTypes;
        this.signature = signature;
    }

    /**
     * The parameter types, separated by commas.
     *
     * @return the types, or the empty string if the method takes nothing
     */
    public String parameterTypes() {
        return this.parameterTypes;
    }

    /**
     * The full signature, with the return type.
     *
     * @return the signature
     */
    public String signature() {
        return this.signature;
    }

    /**
     * For reading while debugging.
     *
     * @return what {@link Snippet#toString} gives plus the parameter types
     */
    @Override
    public String toString() {
        return super.toString() + "-" + this.parameterTypes;
    }
}
