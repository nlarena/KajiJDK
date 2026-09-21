package javax.naming.ldap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.naming.InvalidNameException;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;

/**
 * A component of a distinguished name: {@code cn=John}, and sometimes more than one pair at once.
 *
 * <h2>Why it can have several pairs</h2>
 *
 * <p>A <em>multi-valued</em> RDN --{@code cn=John+ou=Sales}-- exists for when a single attribute
 * is not enough to tell two sibling entries apart. It is rare, and it is the reason this class has
 * {@link #size} and {@link #toAttributes} instead of being a plain type/value pair.
 *
 * <p>{@link #getType} and {@link #getValue} return <strong>one</strong> of them, not all; with
 * several pairs you have to go through {@link #toAttributes}.
 *
 * <h2>Escaping, which is where the bugs are</h2>
 *
 * <p>A value may contain the characters the syntax uses as separators: {@code ,}, {@code +},
 * {@code =}, {@code "}, {@code \}, {@code <}, {@code >}, {@code ;}. Writing a name without
 * escaping them produces something that parses differently from what was meant -- and since it
 * parses <em>fine</em>, the error is silent.
 *
 * <p>{@link #escapeValue} and {@link #unescapeValue} are inverses, and they are static precisely
 * so they can be used when building a name by hand.
 *
 * <p>Space has a rule of its own that surprises: it is only escaped at the start and at the end,
 * because in the middle it is not ambiguous. Here only the first and last characters count as
 * the edges; the JDK escapes every leading and trailing whitespace character. {@code #} only means
 * something as the first character --it marks a hexadecimal value-- but it is escaped wherever it
 * appears, here and in the JDK. (An earlier note implied it was escaped only in first position.)
 */
public class Rdn implements Serializable, Comparable<Object> {

    private static final long serialVersionUID = -5994465067210009656L;

    private static final String ESCAPED = ",=+<>#;\"\\";

    private final List<String> types = new ArrayList<String>();
    private final List<Object> values = new ArrayList<Object>();

    /**
     * From a set of attributes; each one contributes a pair.
     *
     * @throws InvalidNameException if the set is empty, or if an attribute has no value
     */
    public Rdn(Attributes attrSet) throws InvalidNameException {
        if (attrSet == null || attrSet.size() == 0) {
            throw new InvalidNameException("an RDN needs at least one attribute");
        }
        try {
            NamingEnumeration<? extends Attribute> e = attrSet.getAll();
            while (e.hasMore()) {
                Attribute a = e.next();
                if (a.size() == 0) {
                    throw new InvalidNameException("the attribute " + a.getID() + " has no value");
                }
                this.types.add(a.getID());
                this.values.add(a.get());
            }
        } catch (InvalidNameException e) {
            throw e;
        } catch (Exception e) {
            InvalidNameException x = new InvalidNameException("could not read the attributes");
            x.initCause(e);
            throw x;
        }
        sortTypes();
    }

    /**
     * From its text form: {@code "cn=John"} or {@code "cn=John+ou=Sales"}.
     *
     * @throws InvalidNameException if it is not a valid RDN
     */
    public Rdn(String rdnString) throws InvalidNameException {
        parse(rdnString);
        sortTypes();
    }

    /** A copy. */
    public Rdn(Rdn rdn) {
        this.types.addAll(rdn.types);
        this.values.addAll(rdn.values);
    }

    /**
     * With a single pair.
     *
     * @throws InvalidNameException if the type is empty
     */
    public Rdn(String type, Object value) throws InvalidNameException {
        if (type == null || type.isEmpty()) {
            throw new InvalidNameException("the type cannot be empty");
        }
        if (value == null) {
            throw new InvalidNameException("the value cannot be null");
        }
        this.types.add(type);
        this.values.add(value);
    }

    /**
     * The pairs are kept sorted by type, case-insensitively.
     *
     * <p>It is not cosmetic: {@code cn=a+ou=b} and {@code ou=b+cn=a} are <strong>the same</strong>
     * RDN according to the RFC, and without a canonical order neither {@code equals} nor {@code
     * compareTo} could tell.
     */
    private void sortTypes() {
        for (int i = 1; i < this.types.size(); i++) {
            for (int j = i; j > 0; j--) {
                if (this.types.get(j).compareToIgnoreCase(this.types.get(j - 1)) < 0) {
                    Collections.swap(this.types, j, j - 1);
                    Collections.swap(this.values, j, j - 1);
                } else {
                    break;
                }
            }
        }
    }

    private void parse(String s) throws InvalidNameException {
        if (s == null) {
            throw new InvalidNameException("the RDN cannot be null");
        }
        int i = 0;
        int n = s.length();
        while (true) {
            int eqPos = indexOutsideQuotes(s, i, '=');
            if (eqPos < 0) {
                throw new InvalidNameException("missing '=' in: " + s);
            }
            String type = s.substring(i, eqPos).trim();
            if (type.isEmpty()) {
                throw new InvalidNameException("empty type in: " + s);
            }
            int plusPos = indexOutsideQuotes(s, eqPos + 1, '+');
            int end = plusPos < 0 ? n : plusPos;
            String value = s.substring(eqPos + 1, end);
            this.types.add(type);
            this.values.add(unescapeValue(value));
            if (plusPos < 0) {
                return;
            }
            i = plusPos + 1;
        }
    }

    /**
     * Looks for {@code c} outside quotes and skipping escaped ones.
     *
     * <p>A naive search would split {@code cn=a\+b} into two pairs, which is exactly the silent
     * error escaping exists to prevent.
     */
    private static int indexOutsideQuotes(String s, int from, char c) {
        boolean inQuotes = false;
        for (int i = from; i < s.length(); i++) {
            char d = s.charAt(i);
            if (d == '\\') {
                i++;
            } else if (d == '"') {
                inQuotes = !inQuotes;
            } else if (d == c && !inQuotes) {
                return i;
            }
        }
        return -1;
    }

    /** One of the values. With several pairs, which one is unspecified. */
    public Object getValue() {
        return this.values.get(0);
    }

    /** One of the types. */
    public String getType() {
        return this.types.get(0);
    }

    /** The text form, with the values escaped. */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.types.size(); i++) {
            if (i > 0) {
                sb.append('+');
            }
            sb.append(this.types.get(i)).append('=').append(escapeValue(this.values.get(i)));
        }
        return sb.toString();
    }

    /**
     * Compares case-insensitively, type by type and then value by value.
     *
     * @throws ClassCastException if {@code obj} is not an {@link Rdn}
     */
    public int compareTo(Object obj) {
        if (!(obj instanceof Rdn)) {
            throw new ClassCastException("not an Rdn: " + String.valueOf(obj));
        }
        Rdn o = (Rdn) obj;
        int n = Math.min(this.types.size(), o.types.size());
        for (int i = 0; i < n; i++) {
            int c = this.types.get(i).compareToIgnoreCase(o.types.get(i));
            if (c != 0) {
                return c;
            }
            c = String.valueOf(this.values.get(i))
                    .compareToIgnoreCase(String.valueOf(o.values.get(i)));
            if (c != 0) {
                return c;
            }
        }
        return this.types.size() - o.types.size();
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Rdn)) {
            return false;
        }
        return compareTo(obj) == 0;
    }

    /** Over the lower-cased types and values, consistent with {@link #equals}. */
    public int hashCode() {
        int h = 0;
        for (int i = 0; i < this.types.size(); i++) {
            h = h + this.types.get(i).toLowerCase(Locale.ENGLISH).hashCode()
                    + String.valueOf(this.values.get(i)).toLowerCase(Locale.ENGLISH).hashCode();
        }
        return h;
    }

    /** The pairs as a set of attributes. It is the way to see them all when there are several. */
    public Attributes toAttributes() {
        // Case-insensitive identifiers, which is how LDAP works.
        BasicAttributes attrs = new BasicAttributes(true);
        for (int i = 0; i < this.types.size(); i++) {
            attrs.put(this.types.get(i), this.values.get(i));
        }
        return attrs;
    }

    /** How many pairs it has; almost always one. */
    public int size() {
        return this.types.size();
    }

    /**
     * Escapes a value so it can be written in a name.
     *
     * <p>A {@code byte[]} is written as {@code #} followed by hexadecimal, which is the form the
     * RFC defines for what is not text.
     *
     * @throws IllegalArgumentException if the value is neither a string nor a byte array (the JDK
     *     casts to {@code String} and throws {@code ClassCastException})
     */
    public static String escapeValue(Object val) {
        if (val instanceof byte[]) {
            byte[] b = (byte[]) val;
            StringBuilder sb = new StringBuilder(1 + b.length * 2);
            sb.append('#');
            for (int i = 0; i < b.length; i++) {
                int v = b[i] & 0xFF;
                if (v < 16) {
                    sb.append('0');
                }
                sb.append(Integer.toHexString(v));
            }
            return sb.toString();
        }
        if (!(val instanceof String)) {
            throw new IllegalArgumentException(
                    "only a String or a byte[] can be escaped: " + String.valueOf(val));
        }
        String s = (String) val;
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            // Space is only ambiguous at the ends; `#` is in ESCAPED, so leadingHash adds nothing.
            boolean edgeSpace = c == ' ' && (i == 0 || i == s.length() - 1);
            boolean leadingHash = c == '#' && i == 0;
            if (ESCAPED.indexOf(c) >= 0 || edgeSpace || leadingHash) {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * The inverse of {@link #escapeValue}.
     *
     * <p>Returns a {@code byte[]} when the value starts with {@code #}, and a {@link String} in any
     * other case -- hence the return type being {@link Object}.
     *
     * @throws IllegalArgumentException if the text is not a valid value
     */
    public static Object unescapeValue(String val) {
        String s = val.trim();
        if (s.isEmpty()) {
            return "";
        }
        if (s.charAt(0) == '#') {
            String hex = s.substring(1);
            if (hex.length() % 2 != 0) {
                throw new IllegalArgumentException("the hexadecimal has an odd length: " + s);
            }
            byte[] out = new byte[hex.length() / 2];
            for (int i = 0; i < out.length; i++) {
                out[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
            }
            return out;
        }
        StringBuilder sb = new StringBuilder(s.length());
        boolean inQuotes = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\') {
                if (i + 1 >= s.length()) {
                    throw new IllegalArgumentException("trailing backslash in: " + val);
                }
                char d = s.charAt(++i);
                // A `\` may escape a character or introduce a hexadecimal pair.
                if (isHex(d) && i + 1 < s.length() && isHex(s.charAt(i + 1))) {
                    sb.append((char) Integer.parseInt(s.substring(i, i + 2), 16));
                    i++;
                } else {
                    sb.append(d);
                }
            } else if (c == '"') {
                inQuotes = !inQuotes;
            } else {
                sb.append(c);
            }
        }
        if (inQuotes) {
            throw new IllegalArgumentException("unclosed quote in: " + val);
        }
        return sb.toString();
    }

    private static boolean isHex(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }
}
