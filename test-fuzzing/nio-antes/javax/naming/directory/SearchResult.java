package javax.naming.directory;

import javax.naming.Binding;

/**
 * KajiLibrary's javax.naming.directory.SearchResult -- an entry that matched the search.
 *
 * <p>A {@link Binding} --name and object-- plus the attributes that were asked for. It extends
 * {@code Binding} instead of copying it because a search result <b>is</b> a name-object binding;
 * what the directory adds is the attributes.
 *
 * <p>The name may be relative or absolute and that matters when using it: with
 * {@link javax.naming.NameClassPair#isRelative} false, the name is a full URL and cannot be passed
 * to {@code lookup} on the same context. It happens when the search crossed a referral to another
 * server.
 *
 * <p>The object comes only if asked for with {@link SearchControls#setReturningObjFlag}; otherwise
 * it is null and the only useful things are the name and the attributes.
 */
public class SearchResult extends Binding {

    private static final long serialVersionUID = -9158063327699723172L;

    /** The attributes that were asked for. */
    private Attributes attrs;

    /** Relative name, object and attributes. */
    public SearchResult(String name, Object obj, Attributes attrs) {
        super(name, obj);
        this.attrs = attrs;
    }

    /** Same, stating whether the name is relative. */
    public SearchResult(String name, Object obj, Attributes attrs, boolean isRelative) {
        super(name, obj, isRelative);
        this.attrs = attrs;
    }

    /** Same, with an explicit class name. */
    public SearchResult(String name, String className, Object obj, Attributes attrs) {
        super(name, className, obj);
        this.attrs = attrs;
    }

    /** Everything explicit. */
    public SearchResult(String name, String className, Object obj, Attributes attrs,
                        boolean isRelative) {
        super(name, className, obj, isRelative);
        this.attrs = attrs;
    }

    /** The attributes. */
    public Attributes getAttributes() {
        return this.attrs;
    }

    /** Ver {@link #getAttributes}. */
    public void setAttributes(Attributes attrs) {
        this.attrs = attrs;
    }

    /** What {@link Binding} has, plus the attributes. */
    public String toString() {
        return super.toString() + ":" + getAttributes();
    }
}
