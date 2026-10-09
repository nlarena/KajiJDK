package javax.naming;

import java.util.Enumeration;

/**
 * A name as an **ordered sequence of components**, not as a string.
 *
 * <p>That is the whole idea of the type. `"a/b/c"` is a string; `{"a","b","c"}` is a name.
 * Converting between the two forms depends on a syntax --what separates, what quotes, what
 * escapes-- and that is exactly what changes from one namespace to another: LDAP separates with a
 * comma and right to left, a file system with a slash and left to right, and a flat namespace
 * separates nothing. Handling components instead of strings is what allows writing code that
 * crosses namespaces without knowing the syntax of any of them.
 *
 * <p>The package's two implementations are `CompositeName` --fixed syntax, the one for crossing
 * several namespaces-- and `CompoundName` --syntax given by a `Properties`, the one representing a
 * name **within** a namespace.
 *
 * <h2>Two things that surprise and are part of the contract</h2>
 *
 * <p><strong>It is mutable.</strong> `add`, `addAll` and `remove` change the name in place and
 * return `this` for chaining. That is why the rest of the package clones before storing a `Name`
 * (see `NamingException`'s setters).
 *
 * <p><strong>`compareTo` takes `Object` and not `Name`.</strong> The interface is
 * `Comparable<Object>`, which today would be written `Comparable<Name>`. It stayed that way from
 * before generics and cannot be fixed without breaking everyone who already implemented the
 * interface. The same goes for `remove`, which returns `Object` --always a `String`--, and for
 * `clone`, which returns `Object`.
 *
 * <p>The interface is `Serializable`: each implementation defines the serial form.
 */
public interface Name extends Cloneable, java.io.Serializable, Comparable<Object> {

    long serialVersionUID = -3617482732056931635L;

    Object clone();

    /**
     * Order between names of the **same** type; throws `ClassCastException` if they are not.
     *
     * <p>The order is lexicographic by components, normalized by **this** name's syntax --not the
     * other's--: if this syntax ignores case, so does the comparison.
     */
    int compareTo(Object obj);

    int size();

    boolean isEmpty();

    /**
     * The components in order, from zero to the last. It is an `Enumeration` because of the API's
     * age.
     */
    Enumeration<String> getAll();

    String get(int posn);

    /** The first `posn` components, as a new name. `posn == size()` gives a full copy. */
    Name getPrefix(int posn);

    /** From `posn` to the end, as a new name. `posn == size()` gives the empty name. */
    Name getSuffix(int posn);

    boolean startsWith(Name n);

    boolean endsWith(Name n);

    /** Appends `suffix` at the end and returns `this`, already modified. */
    Name addAll(Name suffix) throws InvalidNameException;

    /** Inserts the components of `n` starting at `posn` and returns `this`. */
    Name addAll(int posn, Name n) throws InvalidNameException;

    Name add(String comp) throws InvalidNameException;

    Name add(int posn, String comp) throws InvalidNameException;

    /** Removes component `posn` and returns it; it is always a `String`. */
    Object remove(int posn) throws InvalidNameException;
}
