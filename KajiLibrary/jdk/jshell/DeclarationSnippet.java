package jdk.jshell;

/**
 * Un fragmento que declara algo: un tipo, un metodo o una variable.
 *
 * <h2>Que lo separa de un import</h2>
 *
 * <p>Los dos son persistentes, pero una declaracion puede quedar a medias. Un metodo que llama a
 * otro que todavia no existe se acepta igual --en una sesion interactiva el orden en que se escriben
 * las cosas no tiene por que ser el orden en que se usan-- y queda con referencias sin resolver,
 * que se pueden consultar con {@link JShell#unresolvedDependencies}. Un import no tiene ese estado
 * intermedio.
 *
 * @since 9
 */
public abstract class DeclarationSnippet extends PersistentSnippet {

    DeclarationSnippet(String id, String source, SubKind subkind, String name) {
        super(id, source, subkind, name);
    }
}
