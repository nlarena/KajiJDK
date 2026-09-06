package jdk.jshell;

/**
 * A loose statement.
 *
 * <p>An {@code if}, a {@code for}, a {@code while}. It runs and leaves nothing: no name and no
 * value, unlike an expression. It is the only executable snippet that produces nothing to show.
 *
 * @since 9
 */
public class StatementSnippet extends Snippet {

    StatementSnippet(String id, String source, SubKind subkind) {
        super(id, source, subkind);
    }
}
