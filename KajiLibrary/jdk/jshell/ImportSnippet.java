package jdk.jshell;

/**
 * An import.
 *
 * <h2>The two names</h2>
 *
 * <p>{@link #name} is the simple name --what is written afterwards to refer to the type-- and
 * {@link #fullname} is the full name, with its package. In a star import there is no simple name to
 * speak of, so {@link #name} returns the last segment and the star stays in {@link #fullname}.
 *
 * @since 9
 */
public class ImportSnippet extends PersistentSnippet {

    private final String fullname;
    private final boolean isStatic;

    ImportSnippet(String id, String source, SubKind subkind, String name, String fullname,
            boolean isStatic) {
        super(id, source, subkind, name);
        this.fullname = fullname;
        this.isStatic = isStatic;
    }

    /**
     * The full name of what is imported.
     *
     * @return the name with its package
     */
    public String fullname() {
        return this.fullname;
    }

    /**
     * Whether it is a static import.
     *
     * @return true if it is
     */
    public boolean isStatic() {
        return this.isStatic;
    }
}
