package javax.security.auth.x500;

import java.util.Map;

/**
 * KajiLibrary's javax.security.auth.x500.NameFormat -- writes a name in the three formats.
 *
 * <p>The three walk the same steps in the same order and differ in four decisions: how the type is
 * named, how the value is written, what separates one step from the next, and what separates two
 * pairs within a step. They are together because of that: separating them would give three walks
 * that can drift out of sync, and a name that is written differently depending on the format stops
 * being the same name.
 *
 * <p>(The note called the class {@code Formato}, an earlier name.)
 *
 * <h2>The three rules that are not obvious</h2>
 *
 * <p>All taken from asking JDK 25, not from reading the RFC -- which in the three cases leaves room
 * for more than one reading:
 *
 * <ol>
 *   <li><b>A type without a keyword forces the value to hexadecimal</b> in RFC 2253 and in
 *       canonical: `1.2.3.4=#1304616c676f` and not `1.2.3.4=algo`. The reason is that without a
 *       keyword there is no agreed text form for the value either, so the raw DER is written. If
 *       the caller passes a dictionary that **does** name that OID, it is written as text again.
 *   <li><b>RFC 1779 quotes instead of escaping</b>: `CN="Perez, Juan"` where RFC 2253 puts
 *       `CN=Perez\, Juan`. And it also quotes a value with **two spaces in a row**, which is the
 *       case that gets forgotten.
 *   <li><b>RFC 1779 separates the pairs of a step with ` + `</b>, with spaces, while RFC 2253 uses
 *       a bare `+`.
 * </ol>
 */
final class NameFormat {

    private NameFormat() {
    }

    /**
     * @param legacy RFC 1779: quotes instead of escaping, `OID.x.y`, `, ` and ` + ` as separators
     * @param canonical all in lower case, spaces collapsed, no translations
     */
    static String write(X500Principal.Rdn[] rdns, Map<String, String> oidMap,
            boolean legacy, boolean canonical) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < rdns.length) {
            if (i > 0) {
                out.append(legacy ? ", " : ",");
            }
            oneRdn(out, rdns[i], oidMap, legacy, canonical);
            i = i + 1;
        }
        return out.toString();
    }

    private static void oneRdn(StringBuilder out, X500Principal.Rdn rdn, Map<String, String> oidMap,
            boolean legacy, boolean canonical) {
        int i = 0;
        while (i < rdn.types.length) {
            if (i > 0) {
                out.append(legacy ? " + " : "+");
            }
            String word = wordFor(rdn.types[i], oidMap, canonical);
            out.append(typeLabel(rdn.types[i], word, legacy, canonical));
            out.append('=');
            out.append(valueText(rdn.values[i], word != null, legacy, canonical));
            i = i + 1;
        }
    }

    // The keyword of that OID, or `null` if it has none. The caller's dictionary wins over the
    // standard's: that is what it is passed for. In canonical there is no dictionary -- see
    // `X500Principal.getName`, which rejects a non-empty one.
    private static String wordFor(String oid, Map<String, String> oidMap, boolean canonical) {
        if (!canonical) {
            String own = oidMap.get(oid);
            if (own != null) {
                return own;
            }
        }
        return X500Principal.wordForOid(oid);
    }

    private static String typeLabel(String oid, String word, boolean legacy,
            boolean canonical) {
        if (word == null) {
            // Without a keyword the OID is written. RFC 1779 gives it the `OID.` prefix; RFC 2253
            // does not, and neither does canonical -- there the bare OID is precisely the stable
            // form.
            return legacy ? ("OID." + oid) : oid;
        }
        return canonical ? word.toLowerCase() : word;
    }

    private static String valueText(String value, boolean hasWord, boolean legacy,
            boolean canonical) {
        // Without a keyword and without RFC 1779: the value goes in hexadecimal. See rule 1 above.
        if (!hasWord && !legacy) {
            return "#" + Der.toHex(Der.writeValue(value)).toLowerCase();
        }
        if (canonical) {
            return AttrValue.canonical(value);
        }
        if (legacy) {
            return quotedLegacy(value);
        }
        return AttrValue.write(value);
    }

    // What RFC 1779 quotes. A value with syntax characters, with spaces at the edges, or with two
    // spaces in a row, goes in quotes; inside, only the quote and the backslash have to be escaped.
    private static String quotedLegacy(String value) {
        if (!needsQuotes(value)) {
            return value;
        }
        StringBuilder sb = new StringBuilder();
        sb.append('"');
        int i = 0;
        while (i < value.length()) {
            char c = value.charAt(i);
            if (c == '"' || c == '\\') {
                sb.append('\\');
            }
            sb.append(c);
            i = i + 1;
        }
        sb.append('"');
        return sb.toString();
    }

    private static boolean needsQuotes(String v) {
        if (v.length() == 0) {
            return false;
        }
        if (v.charAt(0) == ' ' || v.charAt(v.length() - 1) == ' ' || v.charAt(0) == '#') {
            return true;
        }
        int i = 0;
        while (i < v.length()) {
            char c = v.charAt(i);
            if (",+=\"<>;\\".indexOf(c) >= 0) {
                return true;
            }
            if (c == ' ' && i + 1 < v.length() && v.charAt(i + 1) == ' ') {
                return true;
            }
            i = i + 1;
        }
        return false;
    }
}
