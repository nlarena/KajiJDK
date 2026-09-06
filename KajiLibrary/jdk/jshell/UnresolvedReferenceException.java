package jdk.jshell;

/**
 * Se intento usar algo que quedo declarado a medias.
 *
 * <h2>Cuando pasa</h2>
 *
 * <p>Escribir un metodo que llama a otro que todavia no existe se acepta: en una sesion interactiva
 * el orden en que se escriben las cosas no tiene por que ser el orden en que se usan. El metodo
 * queda en {@link Snippet.Status#RECOVERABLE_DEFINED} y esperando. Si se lo llama antes de que
 * aparezca lo que le falta, salta esto.
 *
 * <p>{@link #getSnippet} devuelve el fragmento incompleto, y de ahi
 * {@link JShell#unresolvedDependencies} dice que le falta.
 *
 * @since 9
 */
public class UnresolvedReferenceException extends JShellException {

    private static final long serialVersionUID = 1L;

    private final transient DeclarationSnippet snippet;

    UnresolvedReferenceException(DeclarationSnippet snippet, String message) {
        super(message);
        this.snippet = snippet;
    }

    /**
     * El fragmento que quedo declarado a medias.
     *
     * @return el fragmento
     */
    public DeclarationSnippet getSnippet() {
        return this.snippet;
    }
}
