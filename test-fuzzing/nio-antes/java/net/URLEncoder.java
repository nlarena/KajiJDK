package java.net;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

// The "application/x-www-form-urlencoded" escape: what an HTML form does when it is submitted.
//
// **It is not URL escaping**, although the name suggests it, and confusing them is the classic
// mistake. This format comes from forms: the space is encoded as '+' and the slash '/' is encoded,
// which ruins any path. For building a URL there is `java.net.URI`, which applies RFC 3986. This one
// is for building a POST body or a query string, and there it is correct.
//
// The set that is NOT encoded is the JDK's and it is smaller than one expects: letters, digits, and
// only four signs -- '-', '_', '.', '*'. The tilde **is** encoded, even though RFC 3986 considers it
// unreserved, because this format predates that RFC and changing it would break servers.
//
// It is pure computation: nothing is omitted.
public final class URLEncoder {

    private URLEncoder() {
    }

    /**
     * Encodes with UTF-8.
     *
     * @deprecated The result depends on the character set; use the overload that asks for it.
     */
    @Deprecated
    public static String encode(String s) {
        return encode(s, StandardCharsets.UTF_8);
    }

    /**
     * Encodes with the character set of that name.
     *
     * @throws UnsupportedEncodingException if the name matches none
     */
    public static String encode(String s, String enc) throws UnsupportedEncodingException {
        if (enc == null) {
            throw new NullPointerException("charsetName");
        }
        Charset cs;
        try {
            cs = Charset.forName(enc);
        } catch (Exception e) {
            throw new UnsupportedEncodingException(enc);
        }
        return encode(s, cs);
    }

    /** Encodes with that character set. */
    public static String encode(String s, Charset charset) {
        Objects.requireNonNull(charset, "charset");
        StringBuilder out = new StringBuilder(s.length());
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (dontNeedEncoding(c)) {
                out.append(c);
                i = i + 1;
            } else if (c == ' ') {
                out.append('+');
                i = i + 1;
            } else {
                // A surrogate pair is two `char`s forming a single character: they have to be
                // passed to the character set together or two invalid sequences come out instead of
                // one valid one.
                int end = i + 1;
                if (Character.isHighSurrogate(c) && end < s.length()
                        && Character.isLowSurrogate(s.charAt(end))) {
                    end = end + 1;
                }
                byte[] bytes = s.substring(i, end).getBytes(charset);
                int k = 0;
                while (k < bytes.length) {
                    out.append('%');
                    out.append(hex((bytes[k] >> 4) & 0xf));
                    out.append(hex(bytes[k] & 0xf));
                    k = k + 1;
                }
                i = end;
            }
        }
        return out.toString();
    }

    private static char hex(int v) {
        if (v < 10) {
            return (char) ('0' + v);
        }
        return (char) ('A' + (v - 10));
    }

    private static boolean dontNeedEncoding(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                || c == '-' || c == '_' || c == '.' || c == '*';
    }
}
