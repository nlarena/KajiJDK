package javax.print;

import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;

/**
 * KajiLibrary's javax.print.MimeType -- a normalised MIME type.
 *
 * <p>It is package-private: it exists only so that {@link DocFlavor} has something to compare
 * against. It is not part of the public API and must not be exposed.
 *
 * <h2>What it normalises, and what not</h2>
 *
 * <p>The point of the class is that {@code "Text/Plain; CharSet=Utf-8"} and {@code "text/plain;
 * charset=utf-8"} have to be equal. For that:
 *
 * <ul>
 *   <li>type and subtype are lowered;
 *   <li>the parameter <b>names</b> are lowered;
 *   <li>the parameters are sorted alphabetically by name;
 *   <li>the <b>values</b> are left as they are, with one exception: the {@code charset} one, which
 *       is lowered too.
 * </ul>
 *
 * <p>That exception is not a whim. A character set name is case-insensitive by definition, while
 * the value of an arbitrary parameter --{@code name="Final Report"}-- may not be, and lowering it
 * would destroy information.
 */
class MimeType implements Serializable, Cloneable {

    private static final long serialVersionUID = -2785720609362367683L;

    /** The input text, as it is. */
    private final String mimeType;

    /** Type, already in lower case. */
    private final String mediaType;

    /** Subtype, already in lower case. */
    private final String mediaSubtype;

    /** Parameters, sorted and normalised. */
    private final TreeMap<String, String> parameterMap;

    /** The canonical form, which is what is compared. */
    private final String canonical;

    /**
     * @throws NullPointerException if it is null
     * @throws IllegalArgumentException if it is not a valid MIME type
     */
    public MimeType(String s) {
        if (s == null) {
            throw new NullPointerException();
        }
        this.mimeType = s;
        this.parameterMap = new TreeMap<String, String>();
        Parser parser = new Parser(s);
        this.mediaType = parser.token().toLowerCase();
        parser.expect('/');
        this.mediaSubtype = parser.token().toLowerCase();
        while (parser.skipSemicolon()) {
            String name = parser.token().toLowerCase();
            parser.expect('=');
            String value = parser.value();
            if (name.equals("charset")) {
                value = value.toLowerCase();
            }
            this.parameterMap.put(name, value);
        }
        parser.expectEnd();
        this.canonical = build(this.mediaType, this.mediaSubtype, this.parameterMap);
    }

    /** The canonical form: type, subtype and sorted parameters. See the class note. */
    public String getMimeType() {
        return this.canonical;
    }

    /** The type, in lower case. */
    public String getMediaType() {
        return this.mediaType;
    }

    /** The subtype, in lower case. */
    public String getMediaSubtype() {
        return this.mediaSubtype;
    }

    /** The parameters, read-only. */
    public Map<String, String> getParameterMap() {
        return java.util.Collections.unmodifiableMap(this.parameterMap);
    }

    /** The canonical form. */
    @Override
    public String toString() {
        return this.canonical;
    }

    /** On the canonical form, not on the input text. */
    @Override
    public int hashCode() {
        return this.canonical.hashCode();
    }

    /** Likewise. Two types written differently but equivalent are equal. */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MimeType)) {
            return false;
        }
        return this.canonical.equals(((MimeType) obj).canonical);
    }

    /** The text as it was passed, not normalised. */
    String getOriginal() {
        return this.mimeType;
    }

    /** Builds the canonical form. */
    private static String build(String type, String subtype, TreeMap<String, String> params) {
        StringBuilder sb = new StringBuilder();
        sb.append(type).append('/').append(subtype);
        java.util.Iterator<Map.Entry<String, String>> it = params.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> e = it.next();
            sb.append("; ").append(e.getKey()).append("=\"").append(e.getValue()).append('"');
        }
        return sb.toString();
    }

    /**
     * RFC 2045's parser, reduced to what is needed.
     *
     * <p>A token is anything that is not a space, a control, or one of the separators; a value may
     * also come in quotes, and then it does accept spaces and escaping backslashes.
     */
    private static final class Parser {

        /** RFC 2045's separators; a token cannot contain any. */
        private static final String TSPECIALS = "()<>@,;:/[]?=\\\"";

        private final String text;

        private int pos;

        Parser(String text) {
            this.text = text;
            this.pos = 0;
        }

        /** Eats the spaces. */
        private void skipSpace() {
            while (this.pos < this.text.length() && this.text.charAt(this.pos) <= ' ') {
                this.pos = this.pos + 1;
            }
        }

        /** A non-empty token. */
        String token() {
            skipSpace();
            int start = this.pos;
            while (this.pos < this.text.length()) {
                char c = this.text.charAt(this.pos);
                if (c <= ' ' || c >= 127 || TSPECIALS.indexOf(c) >= 0) {
                    break;
                }
                this.pos = this.pos + 1;
            }
            if (this.pos == start) {
                throw new IllegalArgumentException();
            }
            return this.text.substring(start, this.pos);
        }

        /** A token or a quoted string. */
        String value() {
            skipSpace();
            if (this.pos < this.text.length() && this.text.charAt(this.pos) == '"') {
                this.pos = this.pos + 1;
                StringBuilder sb = new StringBuilder();
                while (true) {
                    if (this.pos >= this.text.length()) {
                        throw new IllegalArgumentException();
                    }
                    char c = this.text.charAt(this.pos);
                    this.pos = this.pos + 1;
                    if (c == '"') {
                        return sb.toString();
                    }
                    if (c == '\\') {
                        if (this.pos >= this.text.length()) {
                            throw new IllegalArgumentException();
                        }
                        c = this.text.charAt(this.pos);
                        this.pos = this.pos + 1;
                    }
                    sb.append(c);
                }
            }
            return token();
        }

        /** Consumes that character or fails. */
        void expect(char c) {
            skipSpace();
            if (this.pos >= this.text.length() || this.text.charAt(this.pos) != c) {
                throw new IllegalArgumentException();
            }
            this.pos = this.pos + 1;
        }

        /** Consumes a semicolon if there is one; says whether there was. */
        boolean skipSemicolon() {
            skipSpace();
            if (this.pos < this.text.length() && this.text.charAt(this.pos) == ';') {
                this.pos = this.pos + 1;
                return true;
            }
            return false;
        }

        /** Fails if something was left over. */
        void expectEnd() {
            skipSpace();
            if (this.pos < this.text.length()) {
                throw new IllegalArgumentException();
            }
        }
    }
}
