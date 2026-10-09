package jdk.dynalink;

/**
 * The three namespaces every object of the language has.
 *
 * <p>The split between {@link #PROPERTY} and {@link #ELEMENT} is the one Java does not make and
 * dynamic languages do: `a.x` and `a[x]` are different operations even when the name coincides.
 * {@link #METHOD} exists apart from `PROPERTY` because in a JavaBean the method `getFoo` and the
 * property `foo` live side by side, and asking for "the member foo" has two answers depending on
 * where you look.
 *
 * @since 9
 */
public enum StandardNamespace implements Namespace {

    /** Named property: `obj.foo`. */
    PROPERTY,

    /** Element indexed by key or position: `obj[foo]`. */
    ELEMENT,

    /** Method: what you get when asking for `obj.foo` expecting something invocable. */
    METHOD;

    /**
     * The first standard namespace of `op`, or `null` if it has none.
     *
     * <p>It unwraps the two layers of decoration in the only order they can be in (name outside,
     * namespaces inside), so it serves both `GET:PROPERTY` and `GET:PROPERTY:x`.
     */
    public static StandardNamespace findFirst(final Operation op) {
        for (final Namespace ns : NamespaceOperation.getNamespaces(NamedOperation.getBaseOperation(op))) {
            if (ns instanceof StandardNamespace) {
                return (StandardNamespace) ns;
            }
        }
        return null;
    }
}
