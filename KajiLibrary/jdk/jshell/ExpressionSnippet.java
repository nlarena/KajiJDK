package jdk.jshell;

/**
 * A loose expression.
 *
 * <h2>Why it has a name and a type</h2>
 *
 * <p>An expression declares nothing, but it produces a value, and the interpreter keeps it so that
 * it can go on being used. If the expression is only a variable's name, {@link #name} returns that
 * name; if it is anything else --{@link Snippet.SubKind#OTHER_EXPRESSION_SUBKIND}-- the value goes
 * into a temporary variable and the snippet becomes a {@link VarSnippet}, not this.
 *
 * @since 9
 */
public class ExpressionSnippet extends Snippet {

    private final String name;
    private final String typeName;

    ExpressionSnippet(String id, String source, SubKind subkind, String name, String typeName) {
        super(id, source, subkind);
        this.name = name;
        this.typeName = typeName;
    }

    /**
     * The variable's name, if the expression is only that.
     *
     * @return the name, or {@code null} if the expression is not a variable's name
     */
    public String name() {
        return this.name;
    }

    /**
     * The type of the value it produces.
     *
     * @return the type's name
     */
    public String typeName() {
        return this.typeName;
    }
}
