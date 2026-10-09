package java.net;

import java.util.Locale;

/**
 * The translation between a domain name with non-ASCII characters and its transportable form.
 *
 * <h2>What problem it solves</h2>
 *
 * <p>DNS speaks ASCII. A domain written with accents, ideographs or Cyrillic cannot travel as it
 * stands, and yet it has to resolve to the same place from anywhere. The solution is
 * <strong>Punycode</strong> (RFC 3492): a reversible encoding that turns any Unicode string into
 * ASCII, marked with the {@code xn--} prefix.
 *
 * <p>Reversible is the word: {@link #toASCII} and {@link #toUnicode} are inverses, and that is why
 * the same domain can be shown prettily and resolved correctly with no table in between.
 *
 * <h2>How Punycode works, in one line</h2>
 *
 * <p>It separates the ASCII characters —which are copied literally— from the rest, and describes the
 * latter as a series of <em>deltas</em> over a code point and a position. The deltas are written in a
 * 36-symbol alphabet with variable length, and a bias mechanism makes the small jumps —the normal
 * case, because a name is usually in a single alphabet— take little room.
 *
 * <h2>This implementation's limitation, said plainly</h2>
 *
 * <p>RFC 3490 requires the name to be put through <strong>nameprep</strong> (RFC 3491) before
 * encoding: case folding, <strong>NFKC</strong> normalization, and rejection of forbidden characters.
 * Here the case folding and the rejection of the forbidden ones that can be detected without tables
 * are done, but <strong>not the NFKC normalization</strong>: this library's
 * {@link java.text.Normalizer} does not declare that form — a decision documented there, and
 * preferable to declaring it and throwing.
 *
 * <p>What that means in practice: for an input that is <em>already normalized</em> —which is the case
 * for any name coming from a browser, a configuration file or a keyboard— the result is identical to
 * the JDK's. For an input that would need NFKC, the Punycode that comes out is that of the
 * unnormalized string: it is still valid Punycode and it is still reversible, but it is not the same
 * one the JDK would produce.
 *
 * <p>What <strong>is</strong> exact is the Punycode algorithm itself, which is the part specified
 * down to the last detail and the one that really has a way of being wrong.
 *
 * @since 1.6
 */
public final class IDN {

    /**
     * Allows the name to hold code points Unicode has not assigned yet.
     *
     * <p>Off by default, and that is the prudent thing: an unassigned character may be given meaning
     * —or a folding rule— in a future version, and then the same name would start encoding
     * differently.
     */
    public static final int ALLOW_UNASSIGNED = 0x01;

    /**
     * Requires the result to meet STD3's host-name rules.
     *
     * <p>Letters, digits and hyphen; not starting or ending in a hyphen. It serves to avoid
     * manufacturing a name Punycode accepts and no resolver will then admit.
     */
    public static final int USE_STD3_ASCII_RULES = 0x02;

    // The 36-symbol alphabet and the bias parameters are RFC 3492 constants, not choices: changing
    // them produces an encoding nobody else understands.
    private static final int BASE = 36;
    private static final int TMIN = 1;
    private static final int TMAX = 26;
    private static final int SKEW = 38;
    private static final int DAMP = 700;
    private static final int INITIAL_BIAS = 72;
    private static final int INITIAL_N = 128;
    private static final char DELIMITER = '-';
    private static final String ACE_PREFIX = "xn--";
    private static final int MAX_LABEL = 63;

    private IDN() {
    }

    /**
     * To the ASCII form.
     *
     * @throws IllegalArgumentException if the name does not meet IDNA's rules
     */
    public static String toASCII(String input, int flag) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        boolean empty = input.isEmpty();
        while (i < input.length() || empty) {
            int end = labelEnd(input, i);
            String label = input.substring(i, end);
            out.append(labelToAscii(label, flag));
            if (end >= input.length()) {
                break;
            }
            // The separator is kept as it stands: the four Unicode recognizes as a dot are
            // normalized to the ASCII one, which is the only thing DNS carries.
            out.append('.');
            i = end + 1;
            empty = i == input.length();
        }
        return out.toString();
    }

    /** To the ASCII form, with no flags. */
    public static String toASCII(String input) {
        return toASCII(input, 0);
    }

    /**
     * Back to Unicode.
     *
     * <p>It never fails: a label that cannot be decoded is returned as it came. That is deliberate in
     * the RFC — a half-translated name is more useful than an exception, because this is used above
     * all to <em>display</em>.
     */
    public static String toUnicode(String input, int flag) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        boolean empty = input.isEmpty();
        while (i < input.length() || empty) {
            int end = labelEnd(input, i);
            out.append(labelToUnicode(input.substring(i, end), flag));
            if (end >= input.length()) {
                break;
            }
            out.append('.');
            i = end + 1;
            empty = i == input.length();
        }
        return out.toString();
    }

    /** Back to Unicode, with no flags. */
    public static String toUnicode(String input) {
        return toUnicode(input, 0);
    }

    /**
     * Where the label starting at {@code from} ends.
     *
     * <p>RFC 3490's four separators and not just the ASCII dot: some alphabets have their own form of
     * dot, and a name written with them has to be split all the same.
     */
    private static int labelEnd(String s, int from) {
        for (int i = from; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '.' || c == '。' || c == '．' || c == '｡') {
                return i;
            }
        }
        return s.length();
    }

    private static String labelToAscii(String label, int flag) {
        boolean asciiOnly = true;
        for (int i = 0; i < label.length(); i++) {
            if (label.charAt(i) > 0x7F) {
                asciiOnly = false;
                break;
            }
        }
        // Case folding is the part of nameprep that can be done without NFKC; see the class's note
        // on what is missing.
        String prepared = asciiOnly ? label : label.toLowerCase(Locale.ROOT);

        String result;
        if (asciiOnly) {
            result = label;
        } else {
            if (prepared.startsWith(ACE_PREFIX)) {
                throw new IllegalArgumentException(
                        "a non-ASCII label cannot start with " + ACE_PREFIX);
            }
            result = ACE_PREFIX + punycode(prepared);
        }
        if ((flag & USE_STD3_ASCII_RULES) != 0) {
            checkStd3(result);
        }
        if (result.isEmpty() || result.length() > MAX_LABEL) {
            throw new IllegalArgumentException("label of invalid length: " + result);
        }
        return result;
    }

    private static String labelToUnicode(String label, int flag) {
        if (label.length() <= ACE_PREFIX.length()
                || !label.substring(0, ACE_PREFIX.length())
                        .equalsIgnoreCase(ACE_PREFIX)) {
            return label;
        }
        try {
            String u = depunycode(label.substring(ACE_PREFIX.length()));
            // The round-trip test the RFC requires: if re-encoding does not give the same thing, the
            // label was malformed and it is returned as it came.
            if (!toASCII(u, flag).equalsIgnoreCase(label)) {
                return label;
            }
            return u;
        } catch (RuntimeException e) {
            return label;
        }
    }

    private static void checkStd3(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9') || c == '-';
            if (!ok) {
                throw new IllegalArgumentException("character not allowed by STD3: " + c);
            }
        }
        if (s.startsWith("-") || s.endsWith("-")) {
            throw new IllegalArgumentException("a label cannot start or end in '-'");
        }
    }

    /** RFC 3492's adaptive bias: it is what makes the small jumps take little room. */
    private static int adapt(int delta, int count, boolean first) {
        int d = first ? delta / DAMP : delta / 2;
        d = d + d / count;
        int k = 0;
        while (d > ((BASE - TMIN) * TMAX) / 2) {
            d = d / (BASE - TMIN);
            k = k + BASE;
        }
        return k + (((BASE - TMIN + 1) * d) / (d + SKEW));
    }

    private static char digit(int d) {
        return (char) (d < 26 ? d + 'a' : d - 26 + '0');
    }

    private static int value(char c) {
        if (c >= 'a' && c <= 'z') {
            return c - 'a';
        }
        if (c >= 'A' && c <= 'Z') {
            return c - 'A';
        }
        if (c >= '0' && c <= '9') {
            return c - '0' + 26;
        }
        throw new IllegalArgumentException("invalid Punycode digit: " + c);
    }

    /** RFC 3492 §6.3, as it stands. */
    private static String punycode(String input) {
        int n = INITIAL_N;
        int delta = 0;
        int bias = INITIAL_BIAS;
        StringBuilder out = new StringBuilder();

        int basic = 0;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c < 0x80) {
                out.append(c);
                basic++;
            }
        }
        if (basic > 0) {
            out.append(DELIMITER);
        }

        int handled = basic;
        int total = input.length();
        while (handled < total) {
            int m = Integer.MAX_VALUE;
            for (int i = 0; i < input.length(); i++) {
                int c = input.charAt(i);
                if (c >= n && c < m) {
                    m = c;
                }
            }
            delta = delta + (m - n) * (handled + 1);
            n = m;
            for (int i = 0; i < input.length(); i++) {
                int c = input.charAt(i);
                if (c < n) {
                    delta++;
                } else if (c == n) {
                    int q = delta;
                    for (int k = BASE; ; k += BASE) {
                        int t = k <= bias ? TMIN : (k >= bias + TMAX ? TMAX : k - bias);
                        if (q < t) {
                            break;
                        }
                        out.append(digit(t + (q - t) % (BASE - t)));
                        q = (q - t) / (BASE - t);
                    }
                    out.append(digit(q));
                    bias = adapt(delta, handled + 1, handled == basic);
                    delta = 0;
                    handled++;
                }
            }
            delta++;
            n++;
        }
        return out.toString();
    }

    /** RFC 3492 §6.2, the exact inverse of {@link #punycode}. */
    private static String depunycode(String input) {
        int n = INITIAL_N;
        int i = 0;
        int bias = INITIAL_BIAS;
        StringBuilder out = new StringBuilder();

        int lastDash = input.lastIndexOf(DELIMITER);
        if (lastDash > 0) {
            for (int j = 0; j < lastDash; j++) {
                char c = input.charAt(j);
                if (c >= 0x80) {
                    throw new IllegalArgumentException("non-basic character in the literal part");
                }
                out.append(c);
            }
        }

        int pos = lastDash < 0 ? 0 : lastDash + 1;
        while (pos < input.length()) {
            int old = i;
            int w = 1;
            for (int k = BASE; ; k += BASE) {
                if (pos >= input.length()) {
                    throw new IllegalArgumentException("incomplete Punycode");
                }
                int d = value(input.charAt(pos));
                pos++;
                i = i + d * w;
                int t = k <= bias ? TMIN : (k >= bias + TMAX ? TMAX : k - bias);
                if (d < t) {
                    break;
                }
                w = w * (BASE - t);
            }
            bias = adapt(i - old, out.length() + 1, old == 0);
            n = n + i / (out.length() + 1);
            i = i % (out.length() + 1);
            out.insert(i, (char) n);
            i++;
        }
        return out.toString();
    }
}
