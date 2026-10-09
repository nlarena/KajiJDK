package java.util.prefs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;

// The interchange format: what `exportNode`/`exportSubtree` write and what `importPreferences`
// reads.
//
// WHY THERE IS AN XML PARSER IN HERE. Writing XML is concatenating strings; reading it is not. This
// tree has no `org.w3c.dom`, no `org.xml.sax` and no `javax.xml.parsers` --the only XML thing that
// exists is `javax.xml.transform`, which is interfaces with no XSLT implementation-- so
// `importPreferences` either brought its own parser or stayed out.
//
// It brings its own, and it can because **the preferences DTD has no text content**:
//
//     <!ELEMENT preferences (root)>      <!ATTLIST preferences EXTERNAL_XML_VERSION CDATA "0.0">
//     <!ELEMENT root (map, node*)>       <!ATTLIST root type (system|user) #REQUIRED>
//     <!ELEMENT node (map, node*)>       <!ATTLIST node name CDATA #REQUIRED>
//     <!ELEMENT map (entry*)>
//     <!ELEMENT entry EMPTY>             <!ATTLIST entry key CDATA #REQUIRED value CDATA #REQUIRED>
//
// Every element is of *element only* content or empty, there are no entity declarations of its own
// and there are no namespaces. That leaves the grammar to be covered at: declaration, DOCTYPE,
// comments, processing instructions, tags with attributes, the five predefined entities and the
// numeric references. It is a closed list, and that is why the parser below is **complete** for this
// DTD and not an approximation that works with the files we write ourselves. Anything falling
// outside --loose text where there can be none, an unclosed tag, an unknown entity-- comes out as
// `InvalidPreferencesFormatException`, which is exactly what the contract asks for.
//
// THE ONLY THING THAT CANNOT BE EXPORTED is values with C0 control characters other than tab, line
// feed and carriage return: XML 1.0 admits them **not even** as a numeric reference, so there is no
// valid document that contains them. The reference implementation writes them anyway and produces a
// file nobody can read afterwards; here `IllegalArgumentException` is thrown on export, because a
// file that cannot be imported back is worse than an exception.
//
// The other three are written, but **as a numeric reference** and not raw: a literal tab or line
// feed inside an attribute is normalised to a space by any parser that follows the standard, and the
// value would come back different from how it went out.
final class Xml {

    private static final String VERSION = "1.0";
    private static final String DTD = "http://java.sun.com/dtd/preferences.dtd";

    private Xml() {
    }

    // ---- write ---------------------------------------------------------------------------

    static void export(OutputStream os, Preferences p, boolean subtree)
            throws IOException, BackingStoreException {
        if (((AbstractPreferences) p).isRemoved()) {
            throw new IllegalStateException("Node has been removed");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
        sb.append("<!DOCTYPE preferences SYSTEM \"").append(DTD).append("\">\n");
        sb.append("<preferences EXTERNAL_XML_VERSION=\"").append(VERSION).append("\">\n");
        sb.append("  <root type=\"").append(p.isUserNode() ? "user" : "system").append("\">\n");

        // The chain of ancestors from the root down to `p` is written whole, with each one's empty
        // `<map/>`. It is what makes the document self-sufficient: whoever imports it recreates the
        // full path without having to know where it came from.
        ArrayList<Preferences> ancestors = new ArrayList<Preferences>();
        // A `while` and not the two-variable `for` the case asked for: our javac does not keep in
        // scope the variables of a `for` with several declarators (see
        // scratchpad/zzprefs/ForVariosDeclaradores.java).
        Preferences climbing = p;
        while (climbing.parent() != null) {
            ancestors.add(climbing);
            climbing = climbing.parent();
        }
        int indentation = 2;
        for (int i = ancestors.size() - 1; i >= 0; i--) {
            indent(sb, indentation).append("<map/>\n");
            indent(sb, indentation).append("<node name=\"");
            writeEscaped(sb, ancestors.get(i).name());
            sb.append("\">\n");
            indentation++;
        }
        dump(sb, p, subtree, indentation);
        while (indentation > 2) {
            indentation--;
            indent(sb, indentation).append("</node>\n");
        }
        sb.append("  </root>\n");
        sb.append("</preferences>\n");
        os.write(sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        os.flush();
    }

    private static void dump(StringBuilder sb, Preferences p, boolean subtree, int indentation)
            throws BackingStoreException {
        String[] keyNames;
        String[] childNames = null;
        Preferences[] children = null;
        // The keys and the list of children are taken under the node's lock so the document is a
        // snapshot and not a mix of two moments; the recursive walk is done afterwards, outside.
        synchronized (((AbstractPreferences) p).lock) {
            if (((AbstractPreferences) p).isRemoved()) {
                return;
            }
            keyNames = p.keys();
            if (subtree) {
                childNames = p.childrenNames();
                children = new Preferences[childNames.length];
                for (int i = 0; i < childNames.length; i++) {
                    children[i] = p.node(childNames[i]);
                }
            }
        }
        if (keyNames.length == 0) {
            indent(sb, indentation).append("<map/>\n");
        } else {
            indent(sb, indentation).append("<map>\n");
            for (int i = 0; i < keyNames.length; i++) {
                indent(sb, indentation + 1).append("<entry key=\"");
                writeEscaped(sb, keyNames[i]);
                sb.append("\" value=\"");
                writeEscaped(sb, p.get(keyNames[i], ""));
                sb.append("\"/>\n");
            }
            indent(sb, indentation).append("</map>\n");
        }
        if (subtree) {
            for (int i = 0; i < childNames.length; i++) {
                indent(sb, indentation).append("<node name=\"");
                writeEscaped(sb, childNames[i]);
                sb.append("\">\n");
                dump(sb, children[i], true, indentation + 1);
                indent(sb, indentation).append("</node>\n");
            }
        }
    }

    private static StringBuilder indent(StringBuilder sb, int n) {
        for (int i = 0; i < n; i++) {
            sb.append("  ");
        }
        return sb;
    }

    private static void writeEscaped(StringBuilder sb, String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '&') {
                sb.append("&amp;");
            } else if (c == '<') {
                sb.append("&lt;");
            } else if (c == '>') {
                sb.append("&gt;");
            } else if (c == '"') {
                sb.append("&quot;");
            } else if (c == '\'') {
                sb.append("&apos;");
            } else if (c == '\t' || c == '\n' || c == '\r') {
                sb.append("&#").append((int) c).append(';');
            } else if (c < 0x20) {
                throw new IllegalArgumentException(
                        "XML 1.0 does not admit the control character U+"
                                + Integer.toHexString(c) + ", not even escaped");
            } else {
                sb.append(c);
            }
        }
    }

    // ---- read -------------------------------------------------------------------------------

    static void doImport(InputStream is) throws IOException, InvalidPreferencesFormatException {
        String text = decodeText(readAll(is));
        Elem doc;
        try {
            doc = new Parser(text).document();
        } catch (InvalidPreferencesFormatException e) {
            throw e;
        } catch (RuntimeException e) {
            // An index out of range from the parser is a broken document, not an error of ours the
            // program should have to tell apart.
            throw new InvalidPreferencesFormatException(e);
        }
        if (!doc.tag.equals("preferences")) {
            throw new InvalidPreferencesFormatException(
                    "the root element is <" + doc.tag + "> and not <preferences>");
        }
        String v = doc.attr("EXTERNAL_XML_VERSION");
        if (v.length() != 0 && v.compareTo(VERSION) > 0) {
            throw new InvalidPreferencesFormatException(
                    "Exported preferences file format version " + v
                            + " is not supported. This java installation can read versions "
                            + VERSION + " or older.");
        }
        if (doc.children.size() != 1 || !doc.children.get(0).tag.equals("root")) {
            throw new InvalidPreferencesFormatException("<preferences> must have exactly one <root>");
        }
        Elem root = doc.children.get(0);
        String type = root.attr("type");
        Preferences target;
        if (type.equals("user")) {
            target = Preferences.userRoot();
        } else if (type.equals("system")) {
            target = Preferences.systemRoot();
        } else {
            throw new InvalidPreferencesFormatException(
                    "<root type> is \"" + type + "\" and it has to be \"user\" or \"system\"");
        }
        importSubtree(target, root);
    }

    private static void importSubtree(Preferences target, Elem xml)
            throws InvalidPreferencesFormatException {
        if (xml.children.isEmpty() || !xml.children.get(0).tag.equals("map")) {
            throw new InvalidPreferencesFormatException(
                    "<" + xml.tag + "> has to start with <map>");
        }
        Elem map = xml.children.get(0);
        for (int i = 0; i < map.children.size(); i++) {
            Elem e = map.children.get(i);
            if (!e.tag.equals("entry")) {
                throw new InvalidPreferencesFormatException(
                        "<map> admits only <entry>, not <" + e.tag + ">");
            }
            if (!e.attrs.containsKey("key") || !e.attrs.containsKey("value")) {
                throw new InvalidPreferencesFormatException("<entry> needs `key` and `value`");
            }
            target.put(e.attr("key"), e.attr("value"));
        }
        for (int i = 1; i < xml.children.size(); i++) {
            Elem e = xml.children.get(i);
            if (!e.tag.equals("node")) {
                throw new InvalidPreferencesFormatException(
                        "after the <map> only <node> goes, not <" + e.tag + ">");
            }
            if (!e.attrs.containsKey("name")) {
                throw new InvalidPreferencesFormatException("<node> needs `name`");
            }
            importSubtree(target.node(e.attr("name")), e);
        }
    }

    private static byte[] readAll(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) > 0) {
            bos.write(buf, 0, n);
        }
        return bos.toByteArray();
    }

    // The document says which encoding it is in, but reading that declaration already requires
    // decoding it. The way out of the circle is that every encoding XML admits is either
    // ASCII-compatible --and then the declaration reads correctly decoding as UTF-8-- or UTF-16,
    // which is recognised by the byte order mark.
    private static String decodeText(byte[] b) {
        if (b.length >= 2 && (b[0] & 0xff) == 0xfe && (b[1] & 0xff) == 0xff) {
            return new String(b, java.nio.charset.StandardCharsets.UTF_16BE).substring(1);
        }
        if (b.length >= 2 && (b[0] & 0xff) == 0xff && (b[1] & 0xff) == 0xfe) {
            return new String(b, java.nio.charset.StandardCharsets.UTF_16LE).substring(1);
        }
        String s = new String(b, java.nio.charset.StandardCharsets.UTF_8);
        if (s.length() > 0 && s.charAt(0) == '﻿') {
            s = s.substring(1);
        }
        String enc = declaredEncoding(s);
        if (enc != null && !enc.equalsIgnoreCase("UTF-8") && !enc.equalsIgnoreCase("UTF8")) {
            try {
                return new String(b, enc);
            } catch (Exception e) {
                // An encoding the VM does not know: it carries on with UTF-8 and the parser will
                // say the document is not understood, which is the truth.
            }
        }
        return s;
    }

    private static String declaredEncoding(String s) {
        if (!s.startsWith("<?xml")) {
            return null;
        }
        int end = s.indexOf("?>");
        if (end < 0) {
            return null;
        }
        String decl = s.substring(0, end);
        int i = decl.indexOf("encoding");
        if (i < 0) {
            return null;
        }
        int quote = -1;
        for (int j = i + 8; j < decl.length(); j++) {
            char c = decl.charAt(j);
            if (c == '"' || c == '\'') {
                quote = j;
                break;
            }
        }
        if (quote < 0) {
            return null;
        }
        int closing = decl.indexOf(decl.charAt(quote), quote + 1);
        return closing < 0 ? null : decl.substring(quote + 1, closing);
    }

    // ---- the tree the parser produces    ----------------------------------------------------

    private static final class Elem {
        final String tag;
        final HashMap<String, String> attrs = new HashMap<String, String>();
        final ArrayList<Elem> children = new ArrayList<Elem>();

        Elem(String tag) {
            this.tag = tag;
        }

        String attr(String n) {
            String v = attrs.get(n);
            return v == null ? "" : v;
        }
    }

    // ---- the parser    -----------------------------------------------------------------------

    private static final class Parser {

        private final String s;
        private int i;

        Parser(String s) {
            this.s = s;
        }

        Elem document() throws InvalidPreferencesFormatException {
            prolog();
            Elem root = element();
            prolog(); // comments and whitespace after the root element
            if (i < s.length()) {
                error("there is text left over after the root element");
            }
            return root;
        }

        // Everything there can be around the root element: whitespace, the XML declaration, the
        // DOCTYPE, comments and processing instructions.
        private void prolog() throws InvalidPreferencesFormatException {
            while (true) {
                skipWhitespace();
                if (i + 1 >= s.length() || s.charAt(i) != '<') {
                    return;
                }
                char c = s.charAt(i + 1);
                if (c == '?') {
                    skipTo("?>");
                } else if (c == '!') {
                    if (s.startsWith("<!--", i)) {
                        skipTo("-->");
                    } else if (s.startsWith("<!DOCTYPE", i)) {
                        doctype();
                    } else {
                        error("`<!` was not expected here");
                    }
                } else {
                    return;
                }
            }
        }

        // The DOCTYPE is skipped whole: the parser does not validate against the DTD --that is
        // `importSubtree`'s job, which demands the exact shape the DTD describes-- but it **does**
        // have to know where it ends, and that is not "the next `>`": the internal subset between
        // brackets may hold as many as it likes.
        private void doctype() throws InvalidPreferencesFormatException {
            i += "<!DOCTYPE".length();
            int brackets = 0;
            while (i < s.length()) {
                char c = s.charAt(i);
                if (c == '"' || c == '\'') {
                    int end = s.indexOf(c, i + 1);
                    if (end < 0) {
                        error("unclosed quote in the DOCTYPE");
                    }
                    i = end + 1;
                    continue;
                }
                if (c == '[') {
                    brackets++;
                } else if (c == ']') {
                    brackets--;
                } else if (c == '>' && brackets == 0) {
                    i++;
                    return;
                }
                i++;
            }
            error("unclosed DOCTYPE");
        }

        private Elem element() throws InvalidPreferencesFormatException {
            expect('<');
            String tag = name();
            Elem e = new Elem(tag);
            while (true) {
                skipWhitespace();
                if (i >= s.length()) {
                    error("tag <" + tag + "> is not closed");
                }
                char c = s.charAt(i);
                if (c == '>') {
                    i++;
                    break;
                }
                if (c == '/') {
                    i++;
                    expect('>');
                    return e; // <tag/>
                }
                String n = name();
                skipWhitespace();
                expect('=');
                skipWhitespace();
                if (e.attrs.put(n, attributeValue()) != null) {
                    error("the attribute `" + n + "` is there twice in <" + tag + ">");
                }
            }
            content(e);
            return e;
        }

        private void content(Elem e) throws InvalidPreferencesFormatException {
            while (true) {
                if (i >= s.length()) {
                    error("missing </" + e.tag + ">");
                }
                char c = s.charAt(i);
                if (c == '<') {
                    if (s.startsWith("<!--", i)) {
                        skipTo("-->");
                    } else if (s.startsWith("<![CDATA[", i)) {
                        int end = s.indexOf("]]>", i);
                        if (end < 0) {
                            error("unclosed CDATA");
                        }
                        requireWhitespace(s.substring(i + 9, end), e.tag);
                        i = end + 3;
                    } else if (s.startsWith("<?", i)) {
                        skipTo("?>");
                    } else if (s.startsWith("</", i)) {
                        i += 2;
                        String closing = name();
                        if (!closing.equals(e.tag)) {
                            error("<" + e.tag + "> was opened and </" + closing + "> closed");
                        }
                        skipWhitespace();
                        expect('>');
                        return;
                    } else {
                        e.children.add(element());
                    }
                } else if (c == '&') {
                    // A reference in the content has to resolve to whitespace: the DTD admits text
                    // in no element.
                    StringBuilder sb = new StringBuilder();
                    reference(sb);
                    requireWhitespace(sb.toString(), e.tag);
                } else if (isWhitespace(c)) {
                    i++;
                } else {
                    error("<" + e.tag + "> admits no text, and there is `" + c + "`");
                }
            }
        }

        private void requireWhitespace(String t, String tag) throws InvalidPreferencesFormatException {
            for (int k = 0; k < t.length(); k++) {
                if (!isWhitespace(t.charAt(k))) {
                    error("<" + tag + "> admits no text, and there is `" + t.trim() + "`");
                }
            }
        }

        private String attributeValue() throws InvalidPreferencesFormatException {
            if (i >= s.length()) {
                error("the attribute value is missing");
            }
            char quote = s.charAt(i);
            if (quote != '"' && quote != '\'') {
                error("an attribute value goes between quotes");
            }
            i++;
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (i >= s.length()) {
                    error("unclosed attribute value");
                }
                char c = s.charAt(i);
                if (c == quote) {
                    i++;
                    return sb.toString();
                }
                if (c == '<') {
                    error("a raw `<` cannot be inside an attribute value");
                }
                if (c == '&') {
                    reference(sb);
                } else if (c == '\t' || c == '\n' || c == '\r') {
                    // Attribute value normalisation, exactly as XML 1.0 asks for it. That is why
                    // the exporter writes these three as a numeric reference: raw they would come
                    // back turned into a space.
                    sb.append(' ');
                    i++;
                } else {
                    sb.append(c);
                    i++;
                }
            }
        }

        private void reference(StringBuilder sb) throws InvalidPreferencesFormatException {
            int end = s.indexOf(';', i);
            if (end < 0) {
                error("reference with no `;`");
            }
            String r = s.substring(i + 1, end);
            i = end + 1;
            if (r.length() == 0) {
                error("empty reference");
            }
            if (r.charAt(0) == '#') {
                int cp;
                try {
                    cp = (r.length() > 1 && (r.charAt(1) == 'x' || r.charAt(1) == 'X'))
                            ? Integer.parseInt(r.substring(2), 16)
                            : Integer.parseInt(r.substring(1));
                } catch (NumberFormatException e) {
                    error("invalid numeric reference `&" + r + ";`");
                    return;
                }
                if (cp < 0 || cp > 0x10ffff) {
                    error("numeric reference out of range `&" + r + ";`");
                }
                sb.appendCodePoint(cp);
                return;
            }
            if (r.equals("lt")) {
                sb.append('<');
            } else if (r.equals("gt")) {
                sb.append('>');
            } else if (r.equals("amp")) {
                sb.append('&');
            } else if (r.equals("quot")) {
                sb.append('"');
            } else if (r.equals("apos")) {
                sb.append('\'');
            } else {
                // With no entity declarations there is no way of knowing what it stands for:
                // inventing a value would be worse than saying it is not understood.
                error("unknown entity `&" + r + ";`");
            }
        }

        private String name() throws InvalidPreferencesFormatException {
            int start = i;
            while (i < s.length() && isNameChar(s.charAt(i))) {
                i++;
            }
            if (i == start) {
                error("a name was expected");
            }
            return s.substring(start, i);
        }

        private static boolean isNameChar(char c) {
            return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '_' || c == '-' || c == '.' || c == ':' || c > 0x7f;
        }

        private static boolean isWhitespace(char c) {
            return c == ' ' || c == '\t' || c == '\n' || c == '\r';
        }

        private void skipWhitespace() {
            while (i < s.length() && isWhitespace(s.charAt(i))) {
                i++;
            }
        }

        private void expect(char c) throws InvalidPreferencesFormatException {
            if (i >= s.length() || s.charAt(i) != c) {
                error("expected `" + c + "`");
            }
            i++;
        }

        private void skipTo(String closing) throws InvalidPreferencesFormatException {
            int end = s.indexOf(closing, i);
            if (end < 0) {
                error("missing `" + closing + "`");
            }
            i = end + closing.length();
        }

        private void error(String m) throws InvalidPreferencesFormatException {
            throw new InvalidPreferencesFormatException(m + " (position " + i + ")");
        }
    }
}
