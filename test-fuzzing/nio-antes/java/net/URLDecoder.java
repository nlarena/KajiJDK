package java.net;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

// `URLEncoder`'s inverse: '+' becomes a space again and "%XX" becomes a byte again.
//
// The one point where this is more than a loop: **consecutive** "%XX" escapes have to be gathered
// before decoding them. A non-ASCII character takes several bytes in UTF-8, and decoding each byte
// separately gives three broken characters instead of one good one. That is why the inner loop
// accumulates the whole run of escapes and only then builds the `String`.
//
// It is pure computation: nothing is omitted.
public final class URLDecoder {

    private URLDecoder() {
    }

    /**
     * Decodes with UTF-8.
     *
     * @deprecated The result depends on the character set; use the overload that asks for it.
     */
    @Deprecated
    public static String decode(String s) {
        return decode(s, StandardCharsets.UTF_8);
    }

    /**
     * Decodes with the character set of that name.
     *
     * @throws UnsupportedEncodingException if the name matches none
     */
    public static String decode(String s, String enc) throws UnsupportedEncodingException {
        if (enc == null) {
            throw new NullPointerException("charsetName");
        }
        Charset cs;
        try {
            cs = Charset.forName(enc);
        } catch (Exception e) {
            throw new UnsupportedEncodingException(enc);
        }
        return decode(s, cs);
    }

    /**
     * Decodes with that character set.
     *
     * @throws IllegalArgumentException if there is a '%' without two hexadecimal digits behind it
     */
    public static String decode(String s, Charset charset) {
        Objects.requireNonNull(charset, "charset");
        boolean changed = false;
        int numChars = s.length();
        StringBuilder sb = new StringBuilder(numChars > 500 ? numChars / 2 : numChars);
        byte[] bytes = null;
        int i = 0;
        while (i < numChars) {
            char c = s.charAt(i);
            if (c == '+') {
                sb.append(' ');
                i = i + 1;
                changed = true;
            } else if (c == '%') {
                if (bytes == null) {
                    bytes = new byte[(numChars - i) / 3];
                }
                int pos = 0;
                while ((i + 2) < numChars && c == '%') {
                    int hi = hexDigit(s.charAt(i + 1));
                    int lo = hexDigit(s.charAt(i + 2));
                    bytes[pos] = (byte) ((hi << 4) | lo);
                    pos = pos + 1;
                    i = i + 3;
                    if (i < numChars) {
                        c = s.charAt(i);
                    }
                }
                if (i < numChars && c == '%') {
                    throw new IllegalArgumentException(
                            "URLDecoder: Incomplete trailing escape (%) pattern");
                }
                sb.append(new String(bytes, 0, pos, charset));
                changed = true;
            } else {
                sb.append(c);
                i = i + 1;
            }
        }
        if (changed) {
            return sb.toString();
        }
        return s;
    }

    private static int hexDigit(char c) {
        int v = InetAddress.digit(c, 16);
        if (v < 0) {
            throw new IllegalArgumentException(
                    "URLDecoder: Illegal hex characters in escape (%) pattern - "
                            + "not a hexadecimal digit: " + c + " = " + ((int) c));
        }
        return v;
    }
}
