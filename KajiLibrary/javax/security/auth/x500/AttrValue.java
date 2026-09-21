package javax.security.auth.x500;

/**
 * KajiLibrary's javax.security.auth.x500.AttrValue -- the value of a `type=value`, read and
 * written.
 *
 * <p>It is the part with the most rules in all of RFC 2253, and the one that decides whether two
 * implementations understand each other. It was split into its own class because reading and
 * writing have to be **exact inverses**: if they are not, a name passed through `getName()` and
 * parsed again does not give the same name, and that breaks anything that stores DNs as text.
 *
 * <p>(The note called the class {@code Valor}, an earlier name.)
 */
final class AttrValue {

    private AttrValue() {
    }

    // The ones that always have to be escaped when writing (RFC 4514 §2.4). The comma and the `+`
    // separate; the `"` and the `\` are the syntax itself; `<`, `>` and `;` come from the old RFC
    // 2253; the `#` only bothers at the start, because there it means "what follows is
    // hexadecimal".
    private static final String SPECIALS = ",+\"\\<>;";

    /**
     * The **unescaped** value that text represents.
     *
     * @throws IllegalArgumentException if the escape is malformed
     */
    static String read(String rawBytes) {
        String s = rawBytes;
        // The spaces at the edges do not count unless they are escaped, and that is why they are
        // trimmed **before** unescaping: afterwards a written space can no longer be told from an
        // escaped one.
        int from = 0;
        while (from < s.length() && s.charAt(from) == ' ') {
            from = from + 1;
        }
        int to = s.length();
        while (to > from && s.charAt(to - 1) == ' ' && !isEscaped(s, to - 1)) {
            to = to - 1;
        }
        s = s.substring(from, to);
        if (s.length() == 0) {
            return "";
        }
        if (s.charAt(0) == '#') {
            return fromHex(s.substring(1, s.length()));
        }
        if (s.charAt(0) == '"') {
            return unquote(s);
        }
        return unescape(s);
    }

    // Whether the character at `i` is preceded by an **odd** number of backslashes: two backslashes
    // are a literal backslash, not an escape.
    private static boolean isEscaped(String s, int i) {
        int backslashes = 0;
        int k = i - 1;
        while (k >= 0 && s.charAt(k) == '\\') {
            backslashes = backslashes + 1;
            k = k - 1;
        }
        return backslashes % 2 == 1;
    }

    private static String unquote(String s) {
        if (s.length() < 2 || s.charAt(s.length() - 1) != '"') {
            throw new IllegalArgumentException("missing closing quote: " + s);
        }
        return unescape(s.substring(1, s.length() - 1));
    }

    private static String unescape(String s) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c != '\\') {
                out.append(c);
                i = i + 1;
                continue;
            }
            if (i + 1 >= s.length()) {
                throw new IllegalArgumentException("value ends in a backslash: " + s);
            }
            char sig = s.charAt(i + 1);
            // `\XX` with two hexadecimal digits is a **byte**, not an escaped character. The
            // difference matters: `\41` is an `A` and not a `4` followed by a `1`.
            if (isHex(sig) && i + 2 < s.length() && isHex(s.charAt(i + 2))) {
                out.append((char) ((hexValue(sig) << 4) | hexValue(s.charAt(i + 2))));
                i = i + 3;
            } else {
                out.append(sig);
                i = i + 2;
            }
        }
        return out.toString();
    }

    private static String fromHex(String hex) {
        if (hex.length() == 0 || hex.length() % 2 != 0) {
            throw new IllegalArgumentException("hexadecimal of odd length: #" + hex);
        }
        byte[] bytes = new byte[hex.length() / 2];
        int i = 0;
        while (i < bytes.length) {
            char a = hex.charAt(2 * i);
            char b = hex.charAt(2 * i + 1);
            if (!isHex(a) || !isHex(b)) {
                throw new IllegalArgumentException("not hexadecimal: #" + hex);
            }
            bytes[i] = (byte) ((hexValue(a) << 4) | hexValue(b));
            i = i + 1;
        }
        // The bytes are the value's DER. Whatever can be read as text is read; otherwise the
        // hexadecimal form is kept as is, which is what the JDK shows for an unknown type.
        try {
            return Der.readAttributeValue(bytes);
        } catch (java.io.IOException e) {
            return "#" + hex;
        }
    }

    private static boolean isHex(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private static int hexValue(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        return c - 'A' + 10;
    }

    /**
     * The **escaped** text of that value, ready to go in a DN.
     *
     * <p>The exact inverse of {@link #read}: what comes out of here, parsed, gives the same value
     * again.
     */
    static String write(String value) {
        if (value.length() == 0) {
            return "";
        }
        // A value that already comes in hexadecimal form --because its type could not be read as
        // text-- is passed as is: escaping it would turn it into the text `#30...` instead of the
        // value.
        if (value.charAt(0) == '#' && looksHex(value)) {
            return value;
        }
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < value.length()) {
            char c = value.charAt(i);
            boolean atEdge = i == 0 || i == value.length() - 1;
            if (SPECIALS.indexOf(c) >= 0) {
                out.append('\\').append(c);
            } else if (c == ' ' && atEdge) {
                // Only the spaces at the edges: inside it is not needed and would clutter the name.
                out.append("\\ ");
            } else if (c == '#' && i == 0) {
                out.append("\\#");
            } else if (c < 0x20) {
                out.append('\\').append(hexDigit(c >> 4)).append(hexDigit(c & 0xf));
            } else {
                out.append(c);
            }
            i = i + 1;
        }
        return out.toString();
    }

    private static boolean looksHex(String s) {
        if (s.length() < 3 || (s.length() - 1) % 2 != 0) {
            return false;
        }
        int i = 1;
        while (i < s.length()) {
            if (!isHex(s.charAt(i))) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    private static char hexDigit(int n) {
        return (char) (n < 10 ? '0' + n : 'A' + n - 10);
    }

    /**
     * The value in **canonical** form: lower case, no spaces at the edges and the inner ones
     * collapsed to one.
     *
     * <p>It is what makes two names written differently give the same string. Collapsing the inner
     * spaces --not only trimming the edges-- is the part that gets forgotten: `CN=Juan  Perez` and
     * `CN=Juan Perez` are the same name.
     *
     * <p>The note said this is the only transformation of the canonical format. It is not in the
     * JDK, which also normalizes the value to NFKD (an A with a ring above gives two characters
     * after the `=`) and sorts the pairs of a multi-valued RDN ({@code OU=b+CN=a} gives
     * {@code cn=a+ou=b}). Neither is done here, so those names give a different canonical string
     * than in the JDK.
     */
    static String canonical(String value) {
        StringBuilder out = new StringBuilder();
        boolean spacing = false;
        int i = 0;
        while (i < value.length()) {
            char c = value.charAt(i);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                spacing = true;
            } else {
                if (spacing && out.length() > 0) {
                    out.append(' ');
                }
                spacing = false;
                out.append(Character.toLowerCase(c));
            }
            i = i + 1;
        }
        return write(out.toString());
    }
}
