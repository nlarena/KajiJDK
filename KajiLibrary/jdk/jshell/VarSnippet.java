package jdk.jshell;

/**
 * The declaration of a variable.
 *
 * <h2>The three shapes</h2>
 *
 * <p>Declared with no value, declared with a value, or invented by the interpreter to hold the
 * result of a loose expression --{@link Snippet.SubKind#TEMP_VAR_EXPRESSION_SUBKIND}. All three are
 * real variables: the third has a name too and can be used afterwards, which is what makes writing a
 * loose expression leave something to refer to.
 *
 * @since 9
 */
public class VarSnippet extends DeclarationSnippet {

    private final String typeName;

    VarSnippet(String id, String source, SubKind subkind, String name, String typeName) {
        super(id, source, subkind, name);
        this.typeName = typeName;
    }

    /**
     * The variable's type.
     *
     * @return the type's name
     */
    public String typeName() {
        return this.typeName;
    }
}
