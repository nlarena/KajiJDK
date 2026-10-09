package javax.naming;

/**
 * What `Context.list()` returns: the name of a binding and the class name of what is bound,
 * **without fetching the object**.
 *
 * <p>That is the whole reason the type exists and that `list` and `listBindings` are two different
 * methods. Listing a context with a thousand bindings and materializing the thousand objects --
 * opening a thousand connections, deserializing a thousand graphs-- to then look at the name of a
 * single one is exactly what must be avoided. `list` returns these pairs, which are metadata the
 * server already had; whoever wants the object does a targeted `lookup`, or uses `listBindings`
 * and gets `Binding`, which is this same class plus the object.
 *
 * <h2>The name is relative, except when it is not</h2>
 *
 * <p>`getName()` returns a name **relative to the context that was listed**, not absolute: listing
 * `ou=people` gives `cn=john`, not `cn=john,ou=people`. `isRelative()` is `false` in the only case
 * where it cannot be: when the binding points outside the context and the name is a URL. That is
 * why the flag exists, and why `toString()` marks it: a consumer that blindly builds
 * `context + "/" + name` would produce garbage for those entries.
 *
 * <p>`getNameInNamespace()` is the absolute name, and it is **optional**: it throws
 * `UnsupportedOperationException` if the provider did not fill it in. It does not return `null`
 * because `null` would be confused with "the absolute name is empty", which is what holds for the
 * root.
 */
public class NameClassPair implements java.io.Serializable {

    private static final long serialVersionUID = 5620776610160863339L;

    private String name;
    private String className;
    private String fullName;
    private boolean isRel;

    /** Relative by default, which is the normal case: hardly any binding points outside. */
    public NameClassPair(String name, String className) {
        this(name, className, true);
    }

    public NameClassPair(String name, String className, boolean isRelative) {
        this.name = name;
        this.className = className;
        this.isRel = isRelative;
    }

    public String getClassName() {
        return className;
    }

    public String getName() {
        return name;
    }

    /**
     * The absolute name, if the provider set it.
     *
     * @throws UnsupportedOperationException if it did not
     */
    public String getNameInNamespace() {
        if (fullName == null) {
            throw new UnsupportedOperationException();
        }
        return fullName;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setClassName(String name) {
        this.className = name;
    }

    public void setNameInNamespace(String fullName) {
        this.fullName = fullName;
    }

    public boolean isRelative() {
        return isRel;
    }

    public void setRelative(boolean r) {
        this.isRel = r;
    }

    /**
     * Marks the non-relative ones up front: it is the only difference that changes how the name is
     * used.
     */
    @Override
    public String toString() {
        return (isRelative() ? "" : "(not relative)") + getName() + ": " + getClassName();
    }
}
