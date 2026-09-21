package javax.naming;

import java.util.Enumeration;

/**
 * The name that **crosses** namespaces, with a fixed syntax.
 *
 * <h2>What a composite name is</h2>
 *
 * <p>`"jdbc/pool/sales"` does not live in a single namespace: `jdbc` may be resolved by one
 * context and `pool/sales` by another, from a different provider and with its own syntax. A
 * composite name is the sequence of those stretches. Each component is a name **for another
 * system**, and this class does not know --and has no reason to-- what it means inside.
 *
 * <p>That is why the syntax is fixed and not configurable, which is the only real difference from
 * `CompoundName`: if it depended on the provider there would be no way to write a name that crosses
 * two. It is always: separator `/`, escape `\`, quotes `"` and `'`, left to right, without
 * ignoring case or trimming blanks. Those are literally `NameImpl`'s default values, and that is
 * why this class passes it `null` as the syntax.
 *
 * <h2>Empty components, which is where everybody gets it wrong</h2>
 *
 * <p>A trailing separator adds an empty component: `"a/"` has **two** components, `"a"` and
 * `""`. But `"/"` has **one** --the empty one-- and not two, and `""` has **zero**. The rule
 * that makes the three consistent is that a name whose components are all empty is printed with
 * an extra separator, so that `""` and `{""}` do not collapse into the same string. All that
 * arithmetic is in `NameImpl`; what matters here is that an empty component is a real component
 * and not a parsing artifact.
 *
 * <p>Like every `Name`, it is **mutable**: `add`, `addAll` and `remove` change this and return
 * `this`.
 */
public class CompositeName implements Name {

    private static final long serialVersionUID = 1667768148915813118L;

    /**
     * `transient` because this class's serial form is its own --the number of components and then
     * each one-- and not a dump of the `NameImpl`. That form is not written: the class declares no
     * `writeObject`/`readObject`. An earlier note said this tree had no `ObjectOutputStream`; it
     * has one now, and a `CompositeName` written and read back on this VM comes out with a null
     * `impl`, so its first `size()` throws `NullPointerException`.
     */
    private transient NameImpl impl;

    /**
     * Builds from already split components, without parsing.
     *
     * <p>It is `protected` because it is the constructor used by subclasses and by this class's own
     * methods that return new names --`getPrefix`, `getSuffix`, `clone`--: there the components are
     * already separated, and parsing their string form again would be, besides expensive, a round
     * trip that may lose information.
     */
    protected CompositeName(Enumeration<String> comps) {
        impl = new NameImpl(null, comps);
    }

    /** Parses the string with the composite name's fixed syntax. */
    public CompositeName(String n) throws InvalidNameException {
        impl = new NameImpl(null, n);
    }

    /** The empty name: zero components. */
    public CompositeName() {
        impl = new NameImpl(null);
    }

    /** Parses back: `new CompositeName(x.toString())` equals `x`. */
    @Override
    public String toString() {
        return impl.toString();
    }

    /**
     * Only against another `CompositeName`: a `CompoundName` with the same components is not equal.
     */
    @Override
    public boolean equals(Object obj) {
        return (obj instanceof CompositeName)
            && impl.equals(((CompositeName) obj).impl);
    }

    @Override
    public int hashCode() {
        return impl.hashCode();
    }

    /**
     * Takes `Object` because of the interface's age; throws `ClassCastException` if not one of
     * these.
     */
    @Override
    public int compareTo(Object obj) {
        if (!(obj instanceof CompositeName)) {
            throw new ClassCastException("Not a CompositeName");
        }
        return impl.compareTo(((CompositeName) obj).impl);
    }

    /** A copy with its own component list; the components are `String`s and need no copying. */
    @Override
    public Object clone() {
        return new CompositeName(getAll());
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

    @Override
    public Name getPrefix(int posn) {
        return new CompositeName(impl.getPrefix(posn));
    }

    @Override
    public Name getSuffix(int posn) {
        return new CompositeName(impl.getSuffix(posn));
    }

    // The four that compare with another name return `false` --or throw-- if the other is not
    // composite. A `CompoundName` with the same components means something else: its components
    // belong to a single namespace and these to several.

    @Override
    public boolean startsWith(Name n) {
        return (n instanceof CompositeName) && impl.startsWith(n.size(), n.getAll());
    }

    @Override
    public boolean endsWith(Name n) {
        return (n instanceof CompositeName) && impl.endsWith(n.size(), n.getAll());
    }

    @Override
    public Name addAll(Name suffix) throws InvalidNameException {
        if (suffix instanceof CompositeName) {
            impl.addAll(suffix.getAll());
            return this;
        }
        throw new InvalidNameException("Not a composite name: " + suffix.toString());
    }

    @Override
    public Name addAll(int posn, Name n) throws InvalidNameException {
        if (n instanceof CompositeName) {
            impl.addAll(posn, n.getAll());
            return this;
        }
        throw new InvalidNameException("Not a composite name: " + n.toString());
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
