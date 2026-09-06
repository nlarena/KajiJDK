package jdk.jshell;

/**
 * Una expresion suelta.
 *
 * <h2>Por que tiene nombre y tipo</h2>
 *
 * <p>Una expresion no declara nada, pero produce un valor, y el interprete lo guarda para que se
 * pueda seguir usando. Si la expresion es solo el nombre de una variable, {@link #name} devuelve ese
 * nombre; si es cualquier otra cosa --{@link Snippet.SubKind#OTHER_EXPRESSION_SUBKIND}--, el valor
 * va a una variable temporal y el fragmento pasa a ser un {@link VarSnippet}, no este.
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
     * El nombre de la variable, si la expresion es solo eso.
     *
     * @return el nombre, o {@code null} si la expresion no es el nombre de una variable
     */
    public String name() {
        return this.name;
    }

    /**
     * El tipo del valor que produce.
     *
     * @return el nombre del tipo
     */
    public String typeName() {
        return this.typeName;
    }
}
