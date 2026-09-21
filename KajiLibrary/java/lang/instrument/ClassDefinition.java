package java.lang.instrument;

/**
 * KajiLibrary's java.lang.instrument.ClassDefinition -- a class and the bytes to replace it with.
 *
 * <p>An immutable pair, and nothing else. It exists because {@code redefineClasses} takes
 * <b>several</b> redefinitions and applies them together: without a pair it would need two parallel
 * arrays, and one index slip would put one class's bytes into another.
 *
 * <p>That they are applied together is no detail. Redefining two classes that call each other one at
 * a time would leave an instant with the old version of one and the new version of the other, and
 * there the program can break.
 *
 * <p>The bytes are <b>not copied</b>: the array handed in is kept, and
 * {@link #getDefinitionClassFile} returns it as it stands. It is what the JDK does, and it has to be
 * known -- modifying the array after building this changes what is going to be redefined.
 */
public final class ClassDefinition {

    /** The class to replace. */
    private final Class<?> definitionClass;

    /** With what bytes. */
    private final byte[] definitionClassFile;

    /**
     * @param theClass the class to replace
     * @param theClassFile the bytes of the new class file
     * @throws NullPointerException if either is null
     */
    public ClassDefinition(Class<?> theClass, byte[] theClassFile) {
        if (theClass == null) {
            throw new NullPointerException("null passed as 'theClass' in ClassDefinition");
        }
        if (theClassFile == null) {
            throw new NullPointerException("null passed as 'theClassFile' in ClassDefinition");
        }
        this.definitionClass = theClass;
        this.definitionClassFile = theClassFile;
    }

    /** The class to replace. */
    public Class<?> getDefinitionClass() {
        return this.definitionClass;
    }

    /** The bytes. The same array that was handed in; see the class's note. */
    public byte[] getDefinitionClassFile() {
        return this.definitionClassFile;
    }
}
