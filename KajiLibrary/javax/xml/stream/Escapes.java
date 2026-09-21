package javax.xml.stream;

import java.io.IOException;
import java.io.Writer;

/**
 * Escaping of text, in a single place.
 *
 * <p>It is used by the cursor writer and by each event's {@code writeAsEncodedUnicode}, which have
 * to produce exactly the same.
 *
 * <p>The rules are not the same inside and outside an attribute, and that is why there are two
 * methods. In content {@code &} and {@code <} have to be escaped --and {@code >} only because of
 * the sequence {@code ]]>}, although it is always escaped, which is what everybody does and is
 * simpler than detecting it--. In an attribute value the quotes it is delimited with also have to
 * be escaped, and tabs and line breaks: without that, attribute value normalization would turn them
 * into spaces when reading back, that is, the document would not say the same.
 */
final class Escapes {

    private Escapes() {
    }

    /** Escapes content text. */
    static void content(Writer w, String s) throws IOException {
        int n = s.length();
        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            if (c == '&') {
                w.write("&amp;");
            } else if (c == '<') {
                w.write("&lt;");
            } else if (c == '>') {
                w.write("&gt;");
            } else {
                w.write(c);
            }
        }
    }

    /** Escapes content text, from an array. */
    static void content(Writer w, char[] b, int from, int len) throws IOException {
        int end = from + len;
        for (int i = from; i < end; i++) {
            char c = b[i];
            if (c == '&') {
                w.write("&amp;");
            } else if (c == '<') {
                w.write("&lt;");
            } else if (c == '>') {
                w.write("&gt;");
            } else {
                w.write(c);
            }
        }
    }

    /** Escapes an attribute value, which goes in double quotes. */
    static void attribute(Writer w, String s) throws IOException {
        int n = s.length();
        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            if (c == '&') {
                w.write("&amp;");
            } else if (c == '<') {
                w.write("&lt;");
            } else if (c == '>') {
                w.write("&gt;");
            } else if (c == '"') {
                w.write("&quot;");
            } else if (c == '\t') {
                w.write("&#9;");
            } else if (c == '\n') {
                w.write("&#10;");
            } else if (c == '\r') {
                w.write("&#13;");
            } else {
                w.write(c);
            }
        }
    }
}
