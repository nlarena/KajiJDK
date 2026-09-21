package javax.naming;

import java.util.Enumeration;
import java.util.Properties;

/**
 * The name **within one** namespace, with the syntax the provider dictates.
 *
 * <h2>The difference from `CompositeName`</h2>
 *
 * <p>`CompositeName` crosses namespaces and that is why its syntax is fixed. This one lives inside
 * a single one, and there the syntax is whatever the provider says: LDAP separates with a comma and
 * counts right to left, a Windows file system separates with a backslash and left to right, and a
 * flat namespace separates nothing. A component of a `CompositeName` is, typically, the string form
 * of a `CompoundName`.
 *
 * <h2>The syntax `Properties`</h2>
 *
 * <p>The keys looked at are `jndi.syntax.direction` --`left_to_right`, `right_to_left` or `flat`--,
 * `.separator`, `.separator2`, `.escape`, `.beginquote`, `.endquote`, `.beginquote2`, `.endquote2`,
 * `.ignorecase`, `.trimblanks`, `.ava` and `.typeval`. The direction is mandatory and so is the
 * separator, except when flat.
 *
 * <p>Right to left is **not** cosmetic: in `cn=john,o=acme` with direction `right_to_left`,
 * `get(0)` is `o=acme` and not `cn=john`. Component 0 is always the most significant, that is, the
 * one closest to the root, and which side of the string that falls on is precisely what the
 * direction says. Everything that follows --`getPrefix`, `startsWith`, `add`-- speaks in indices,
 * so it inherits that convention without mentioning it again.
 *
 * <h2>The `Properties` is not copied, on purpose</h2>
 *
 * <p>`mySyntax` keeps the object that was passed, not a copy, and `clone` passes it to the new
 * name. It is what the real JDK does and it makes sense: a provider has **one** syntax instance and
 * shares it among all its names. The price is that modifying that `Properties` after building a
 * name is the caller's error; the syntax was already read into the `NameImpl` and the name does not
 * notice.
 *
 * <h2>Comparing is asymmetric</h2>
 *
 * <p>`equals`, `compareTo`, `startsWith` and `endsWith` normalize with **this** name's syntax, not
 * the other's. If this one ignores case and the other's does not, `a.equals(b)` may be `true` and
 * `b.equals(a)` `false`. It is in the JDK's contract and cannot be fixed without changing it.
 */
public class CompoundName implements Name {

    private static final long serialVersionUID = 3513100557083972036L;

    /**
     * See the `transient` note in `CompositeName`: the serial form is its own, and is not written.
     */
    private transient NameImpl impl;

    /**
     * The syntax, as the constructor's caller passed it. `protected` for subclasses and `transient`
     * because the serial form writes the properties one by one, not the object. (That form is not
     * written either: see the note in `CompositeName`.)
     */
    protected transient Properties mySyntax;

    /**
     * From already split components. See the note on the equivalent constructor in `CompositeName`.
     */
    protected CompoundName(Enumeration<String> comps, Properties syntax) {
        if (syntax == null) {
            throw new NullPointerException();
        }
        mySyntax = syntax;
        impl = new NameImpl(syntax, comps);
    }

    /** Parses `n` with `syntax`. The syntax is mandatory: without it there is no way to split. */
    public CompoundName(String n, Properties syntax) throws InvalidNameException {
        if (syntax == null) {
            throw new NullPointerException();
        }
        mySyntax = syntax;
        impl = new NameImpl(syntax, n);
    }

    /** Parses back **with the same syntax**: the string alone is not enough. */
    @Override
    public String toString() {
        return impl.toString();
    }

    /** Compares components, not syntax: two names with different syntaxes can give `true`. */
    @Override
    public boolean equals(Object obj) {
        return (obj instanceof CompoundName)
            && impl.equals(((CompoundName) obj).impl);
    }

    @Override
    public int hashCode() {
        return impl.hashCode();
    }

    /** Shares the `Properties` with the original; see the class header. */
    @Override
    public Object clone() {
        return new CompoundName(getAll(), mySyntax);
    }

    @Override
    public int compareTo(Object obj) {
        if (!(obj instanceof CompoundName)) {
            throw new ClassCastException("Not a CompoundName");
        }
        return impl.compareTo(((CompoundName) obj).impl);
    }

    @Override
    public int size() {
        return impl.size();
    }

    @Override
    public boolean isEmpty() {
        return impl.isEmpty();
    }

    @Override
    public Enumeration<String> getAll() {
        return impl.getAll();
    }

    @Override
    public String get(int posn) {
        return impl.get(posn);
    }

    /** The new name inherits this syntax: a prefix of an LDAP name is still LDAP. */
    @Override
    public Name getPrefix(int posn) {
        return new CompoundName(impl.getPrefix(posn), mySyntax);
    }

    @Override
    public Name getSuffix(int posn) {
        return new CompoundName(impl.getSuffix(posn), mySyntax);
    }

    @Override
    public boolean startsWith(Name n) {
        return (n instanceof CompoundName) && impl.startsWith(n.size(), n.getAll());
    }

    @Override
    public boolean endsWith(Name n) {
        return (n instanceof CompoundName) && impl.endsWith(n.size(), n.getAll());
    }

    @Override
    public Name addAll(Name suffix) throws InvalidNameException {
        if (suffix instanceof CompoundName) {
            impl.addAll(suffix.getAll());
            return this;
        }
        throw new InvalidNameException("Not a compound name: " + suffix.toString());
    }

    @Override
    public Name addAll(int posn, Name n) throws InvalidNameException {
        if (n instanceof CompoundName) {
            impl.addAll(posn, n.getAll());
            return this;
        }
        throw new InvalidNameException("Not a compound name: " + n.toString());
    }

    @Override
    public Name add(String comp) throws InvalidNameException {
        impl.add(comp);
        return this;
    }

    @Override
    public Name add(int posn, String comp) throws InvalidNameException {
        impl.add(posn, comp);
        return this;
    }

    @Override
    public Object remove(int posn) throws InvalidNameException {
        return impl.remove(posn);
    }
}
