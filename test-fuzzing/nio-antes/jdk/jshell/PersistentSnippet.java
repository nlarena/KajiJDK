package jdk.jshell;

/**
 * A snippet that stays declared for the ones that come after.
 *
 * <p>Imports and declarations --types, methods, variables-- go on existing after being written; a
 * loose expression is evaluated and done with. The difference is that these have a name, and the
 * name is what lets a later snippet replace them: writing a method with the same signature again
 * leaves the earlier one in {@link Snippet.Status#OVERWRITTEN}.
 *
 * @since 9
 */
public abstract class PersistentSnippet extends Snippet {

    private final String name;

    PersistentSnippet(String id, String source, SubKind subkind, String name) {
        super(id, source, subkind);
        this.name = name;
    }

    /**
     * The name of what it declares.
     *
     * @return the name
     */
    public String name() {
        return this.name;
    }
}
