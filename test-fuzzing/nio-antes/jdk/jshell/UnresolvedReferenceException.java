package jdk.jshell;

/**
 * Something left half declared was used.
 *
 * <h2>When it happens</h2>
 *
 * <p>Writing a method that calls another that does not exist yet is accepted: in an interactive
 * session the order things are written in need not be the order they are used in. The method is left
 * in {@link Snippet.Status#RECOVERABLE_DEFINED} and waiting. If it is called before what it is
 * missing turns up, this is thrown.
 *
 * <p>{@link #getSnippet} returns the incomplete snippet, and from there
 * {@link JShell#unresolvedDependencies} says what it is missing.
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
     * The snippet that was left half declared.
     *
     * @return the snippet
     */
    public DeclarationSnippet getSnippet() {
        return this.snippet;
    }
}
