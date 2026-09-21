package javax.naming.ldap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.naming.InvalidNameException;
import javax.naming.Name;

/**
 * An LDAP distinguished name: {@code cn=John,ou=Sales,dc=example,dc=com}.
 *
 * <h2>The order, which is the first thing that confuses</h2>
 *
 * <p>A DN is <strong>written</strong> from the most specific to the most general --the person
 * first, the domain last-- and {@link Name} numbers the components the other way round: index
 * {@code 0} is the <em>most</em> significant one, the one closest to the root, that is, the last
 * one written. (An earlier note called it the least significant, which inverts the meaning of
 * "significant" used by {@link Name} and the JDK; the index was right, the word was not.)
 *
 * <p>It is not a whim of this class: it is the {@link Name} convention, which exists so that
 * {@code /a/b/c} and {@code cn=x,dc=y} can be walked with the same API despite being written in
 * opposite directions. The practical consequence is that {@code getRdn(0)} of
 * {@code "cn=John,dc=com"} returns {@code dc=com}, not {@code cn=John}.
 *
 * <h2>It is mutable, unlike almost everything else</h2>
 *
 * <p>{@link #add} and {@link #remove} change this object and return {@code this}. It inherits that
 * from {@link Name}, which was designed that way before immutability was the default reflex.
 * Consequence: <strong>do not share it between threads</strong>, and putting it in a map and then
 * modifying it leaves the key broken.
 */
public class LdapName implements Name {

    private static final long serialVersionUID = -1595520034788997356L;

    /** From the most significant (index 0) to the least; see the class note about the order. */
    private final List<Rdn> rdns;

    /**
     * From its text form.
     *
     * @throws InvalidNameException if it is not a valid DN
     */
    public LdapName(String name) throws InvalidNameException {
        this.rdns = parse(name);
    }

    /**
     * From a list of RDNs, the most significant first.
     *
     * @throws NullPointerException if the list is {@code null}
     */
    public LdapName(List<Rdn> rdns) {
        this.rdns = new ArrayList<Rdn>(rdns);
    }

    private LdapName(List<Rdn> rdns, boolean internal) {
        this.rdns = rdns;
    }

    /**
     * Splits the text into RDNs and reverses them.
     *
     * <p>The comma is looked for <strong>outside quotes and skipping escaped ones</strong>, for the
     * same reason as in {@link Rdn}: a {@code cn=Smith\, John} is a single component, and splitting
     * on every comma produces a different name that still parses fine.
     */
    private static List<Rdn> parse(String name) throws InvalidNameException {
        List<Rdn> out = new ArrayList<Rdn>();
        if (name == null) {
            throw new InvalidNameException("the name cannot be null");
        }
        String s = name.trim();
        if (s.isEmpty()) {
            return out;
        }
        int from = 0;
        boolean inQuotes = false;
        for (int i = 0; i <= s.length(); i++) {
            if (i == s.length()) {
                out.add(new Rdn(s.substring(from).trim()));
                break;
            }
            char c = s.charAt(i);
            if (c == '\\') {
                i++;
            } else if (c == '"') {
                inQuotes = !inQuotes;
            } else if ((c == ',' || c == ';') && !inQuotes) {
                out.add(new Rdn(s.substring(from, i).trim()));
                from = i + 1;
            }
        }
        if (inQuotes) {
            throw new InvalidNameException("unclosed quote in: " + name);
        }
        // The text yields them least significant first; `Name` wants them the other way round.
        Collections.reverse(out);
        return out;
    }

    /** How many components it has. */
    public int size() {
        return this.rdns.size();
    }

    /** Whether it has none. */
    public boolean isEmpty() {
        return this.rdns.isEmpty();
    }

    /** The components as text, from the most significant (index 0) to the least. */
    public Enumeration<String> getAll() {
        List<String> out = new ArrayList<String>(this.rdns.size());
        for (int i = 0; i < this.rdns.size(); i++) {
            out.add(this.rdns.get(i).toString());
        }
        return Collections.enumeration(out);
    }

    /** Component {@code posn} as text. */
    public String get(int posn) {
        return this.rdns.get(posn).toString();
    }

    /** Component {@code posn}. */
    public Rdn getRdn(int posn) {
        return this.rdns.get(posn);
    }

    /**
     * The first {@code posn} components.
     *
     * <p>"Prefix" in {@link Name}'s order, that is, the <em>most general</em> part of the name --
     * the suffix in writing. It is the classic source of confusion in this API.
     */
    public Name getPrefix(int posn) {
        return new LdapName(new ArrayList<Rdn>(this.rdns.subList(0, posn)), true);
    }

    /** The components from {@code posn} onwards. */
    public Name getSuffix(int posn) {
        return new LdapName(new ArrayList<Rdn>(this.rdns.subList(posn, this.rdns.size())), true);
    }

    /** Whether {@code n} is a prefix of this name. */
    public boolean startsWith(Name n) {
        if (!(n instanceof LdapName)) {
            return false;
        }
        return startsWith(((LdapName) n).rdns);
    }

    /** Whether those RDNs are a prefix of this name. */
    public boolean startsWith(List<Rdn> rdns) {
        if (rdns.size() > this.rdns.size()) {
            return false;
        }
        for (int i = 0; i < rdns.size(); i++) {
            if (!this.rdns.get(i).equals(rdns.get(i))) {
                return false;
            }
        }
        return true;
    }

    /** Whether {@code n} is a suffix of this name. */
    public boolean endsWith(Name n) {
        if (!(n instanceof LdapName)) {
            return false;
        }
        return endsWith(((LdapName) n).rdns);
    }

    /** Whether those RDNs are a suffix of this name. */
    public boolean endsWith(List<Rdn> rdns) {
        int d = this.rdns.size() - rdns.size();
        if (d < 0) {
            return false;
        }
        for (int i = 0; i < rdns.size(); i++) {
            if (!this.rdns.get(d + i).equals(rdns.get(i))) {
                return false;
            }
        }
        return true;
    }

    /** Appends those components at the end. */
    public Name addAll(Name suffix) throws InvalidNameException {
        if (!(suffix instanceof LdapName)) {
            throw new InvalidNameException("not an LdapName: " + String.valueOf(suffix));
        }
        return addAll(((LdapName) suffix).rdns);
    }

    /** Appends those components at the end. */
    public Name addAll(List<Rdn> suffixRdns) {
        this.rdns.addAll(suffixRdns);
        return this;
    }

    /** Inserts them at that position. */
    public Name addAll(int posn, Name n) throws InvalidNameException {
        if (!(n instanceof LdapName)) {
            throw new InvalidNameException("not an LdapName: " + String.valueOf(n));
        }
        return addAll(posn, ((LdapName) n).rdns);
    }

    /** Inserts them at that position. */
    public Name addAll(int posn, List<Rdn> rdns) {
        this.rdns.addAll(posn, rdns);
        return this;
    }

    /** Appends a component at the end. */
    public Name add(String comp) throws InvalidNameException {
        this.rdns.add(new Rdn(comp));
        return this;
    }

    /** Appends a component at the end. */
    public Name add(Rdn comp) {
        this.rdns.add(comp);
        return this;
    }

    /** Inserts it at that position. */
    public Name add(int posn, String comp) throws InvalidNameException {
        this.rdns.add(posn, new Rdn(comp));
        return this;
    }

    /** Inserts it at that position. */
    public Name add(int posn, Rdn comp) {
        this.rdns.add(posn, comp);
        return this;
    }

    /** Removes component {@code posn} and returns it as text. */
    public Object remove(int posn) throws InvalidNameException {
        return this.rdns.remove(posn).toString();
    }

    /** The components, in an unmodifiable list. */
    public List<Rdn> getRdns() {
        return Collections.unmodifiableList(this.rdns);
    }

    /** An independent copy: modifying it does not touch this one. */
    public Object clone() {
        return new LdapName(new ArrayList<Rdn>(this.rdns), true);
    }

    /** The text form, least significant first -- that is, the reverse of the indices. */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = this.rdns.size() - 1; i >= 0; i--) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(this.rdns.get(i).toString());
        }
        return sb.toString();
    }

    /** Component by component, case-insensitive. */
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof LdapName)) {
            return false;
        }
        return compareTo(obj) == 0;
    }

    /**
     * Compares component by component, starting from the <strong>last index</strong>, the least
     * significant component (the leftmost one written).
     *
     * <p>This differs from the JDK, which starts from index 0, the most significant component, so
     * that sorting a list of DNs groups them by subtree. Sorted with this one, they group by their
     * leaf RDN instead. (An earlier note described the JDK's order as this method's.)
     *
     * @throws ClassCastException if {@code obj} is not an {@link LdapName}
     */
    public int compareTo(Object obj) {
        if (!(obj instanceof LdapName)) {
            throw new ClassCastException("not an LdapName: " + String.valueOf(obj));
        }
        LdapName o = (LdapName) obj;
        int i = this.rdns.size() - 1;
        int j = o.rdns.size() - 1;
        while (i >= 0 && j >= 0) {
            int c = this.rdns.get(i).compareTo(o.rdns.get(j));
            if (c != 0) {
                return c;
            }
            i--;
            j--;
        }
        return this.rdns.size() - o.rdns.size();
    }

    /** Over the components, consistent with {@link #equals}. */
    public int hashCode() {
        int h = 0;
        for (int i = 0; i < this.rdns.size(); i++) {
            h = h + this.rdns.get(i).hashCode();
        }
        return h;
    }
}
