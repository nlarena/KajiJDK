package javax.management;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The name of an MBean: {@code domain:key=value,key=value,...}.
 *
 * <p>It is the class with the most logic in the whole package, and not for fun. An
 * {@code ObjectName} plays three roles at once and each imposes a condition:
 *
 * <ul>
 *   <li><b>identity</b> -- two names are the same MBean if their <i>canonical forms</i> match.
 *       Hence {@code d:b=2,a=1} and {@code d:a=1,b=2} being equal and sharing a {@code hashCode}:
 *       the order the keys were written in means nothing;
 *   <li><b>literal</b> -- but {@link #toString()} returns the <i>original</i> order, not the
 *       canonical one. Whoever wrote the name sees it again as they wrote it;
 *   <li><b>pattern</b> -- it implements {@link QueryExp}, so a name with wildcards is at the same
 *       time a query applied with {@link #apply}.
 * </ul>
 *
 * <h2>Three kinds of wildcard, not one</h2>
 *
 * <p>The package distinguishes three things that are often confused, and each has its predicate:
 *
 * <ul>
 *   <li>{@link #isDomainPattern()} -- the <b>domain</b> carries {@code *} or {@code ?}:
 *       {@code *:k=v};
 *   <li>{@link #isPropertyListPattern()} -- there is a loose {@code *} in the list, meaning
 *       "besides these keys, any other": {@code d:k=v,*};
 *   <li>{@link #isPropertyValuePattern()} -- some <b>value</b> carries a wildcard: {@code d:k=a*b}.
 * </ul>
 *
 * <p>Wherever the list's {@code *} is written, the canonical form sends it to the end:
 * {@code d:*,k=v} canonicalizes to {@code d:k=v,*}.
 *
 * <h2>Quoting does not turn wildcards off; the backslash does</h2>
 *
 * <p>It is the subtlety that surprises most, and it was verified against the JDK:
 * {@code d:k="a*b"} <b>is</b> a value pattern. Quotes serve to put into a value characters the
 * grammar forbids ({@code , : = "}), not to turn the wildcard off. What turns the wildcard off is
 * the backslash: {@code d:k="a\*b"} is a literal value. That is why {@link #quote} escapes
 * {@code *} and {@code ?}: what comes out of there is never a pattern.
 *
 * <h2>Serialization: it is declared, not invented</h2>
 *
 * <p>{@code Serializable} is inherited from {@link QueryExp} because the contract asks for it -- an
 * {@code ObjectName} travels to the remote agent. But the JDK gives it a <b>serial form of its
 * own</b>, with {@code writeObject} and {@code readObject} that write the string and rebuild the
 * parse. Those two methods are not here. An earlier note explained that by saying this library had
 * no {@code ObjectOutputStream}; it has one now, and the consequence is worth knowing: writing an
 * {@code ObjectName} on this VM throws {@code NotSerializableException}, because the private
 * {@code Property} class that holds the keys is not {@code Serializable} (the JDK writes it and
 * reads it back).
 */
public class ObjectName implements Comparable<ObjectName>, QueryExp {

    private static final long serialVersionUID = 1081892073854801359L;

    /** The name that matches everything: {@code *:*}. */
    public static final ObjectName WILDCARD = wildcard();

    /**
     * A key with its value as it was written (the value keeps the quotes if it had them) and
     * whether that value is a pattern.
     */
    private static class Property {
        final String key;
        final String value;
        final boolean pattern;

        Property(String key, String value, boolean pattern) {
            this.key = key;
            this.value = value;
            this.pattern = pattern;
        }
    }

    private String domainName;
    private Property[] inOrder;      // as they were written
    private Property[] canonicals;    // sorted by key
    private String canonical;
    private boolean domainPattern;
    private boolean listPattern;
    private boolean valuePattern;

    /** Internal use: builds {@code *:*} without going through the parser. */
    private static ObjectName wildcard() {
        ObjectName n = new ObjectName();
        n.domainName = "*";
        n.inOrder = new Property[0];
        n.canonicals = new Property[0];
        n.domainPattern = true;
        n.listPattern = true;
        n.canonical = "*:*";
        return n;
    }

    private ObjectName() {
    }

    /**
     * Parses {@code domain:list}.
     *
     * <p>The empty string is not an error: it means {@code *:*}. It is like that in the JDK and is
     * what makes {@code new ObjectName("")} a useful wildcard and not an exception.
     */
    public ObjectName(String name) throws MalformedObjectNameException {
        construct(name);
    }

    /** Shortcut for the single-key case. */
    public ObjectName(String domain, String key, String value) throws MalformedObjectNameException {
        Map<String, String> table = new LinkedHashMap<String, String>();
        if (key == null) {
            throw new NullPointerException("Invalid key (null)");
        }
        if (value == null) {
            throw new NullPointerException("Invalid value (null)");
        }
        table.put(key, value);
        construct(domain, table);
    }

    /**
     * With the key list already built.
     *
     * <p>The literal order comes from walking the table: a {@code Hashtable} promises none, so
     * {@link #toString()} of a name built this way is not predictable. The <b>canonical</b> form
     * is, and it is the one that defines identity.
     */
    public ObjectName(String domain, Hashtable<String, String> table)
            throws MalformedObjectNameException {
        if (table == null) {
            throw new NullPointerException("key property list cannot be null");
        }
        Map<String, String> copy = new LinkedHashMap<String, String>();
        Iterator<Map.Entry<String, String>> it = table.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> e = it.next();
            if (e.getKey() == null) {
                throw new NullPointerException("Invalid key (null)");
            }
            if (e.getValue() == null) {
                throw new NullPointerException("Invalid value (null)");
            }
            copy.put(e.getKey(), e.getValue());
        }
        construct(domain, copy);
    }

    // ---- factories -----------------------------------------------------------------------------

    /**
     * The same as the constructor, but it may return a shared instance.
     *
     * <p>The JDK prefers it over {@code new} because a subclass of {@code ObjectName} cannot be
     * trusted -- it could lie in {@code equals}-- and this factory always returns an exact
     * {@code ObjectName}.
     */
    public static ObjectName getInstance(String name) throws MalformedObjectNameException {
        return new ObjectName(name);
    }

    public static ObjectName getInstance(String domain, String key, String value)
            throws MalformedObjectNameException {
        return new ObjectName(domain, key, value);
    }

    public static ObjectName getInstance(String domain, Hashtable<String, String> table)
            throws MalformedObjectNameException {
        return new ObjectName(domain, table);
    }

    /**
     * Returns {@code name} if it is already an exact {@code ObjectName}; if it is of a subclass, it
     * copies it.
     *
     * <p>That is the whole point of the method: to guarantee that what is returned is not a
     * subclass with behaviour of its own.
     */
    public static ObjectName getInstance(ObjectName name) {
        if (name == null) {
            throw new NullPointerException("name cannot be null");
        }
        if (name.getClass() == ObjectName.class) {
            return name;
        }
        ObjectName copy = new ObjectName();
        copy.domainName = name.domainName;
        copy.inOrder = name.inOrder;
        copy.canonicals = name.canonicals;
        copy.canonical = name.canonical;
        copy.domainPattern = name.domainPattern;
        copy.listPattern = name.listPattern;
        copy.valuePattern = name.valuePattern;
        return copy;
    }

    // ---- parsing -------------------------------------------------------------------------------

    private void construct(String name) throws MalformedObjectNameException {
        if (name == null) {
            throw new NullPointerException("name cannot be null");
        }
        if (name.length() == 0) {
            domainName = "*";
            inOrder = new Property[0];
            canonicals = new Property[0];
            domainPattern = true;
            listPattern = true;
            canonical = "*:*";
            return;
        }
        int cut = name.indexOf(':');
        if (cut < 0) {
            throw new MalformedObjectNameException("Key properties cannot be empty");
        }
        domainName = name.substring(0, cut);
        checkDomain(domainName);
        domainPattern = domainName.indexOf('*') >= 0 || domainName.indexOf('?') >= 0;

        String list = name.substring(cut + 1);
        if (list.length() == 0) {
            throw new MalformedObjectNameException("Key properties cannot be empty");
        }
        parseList(list);
        buildCanonical();
    }

    /**
     * The domain forbids only two characters.
     *
     * <p>The {@code :} cannot be there because we cut at the first one; the newline does have to be
     * checked. Everything else --spaces, commas, quotes, equals signs-- is a valid domain, however
     * odd it looks; it was checked against the JDK.
     */
    private static void checkDomain(String d) throws MalformedObjectNameException {
        for (int i = 0; i < d.length(); i++) {
            char c = d.charAt(i);
            if (c == ':') {
                throw new MalformedObjectNameException("Invalid character ':' in domain name");
            }
            if (c == '\n') {
                throw new MalformedObjectNameException("Invalid character '\\n' in domain name");
            }
        }
    }

    private void parseList(String s) throws MalformedObjectNameException {
        // A `List` and not a `LinkedHashMap`. The reason is concrete: the writing order is part of
        // the contract --`toString()` has to return the name as it was written-- and this library's
        // `LinkedHashMap` declares that its views (`keySet`, and therefore `values` and `entrySet`)
        // do not keep insertion order. Depending on it made `d:z=1,a=2,m=3` print reordered.
        // Duplicate keys are detected with a walk: an MBean's key lists have a handful of elements.
        List<Property> seen = new ArrayList<Property>();
        int i = 0;
        int n = s.length();
        while (true) {
            if (s.charAt(i) == '*') {
                if (listPattern) {
                    throw new MalformedObjectNameException(
                            "Cannot have several '*' characters in pattern property list");
                }
                listPattern = true;
                i++;
                if (i == n) {
                    break;
                }
                if (s.charAt(i) != ',') {
                    throw new MalformedObjectNameException(
                            "Invalid character found after '*': end of name or ',' expected");
                }
                i++;
                // A checked JDK oddity: after "*," the end of the string is accepted, though after
                // "k=v," it is not. It is not symmetric and it is respected as is.
                if (i == n) {
                    break;
                }
                continue;
            }

            int keyStart = i;
            while (i < n && s.charAt(i) != '=') {
                char c = s.charAt(i);
                if (c == ':' || c == ',' || c == '*' || c == '?' || c == '\n') {
                    throw new MalformedObjectNameException(
                            "Invalid character '" + c + "' in key part of property");
                }
                i++;
            }
            if (i == n) {
                throw new MalformedObjectNameException("Unterminated key property part");
            }
            String key = s.substring(keyStart, i);
            if (key.length() == 0) {
                throw new MalformedObjectNameException("Invalid key (empty)");
            }
            i++; // the '='

            int valueStart = i;
            boolean patternValue = false;
            if (i < n && s.charAt(i) == '"') {
                i++;
                boolean closed = false;
                while (i < n) {
                    char c = s.charAt(i);
                    if (c == '\\') {
                        if (i + 1 >= n) {
                            throw new MalformedObjectNameException("Unterminated quoted value");
                        }
                        char e = s.charAt(i + 1);
                        if (e != 'n' && e != '\\' && e != '"' && e != '*' && e != '?') {
                            throw new MalformedObjectNameException(
                                    "Invalid escape sequence '\\" + e + "' in quoted value");
                        }
                        i += 2;
                        continue;
                    }
                    if (c == '\n') {
                        throw new MalformedObjectNameException("Newline in quoted value");
                    }
                    if (c == '*' || c == '?') {
                        patternValue = true;
                        i++;
                        continue;
                    }
                    if (c == '"') {
                        closed = true;
                        i++;
                        break;
                    }
                    i++;
                }
                if (!closed) {
                    throw new MalformedObjectNameException("Unterminated quoted value");
                }
                if (i < n && s.charAt(i) != ',') {
                    throw new MalformedObjectNameException(
                            "Invalid ending character `" + s.charAt(i) + "'");
                }
            } else {
                while (i < n) {
                    char c = s.charAt(i);
                    if (c == ',') {
                        break;
                    }
                    if (c == '=' || c == ':' || c == '"' || c == '\n') {
                        throw new MalformedObjectNameException(
                                "Invalid character '" + c + "' in value part of property");
                    }
                    if (c == '*' || c == '?') {
                        patternValue = true;
                    }
                    i++;
                }
            }
            String value = s.substring(valueStart, i);
            for (int v = 0; v < seen.size(); v++) {
                if (seen.get(v).key.equals(key)) {
                    throw new MalformedObjectNameException("key `" + key + "' already defined");
                }
            }
            seen.add(new Property(key, value, patternValue));
            if (patternValue) {
                valuePattern = true;
            }

            if (i == n) {
                break;
            }
            i++; // the ','
            if (i == n) {
                throw new MalformedObjectNameException("Invalid ending comma");
            }
        }
        inOrder = seen.toArray(new Property[seen.size()]);
    }

    /**
     * The path of the constructors that already receive the keys split.
     *
     * <p>There is no list to parse here, but every key and every value goes through the same
     * character rules: if they did not, the name could not be read back from its own string.
     */
    private void construct(String domain, Map<String, String> table)
            throws MalformedObjectNameException {
        if (domain == null) {
            throw new NullPointerException("domain cannot be null");
        }
        if (domain.indexOf(':') >= 0 || domain.indexOf('\n') >= 0) {
            throw new MalformedObjectNameException("Invalid domain: " + domain);
        }
        domainName = domain;
        domainPattern = domain.indexOf('*') >= 0 || domain.indexOf('?') >= 0;
        if (table.isEmpty()) {
            throw new MalformedObjectNameException("key property list cannot be empty");
        }
        Property[] props = new Property[table.size()];
        int k = 0;
        Iterator<Map.Entry<String, String>> it = table.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> e = it.next();
            String key = e.getKey();
            String value = e.getValue();
            checkKey(key);
            boolean pat = checkValue(value);
            if (pat) {
                valuePattern = true;
            }
            props[k++] = new Property(key, value, pat);
        }
        inOrder = props;
        buildCanonical();
    }

    private static void checkKey(String key) throws MalformedObjectNameException {
        if (key.length() == 0) {
            throw new MalformedObjectNameException("Invalid key (empty)");
        }
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (c == '=' || c == ':' || c == ',' || c == '*' || c == '?' || c == '\n') {
                throw new MalformedObjectNameException("Invalid character in key: `" + c + "'");
            }
        }
    }

    /** Returns whether the value is a pattern; throws if it is not a legal value. */
    private static boolean checkValue(String value) throws MalformedObjectNameException {
        if (value.length() == 0) {
            return false;
        }
        boolean pat = false;
        if (value.charAt(0) == '"') {
            int i = 1;
            int n = value.length();
            boolean closed = false;
            while (i < n) {
                char c = value.charAt(i);
                if (c == '\\') {
                    if (i + 1 >= n) {
                        throw new MalformedObjectNameException("Unterminated quoted value");
                    }
                    char e = value.charAt(i + 1);
                    if (e != 'n' && e != '\\' && e != '"' && e != '*' && e != '?') {
                        throw new MalformedObjectNameException(
                                "Invalid escape sequence '\\" + e + "' in quoted value");
                    }
                    i += 2;
                    continue;
                }
                if (c == '\n') {
                    throw new MalformedObjectNameException("Newline in quoted value");
                }
                if (c == '*' || c == '?') {
                    pat = true;
                }
                if (c == '"') {
                    closed = true;
                    i++;
                    break;
                }
                i++;
            }
            if (!closed || i != n) {
                throw new MalformedObjectNameException("Invalid value: " + value);
            }
            return pat;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '=' || c == ':' || c == ',' || c == '"' || c == '\n') {
                throw new MalformedObjectNameException("Invalid character in value: `" + c + "'");
            }
            if (c == '*' || c == '?') {
                pat = true;
            }
        }
        return pat;
    }

    /**
     * Sorts the keys and builds the string that defines identity.
     *
     * <p>The order is {@code String.compareTo}'s over the key, that is, by code unit: upper case
     * comes before lower case, so that {@code B=2,a=1,C=3} canonicalizes to {@code B=2,C=3,a=1}. It
     * is not "human" alphabetical and must not be -- it has to be the same order in every JMX
     * implementation or two agents would not understand each other.
     */
    private void buildCanonical() {
        Property[] orden = new Property[inOrder.length];
        System.arraycopy(inOrder, 0, orden, 0, inOrder.length);
        // Insertion sort: an MBean's key lists have a handful of elements.
        for (int i = 1; i < orden.length; i++) {
            Property p = orden[i];
            int j = i - 1;
            while (j >= 0 && orden[j].key.compareTo(p.key) > 0) {
                orden[j + 1] = orden[j];
                j--;
            }
            orden[j + 1] = p;
        }
        canonicals = orden;
        StringBuilder b = new StringBuilder();
        b.append(domainName).append(':');
        b.append(join(canonicals));
        if (listPattern) {
            if (canonicals.length > 0) {
                b.append(',');
            }
            b.append('*');
        }
        canonical = b.toString();
    }

    private static String join(Property[] props) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < props.length; i++) {
            if (i > 0) {
                b.append(',');
            }
            b.append(props[i].key).append('=').append(props[i].value);
        }
        return b.toString();
    }

    // ---- shape queries -------------------------------------------------------------------------

    /** Whether the name carries any wildcard, of whichever kind. */
    public boolean isPattern() {
        return domainPattern || listPattern || valuePattern;
    }

    /** Whether the wildcard is in the domain. */
    public boolean isDomainPattern() {
        return domainPattern;
    }

    /**
     * Whether the name admits keys it does not mention.
     *
     * <p>Careful: in the JDK this is {@code isPropertyListPattern() || isPropertyValuePattern()},
     * not just the first. The method's name misleads, which is why the two more precise ones exist.
     */
    public boolean isPropertyPattern() {
        return listPattern || valuePattern;
    }

    /** Whether there is a loose {@code *} in the key list. */
    public boolean isPropertyListPattern() {
        return listPattern;
    }

    /** Whether some value carries a wildcard. */
    public boolean isPropertyValuePattern() {
        return valuePattern;
    }

    /**
     * Whether the value of that particular key carries a wildcard.
     *
     * @throws IllegalArgumentException if the key is not in the name -- asking about an absent key
     *     is a caller's error, not a "no"
     */
    public boolean isPropertyValuePattern(String property) {
        if (property == null) {
            throw new NullPointerException("key property can't be null");
        }
        for (int i = 0; i < canonicals.length; i++) {
            if (canonicals[i].key.equals(property)) {
                return canonicals[i].pattern;
            }
        }
        throw new IllegalArgumentException("key property not found");
    }

    // ---- accessors -----------------------------------------------------------------------------

    /** The form that defines identity: keys sorted, the list's {@code *} at the end. */
    public String getCanonicalName() {
        return canonical;
    }

    /** The domain, without the colon. */
    public String getDomain() {
        return domainName;
    }

    /** The value of a key, or {@code null} if the name does not carry it. */
    public String getKeyProperty(String property) {
        for (int i = 0; i < canonicals.length; i++) {
            if (canonicals[i].key.equals(property)) {
                return canonicals[i].value;
            }
        }
        return null;
    }

    /**
     * The keys as a table.
     *
     * <p>It is a fresh copy on every call: modifying it does not touch the name, which is
     * immutable. The list's {@code *} does not appear -- it is not a key.
     */
    public Hashtable<String, String> getKeyPropertyList() {
        Hashtable<String, String> t = new Hashtable<String, String>();
        for (int i = 0; i < inOrder.length; i++) {
            t.put(inOrder[i].key, inOrder[i].value);
        }
        return t;
    }

    /**
     * The keys in the order they were written, without the {@code *}.
     */
    public String getKeyPropertyListString() {
        return join(inOrder);
    }

    /** The keys in canonical order, without the {@code *}. */
    public String getCanonicalKeyPropertyListString() {
        return join(canonicals);
    }

    /**
     * The name as it was written -- <b>not</b> the canonical one.
     *
     * <p>The only correction it applies is sending the list's {@code *} to the end: {@code d:*,k=v}
     * prints as {@code d:k=v,*}.
     */
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append(domainName).append(':');
        b.append(join(inOrder));
        if (listPattern) {
            if (inOrder.length > 0) {
                b.append(',');
            }
            b.append('*');
        }
        return b.toString();
    }

    /** Identity by canonical form. */
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof ObjectName)) {
            return false;
        }
        return canonical.equals(((ObjectName) object).canonical);
    }

    /** That of the canonical form, so that two equal names share it. */
    public int hashCode() {
        return canonical.hashCode();
    }

    // ---- quoting -------------------------------------------------------------------------------

    /**
     * Wraps {@code s} in quotes, escaping everything the grammar could misread.
     *
     * <p>It escapes {@code \}, {@code "}, {@code *}, {@code ?} and the newline. The two wildcards
     * are on the list for the reason given above: quoting does not turn them off, escaping does.
     * What comes out of here is always a literal value, never a pattern -- which is exactly what
     * the method is for.
     */
    public static String quote(String s) {
        if (s == null) {
            throw new NullPointerException("s cannot be null");
        }
        StringBuilder b = new StringBuilder();
        b.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\n') {
                b.append("\\n");
            } else if (c == '\\' || c == '"' || c == '*' || c == '?') {
                b.append('\\').append(c);
            } else {
                b.append(c);
            }
        }
        b.append('"');
        return b.toString();
    }

    /**
     * The inverse of {@link #quote}.
     *
     * <p>It is <b>stricter</b> than the name parser: an unescaped {@code *} inside the quotes is an
     * error here, even though {@code d:k="a*b"} is a valid name. The difference makes sense: the
     * parser accepts patterns, and this method promises to return the literal text {@code quote}
     * had received, which a pattern does not have.
     */
    public static String unquote(String q) {
        if (q == null) {
            throw new NullPointerException("q cannot be null");
        }
        int n = q.length();
        if (n < 2 || q.charAt(0) != '"' || q.charAt(n - 1) != '"') {
            throw new IllegalArgumentException("Argument not quoted");
        }
        StringBuilder b = new StringBuilder();
        int i = 1;
        while (i < n - 1) {
            char c = q.charAt(i);
            if (c == '\\') {
                if (i + 1 >= n - 1) {
                    throw new IllegalArgumentException("Trailing backslash");
                }
                char e = q.charAt(i + 1);
                if (e == 'n') {
                    b.append('\n');
                } else if (e == '\\' || e == '"' || e == '*' || e == '?') {
                    b.append(e);
                } else {
                    throw new IllegalArgumentException("Bad character '" + e + "' after backslash");
                }
                i += 2;
                continue;
            }
            if (c == '"' || c == '*' || c == '?' || c == '\n') {
                throw new IllegalArgumentException(
                        "Invalid unescaped character '" + c + "' in the string to unquote");
            }
            b.append(c);
            i++;
        }
        return b.toString();
    }

    // ---- the name as a query -------------------------------------------------------------------

    /**
     * Whether {@code name} is one of the MBeans this pattern designates.
     *
     * <p>Two rules that are not obvious and were verified:
     *
     * <ul>
     *   <li>if {@code name} is itself a pattern, the answer is <b>always</b> {@code false}. A
     *       pattern does not designate another pattern; it designates concrete names;
     *   <li>if this name is not a pattern, it reduces to canonical equality.
     * </ul>
     */
    public boolean apply(ObjectName name) {
        if (name == null) {
            throw new NullPointerException("name cannot be null");
        }
        if (name.isPattern()) {
            return false;
        }
        if (!isPattern()) {
            return canonical.equals(name.canonical);
        }
        return domainMatches(name) && keysMatch(name);
    }

    private boolean domainMatches(ObjectName name) {
        if (!domainPattern) {
            return domainName.equals(name.domainName);
        }
        return wildcardMatches(domainName, name.domainName);
    }

    private boolean keysMatch(ObjectName name) {
        // Without a list `*`, the key sets have to be identical; with it, it is enough that the
        // named keys are there.
        if (!listPattern && canonicals.length != name.canonicals.length) {
            return false;
        }
        for (int i = 0; i < canonicals.length; i++) {
            Property p = canonicals[i];
            String other = name.getKeyProperty(p.key);
            if (other == null) {
                return false;
            }
            if (p.pattern) {
                if (!wildcardMatches(p.value, other)) {
                    return false;
                }
            } else if (!p.value.equals(other)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Matching with {@code *} (any number) and {@code ?} (exactly one).
     *
     * <p>Simple backtracking with two indices, without recursion: an {@code ObjectName}'s patterns
     * are short and this cannot overflow the stack with a hostile input.
     */
    private static boolean wildcardMatches(String pattern, String text) {
        int p = 0;
        int t = 0;
        int star = -1;
        int mark = 0;
        while (t < text.length()) {
            if (p < pattern.length() && (pattern.charAt(p) == '?'
                    || pattern.charAt(p) == text.charAt(t))) {
                p++;
                t++;
            } else if (p < pattern.length() && pattern.charAt(p) == '*') {
                star = p;
                mark = t;
                p++;
            } else if (star >= 0) {
                p = star + 1;
                mark++;
                t = mark;
            } else {
                return false;
            }
        }
        while (p < pattern.length() && pattern.charAt(p) == '*') {
            p++;
        }
        return p == pattern.length();
    }

    /**
     * Does nothing, and cannot do anything else: an {@code ObjectName} is evaluated by looking only
     * at the name.
     *
     * <p>It is here because {@link QueryExp} demands it, not because it is needed.
     */
    public void setMBeanServer(MBeanServer mbs) {
    }

    // ---- order ---------------------------------------------------------------------------------

    /**
     * Total order: domain, then the {@code type} key, then the canonical form.
     *
     * <p>The middle step is the oddity worth knowing: JMX privileges {@code type} over the other
     * keys because it is the one that groups related MBeans, and sorting by it keeps all those of
     * the same type together in a listing. A name without {@code type} counts as having it empty,
     * that is, it goes first.
     */
    public int compareTo(ObjectName name) {
        int d = domainName.compareTo(name.domainName);
        if (d != 0) {
            return d;
        }
        String myType = getKeyProperty("type");
        String theirType = name.getKeyProperty("type");
        if (myType == null) {
            myType = "";
        }
        if (theirType == null) {
            theirType = "";
        }
        int t = myType.compareTo(theirType);
        if (t != 0) {
            return t;
        }
        return canonical.compareTo(name.canonical);
    }
}
