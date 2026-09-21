package javax.security.auth.x500;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * KajiLibrary's javax.security.auth.x500.Parser -- reads a distinguished name written in RFC 2253.
 *
 * <h2>The grammar, which is smaller than it looks</h2>
 *
 * <p>A name is steps separated by commas; each step is pairs separated by `+`; each pair is
 * `type=value`. Everything else is how a value is written, which has **three** forms that have to
 * be told apart before doing anything:
 *
 * <ul>
 *   <li>**Hexadecimal**: it starts with `#` and is the value's DER, raw. It is used for types that
 *       cannot be written as text.
 *   <li>**In quotes**: inside the quotes almost everything is literal, commas included.
 *   <li>**With escapes**: the normal form. A backslash makes the next character literal, and
 *       besides `\\XX` with two hexadecimal digits puts in a raw byte.
 * </ul>
 *
 * <p>The case that needs thinking about --and where almost all parsers go wrong-- is the **spaces
 * at the edges**: a space at the start or at the end of a value **does not count** unless it is
 * escaped. `CN= Juan ` is `Juan`, and `CN=\\ Juan` is `" Juan"`. Inside the value the spaces are
 * kept as they are.
 */
final class Parser {

    private Parser() {
    }

    /**
     * The steps of the name, in the order they were written: from the most particular to the most
     * general.
     *
     * @throws IllegalArgumentException if the name does not parse
     */
    static X500Principal.Rdn[] parse(String name, Map<String, String> words) {
        List<X500Principal.Rdn> rdns = new ArrayList<X500Principal.Rdn>();
        // An empty name is an empty DN and it is **valid**: it designates the root of the
        // directory. It is not an error, and treating it as one breaks the certificates that use
        // it.
        if (name.trim().length() == 0) {
            return new X500Principal.Rdn[0];
        }
        List<String> parts = splitTopLevel(name, ',');
        int i = 0;
        while (i < parts.size()) {
            rdns.add(oneRdn(parts.get(i), words));
            i = i + 1;
        }
        return rdns.toArray(new X500Principal.Rdn[rdns.size()]);
    }

    // A step: one or more `type=value` joined by `+`.
    private static X500Principal.Rdn oneRdn(String text, Map<String, String> words) {
        List<String> pairs = splitTopLevel(text, '+');
        String[] types = new String[pairs.size()];
        String[] values = new String[pairs.size()];
        int i = 0;
        while (i < pairs.size()) {
            String pair = pairs.get(i);
            int eq = equalsSignIndex(pair);
            if (eq < 0) {
                throw new IllegalArgumentException("missing `=` in: " + pair);
            }
            String type = pair.substring(0, eq).trim();
            String value = pair.substring(eq + 1, pair.length());
            types[i] = toOid(type, words);
            values[i] = AttrValue.read(value);
            i = i + 1;
        }
        return new X500Principal.Rdn(types, values);
    }

    // The first `=` that is not inside quotes nor escaped. Looking for it with `indexOf` would be
    // wrong: a value can contain `=`, and in fact it is common in email addresses.
    private static int equalsSignIndex(String s) {
        boolean inQuotes = false;
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '\\') {
                i = i + 2;
                continue;
            }
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == '=' && !inQuotes) {
                return i;
            }
            i = i + 1;
        }
        return -1;
    }

    /**
     * Splits by that separator, **respecting** quotes and escapes.
     *
     * <p>It is what prevents `CN="Perez, Juan"` --with the comma inside a quoted value-- from being
     * read as two steps. A plain `split` cannot do this, and that is why it is not used.
     */
    private static List<String> splitTopLevel(String s, char separator) {
        List<String> out = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                current.append(c).append(s.charAt(i + 1));
                i = i + 2;
                continue;
            }
            if (c == '"') {
                inQuotes = !inQuotes;
                current.append(c);
            } else if (c == separator && !inQuotes) {
                out.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
            i = i + 1;
        }
        out.add(current.toString());
        return out;
    }

    // The type, always as a numeric OID: a known keyword, one from the map, or already an OID.
    private static String toOid(String type, Map<String, String> words) {
        if (type.length() == 0) {
            throw new IllegalArgumentException("empty type");
        }
        String known = X500Principal.oidForWord(type);
        if (known != null) {
            return known;
        }
        // The caller's map comes after the known ones: it cannot redefine `CN`. In the JDK it is
        // the other way round, the map takes precedence: {"CN" -> "1.2.3.4"} turns `CN=x` into
        // `1.2.3.4=#130178`.
        String fromMap = lookupIgnoringCase(words, type);
        if (fromMap != null) {
            validateOid(fromMap);
            return fromMap;
        }
        String cleanCert = type;
        // `OID.1.2.3` is the long way of writing an OID; RFC 1779 always uses it.
        if (cleanCert.length() > 4 && cleanCert.substring(0, 4).equalsIgnoreCase("OID.")) {
            cleanCert = cleanCert.substring(4, cleanCert.length());
        }
        validateOid(cleanCert);
        return cleanCert;
    }

    private static String lookupIgnoringCase(Map<String, String> m, String key) {
        for (Map.Entry<String, String> e : m.entrySet()) {
            if (e.getKey() != null && e.getKey().equalsIgnoreCase(key)) {
                return e.getValue();
            }
        }
        return null;
    }

    /**
     * That the OID has the shape of an OID.
     *
     * <p>It is validated when parsing and not when using, and that is why the message can name what
     * was badly written. A malformed OID that gets through parsing reappears much later as a name
     * that matches nothing, and by then nobody knows where it came from.
     */
    static void validateOid(String oid) {
        if (oid == null || oid.length() == 0) {
            throw new IllegalArgumentException("empty OID");
        }
        int arcs = 0;
        int i = 0;
        while (i < oid.length()) {
            int end = i;
            while (end < oid.length() && oid.charAt(end) != '.') {
                end = end + 1;
            }
            if (end == i) {
                throw new IllegalArgumentException("malformed OID: " + oid);
            }
            int k = i;
            while (k < end) {
                if (oid.charAt(k) < '0' || oid.charAt(k) > '9') {
                    throw new IllegalArgumentException("malformed OID: " + oid);
                }
                k = k + 1;
            }
            arcs = arcs + 1;
            i = end + 1;
            if (i == oid.length()) {
                throw new IllegalArgumentException("OID ends in a dot: " + oid);
            }
        }
        // Fewer than two arcs is not an OID: the first chooses the authority and the second the
        // branch.
        if (arcs < 2) {
            throw new IllegalArgumentException("an OID needs at least two arcs: " + oid);
        }
        int first = Integer.parseInt(oid.substring(0, oid.indexOf('.')));
        if (first > 2) {
            throw new IllegalArgumentException("the first arc of an OID is 0, 1 or 2: " + oid);
        }
    }
}
