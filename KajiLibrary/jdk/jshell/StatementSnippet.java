package jdk.jshell;

/**
 * Una sentencia suelta.
 *
 * <p>Un {@code if}, un {@code for}, un {@code while}. Se ejecuta y no deja nada: ni nombre ni valor,
 * a diferencia de una expresion. Es el unico fragmento ejecutable que no produce algo para mostrar.
 *
 * @since 9
 */
public class StatementSnippet extends Snippet {

    StatementSnippet(String id, String source, SubKind subkind) {
        super(id, source, subkind);
    }
}
