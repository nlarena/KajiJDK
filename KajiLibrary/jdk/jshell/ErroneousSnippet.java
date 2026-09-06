package jdk.jshell;

/**
 * A snippet that could not be understood.
 *
 * <h2>Why it exists instead of throwing</h2>
 *
 * <p>In an interpreter, writing something wrong is the normal case, and it cannot cost the session
 * its state. The erroneous snippet is kept all the same, with its errors, and can be listed and
 * consulted like any other. {@link #probableKind} is what the analyser believes was being written
 * --it is for giving a useful message-- and it may be {@link Snippet.Kind#ERRONEOUS} when it could
 * not tell even that.
 *
 * @since 9
 */
public class ErroneousSnippet extends Snippet {

    private final Kind probableKind;

    ErroneousSnippet(String id, String source, SubKind subkind, Kind probableKind) {
        super(id, source, subkind);
        this.probableKind = probableKind;
    }

    /**
     * What was being written, as far as could be told.
     *
     * @return the probable kind
     */
    public Kind probableKind() {
        return this.probableKind;
    }
}
