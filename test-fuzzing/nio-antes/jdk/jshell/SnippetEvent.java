package jdk.jshell;

/**
 * What happened to a snippet.
 *
 * <h2>Why evaluating produces several</h2>
 *
 * <p>Because one snippet drags the others along. Rewriting a method leaves the earlier one in
 * {@link Snippet.Status#OVERWRITTEN} and may make valid a third one that was waiting for it: three
 * events out of a single evaluation. {@link #causeSnippet} is what tells them apart --the snippet
 * that was evaluated has {@code null}, the dragged ones point at it.
 *
 * <h2>{@link #isSignatureChange}</h2>
 *
 * <p>It says whether what changed was the shape of what is declared and not only its body. It
 * matters because a signature change forces everything that depended on it to be recompiled, and a
 * body change does not.
 *
 * @since 9
 */
public class SnippetEvent {

    private final Snippet snippet;
    private final Snippet.Status previousStatus;
    private final Snippet.Status status;
    private final boolean isSignatureChange;
    private final Snippet causeSnippet;
    private final JShellException exception;
    private final String value;

    SnippetEvent(Snippet snippet, Snippet.Status previousStatus, Snippet.Status status,
            boolean isSignatureChange, Snippet causeSnippet, JShellException exception,
            String value) {
        this.snippet = snippet;
        this.previousStatus = previousStatus;
        this.status = status;
        this.isSignatureChange = isSignatureChange;
        this.causeSnippet = causeSnippet;
        this.exception = exception;
        this.value = value;
    }

    /**
     * Which snippet this is about.
     *
     * @return the snippet
     */
    public Snippet snippet() {
        return this.snippet;
    }

    /**
     * What state it was in before.
     *
     * @return the previous state
     */
    public Snippet.Status previousStatus() {
        return this.previousStatus;
    }

    /**
     * What state it ended up in.
     *
     * @return the new state
     */
    public Snippet.Status status() {
        return this.status;
    }

    /**
     * Whether the shape of what is declared changed and not only its body.
     *
     * @return true if the signature changed
     */
    public boolean isSignatureChange() {
        return this.isSignatureChange;
    }

    /**
     * Which snippet dragged this one along.
     *
     * @return the snippet that was evaluated, or {@code null} if this is the one that was
     */
    public Snippet causeSnippet() {
        return this.causeSnippet;
    }

    /**
     * Which exception the user's code threw.
     *
     * @return the exception, or {@code null} if it threw none
     */
    public JShellException exception() {
        return this.exception;
    }

    /**
     * The value it produced, already turned into text.
     *
     * <p>It comes as text and not as an object because the value lives on the other virtual machine,
     * and its class may not exist on this side.
     *
     * @return the value, or {@code null} if the snippet produces none
     */
    public String value() {
        return this.value;
    }

    /**
     * For reading while debugging.
     *
     * @return the snippet, the two states and the value
     */
    @Override
    public String toString() {
        return "SnippetEvent(" + this.snippet + " " + this.previousStatus + "=>" + this.status
                + " sig=" + this.isSignatureChange + " value=" + this.value + ")";
    }
}
