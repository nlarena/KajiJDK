package jdk.jshell;

/**
 * La declaracion de una variable.
 *
 * <h2>Las tres formas</h2>
 *
 * <p>Declarada sin valor, declarada con valor, o inventada por el interprete para guardar el
 * resultado de una expresion suelta --{@link Snippet.SubKind#TEMP_VAR_EXPRESSION_SUBKIND}--. Las
 * tres son variables de verdad: la tercera tambien tiene nombre y se puede usar despues, que es lo
 * que hace que escribir una expresion suelta deje algo a lo que referirse.
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
     * El tipo de la variable.
     *
     * @return el nombre del tipo
     */
    public String typeName() {
        return this.typeName;
    }
}
