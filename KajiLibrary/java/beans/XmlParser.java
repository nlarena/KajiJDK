package java.beans;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// A tree of XML elements. It is not a JDK class and does not pretend to be one: over there the bean
// decoder rests on a system SAX parser, and in this tree `org.xml.sax` are interfaces with no
// implementation --there is no parser. This is the least that is needed to read what XMLEncoder
// writes, and it lives in `java.beans` because it is an internal detail of XMLDecoder.
//
// It is deliberately NOT a general-purpose XML parser: there is no DTD, no namespaces and no
// document-defined entities. It reads the dialect XMLEncoder produces plus whatever the bean
// format's specification allows to be written by hand. If something does not fit, it complains
// instead of guessing.
final class XmlNode {

    final String name;
    final Map<String, String> attributes = new HashMap<String, String>();
    final List<XmlNode> children = new ArrayList<XmlNode>();
    final StringBuilder text = new StringBuilder();

    // Text and children again, but IN ORDER and mixed together: each entry is a String or an
    // XmlNode. `text` gathers all the text and `children` all the elements, and those two views lose
    // where each thing was. For `<string>a<int>9</int>b</string>` the difference is between "a9b"
    // and "ab9", so the decoder reads through here.
    final List<Object> content = new ArrayList<Object>();

    XmlNode(String name) {
        this.name = name;
    }

    String attribute(String key) {
        return this.attributes.get(key);
    }
}

// A recursive-descent parser over the document's complete text.
//
// It works over a String and not over a Reader on purpose: an XMLDecoder's document is small --it is
// the description of a graph, not a stream-- and having it whole lets the errors say the exact
// position. The decoding from bytes to characters is done right here, in UTF-8, because
// `java.io.InputStreamReader` ignores the charset in this tree (see XMLEncoder's header).
final class XmlParser {

    private final String s;
    private int i;

    private XmlParser(String s) {
        this.s = s;
        this.i = 0;
    }

    // It reads the whole stream and returns the root element.
    static XmlNode parseDocument(InputStream input) throws IOException {
        return new XmlParser(readUtf8(input)).root();
    }

    // The same reading over a document already in memory. It is the way in for an
    // `org.xml.sax.InputSource` bringing a Reader: there the decoding was already done by someone
    // else.
    static XmlNode parseText(String document) {
        return new XmlParser(document).root();
    }

    static String readAll(java.io.Reader source) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[4096];
        int read = source.read(buf);
        while (read > 0) {
            sb.append(buf, 0, read);
            read = source.read(buf);
        }
        if (sb.length() > 0 && sb.charAt(0) == '﻿') {
            sb.deleteCharAt(0);
        }
        return sb.toString();
    }

    static String readUtf8(InputStream input) throws IOException {
        byte[] data = readAllBytes(input);
        StringBuilder sb = new StringBuilder(data.length);
        int n = data.length;
        int p = 0;
        while (p < n) {
            int b0 = data[p] & 0xFF;
            int cp;
            int len;
            if (b0 < 0x80) { cp = b0; len = 1; }
            else if ((b0 & 0xE0) == 0xC0) { cp = b0 & 0x1F; len = 2; }
            else if ((b0 & 0xF0) == 0xE0) { cp = b0 & 0x0F; len = 3; }
            else if ((b0 & 0xF8) == 0xF0) { cp = b0 & 0x07; len = 4; }
            else { cp = 0xFFFD; len = 1; }
            if (p + len > n) {
                cp = 0xFFFD;
                len = n - p;
            } else {
                for (int k = 1; k < len; k++) {
                    cp = (cp << 6) | (data[p + k] & 0x3F);
                }
            }
            p += len;
            if (cp > 0xFFFF) {
                cp -= 0x10000;
                sb.append((char) (0xD800 + (cp >> 10)));
                sb.append((char) (0xDC00 + (cp & 0x3FF)));
            } else {
                sb.append((char) cp);
            }
        }
        // The byte order mark, if one came, is no part of the document.
        if (sb.length() > 0 && sb.charAt(0) == '﻿') {
            sb.deleteCharAt(0);
        }
        return sb.toString();
    }

    private static byte[] readAllBytes(InputStream input) throws IOException {
        java.io.ByteArrayOutputStream acc = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int read = input.read(buf);
        while (read > 0) {
            acc.write(buf, 0, read);
            read = input.read(buf);
        }
        return acc.toByteArray();
    }

    private XmlNode root() {
        this.skipPreamble();
        XmlNode r = this.element();
        return r;
    }

    // The XML declaration, comments, processing instructions and DOCTYPE before the root.
    private void skipPreamble() {
        boolean goOn = true;
        while (goOn) {
            this.skipBlanks();
            if (this.looksAt("<?")) {
                this.skipPast("?>");
            } else if (this.looksAt("<!--")) {
                this.skipPast("-->");
            } else if (this.looksAt("<!")) {
                this.skipPast(">");
            } else {
                goOn = false;
            }
        }
    }

    private XmlNode element() {
        this.require("<");
        String name = this.xmlName();
        XmlNode node = new XmlNode(name);
        boolean empty = false;
        boolean selfClosed = false;
        while (!selfClosed) {
            this.skipBlanks();
            if (this.looksAt("/>")) {
                this.i += 2;
                empty = true;
                selfClosed = true;
            } else if (this.looksAt(">")) {
                this.i += 1;
                selfClosed = true;
            } else {
                String key = this.xmlName();
                this.skipBlanks();
                this.require("=");
                this.skipBlanks();
                node.attributes.put(key, this.quotedValue());
            }
        }
        if (!empty) {
            this.contents(node);
            this.require("</");
            String closing = this.xmlName();
            if (!closing.equals(name)) {
                throw this.error("</" + closing + "> closes what <" + name + "> opened");
            }
            this.skipBlanks();
            this.require(">");
        }
        return node;
    }

    private void contents(XmlNode node) {
        boolean goOn = true;
        while (goOn) {
            if (this.i >= this.s.length()) {
                throw this.error("the document ends inside <" + node.name + ">");
            }
            if (this.looksAt("</")) {
                goOn = false;
            } else if (this.looksAt("<!--")) {
                this.skipPast("-->");
            } else if (this.looksAt("<![CDATA[")) {
                int end = this.s.indexOf("]]>", this.i);
                if (end < 0) {
                    throw this.error("unclosed CDATA");
                }
                String cdata = this.s.substring(this.i + 9, end);
                node.text.append(cdata);
                node.content.add(cdata);
                this.i = end + 3;
            } else if (this.looksAt("<?")) {
                this.skipPast("?>");
            } else if (this.looksAt("<")) {
                XmlNode child = this.element();
                node.children.add(child);
                node.content.add(child);
            } else {
                String chunk = this.textUpToMark();
                node.text.append(chunk);
                node.content.add(chunk);
            }
        }
    }

    private String textUpToMark() {
        StringBuilder sb = new StringBuilder();
        while (this.i < this.s.length() && this.s.charAt(this.i) != '<') {
            char c = this.s.charAt(this.i);
            if (c == '&') {
                sb.append(this.entity());
            } else {
                sb.append(c);
                this.i++;
            }
        }
        return sb.toString();
    }

    // The five entities XML defines out of the box, plus the numeric references. There are no
    // document entities: with no DTD there is nowhere to declare them.
    private String entity() {
        int end = this.s.indexOf(';', this.i);
        if (end < 0) {
            throw this.error("entity with no `;`");
        }
        String body = this.s.substring(this.i + 1, end);
        this.i = end + 1;
        String r;
        if (body.equals("amp")) { r = "&"; }
        else if (body.equals("lt")) { r = "<"; }
        else if (body.equals("gt")) { r = ">"; }
        else if (body.equals("quot")) { r = "\""; }
        else if (body.equals("apos")) { r = "'"; }
        else if (body.length() > 1 && body.charAt(0) == '#') {
            int cp;
            if (body.charAt(1) == 'x' || body.charAt(1) == 'X') {
                cp = Integer.parseInt(body.substring(2), 16);
            } else {
                cp = Integer.parseInt(body.substring(1));
            }
            if (cp > 0xFFFF) {
                cp -= 0x10000;
                r = new String(new char[] { (char) (0xD800 + (cp >> 10)), (char) (0xDC00 + (cp & 0x3FF)) });
            } else {
                r = String.valueOf((char) cp);
            }
        } else {
            throw this.error("unknown entity &" + body + ";");
        }
        return r;
    }

    private String quotedValue() {
        char quote = this.peek();
        if (quote != '"' && quote != '\'') {
            throw this.error("an attribute's value goes between quotes");
        }
        this.i++;
        StringBuilder sb = new StringBuilder();
        while (this.i < this.s.length() && this.s.charAt(this.i) != quote) {
            char c = this.s.charAt(this.i);
            if (c == '&') {
                sb.append(this.entity());
            } else {
                sb.append(c);
                this.i++;
            }
        }
        if (this.i >= this.s.length()) {
            throw this.error("unclosed attribute");
        }
        this.i++;
        return sb.toString();
    }

    private String xmlName() {
        int start = this.i;
        while (this.i < this.s.length() && isNameChar(this.s.charAt(this.i))) {
            this.i++;
        }
        if (start == this.i) {
            throw this.error("a name was expected");
        }
        return this.s.substring(start, this.i);
    }

    private static boolean isNameChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
            || c == '_' || c == '-' || c == '.' || c == ':' || c > 127;
    }

    private void skipBlanks() {
        while (this.i < this.s.length() && isBlank(this.s.charAt(this.i))) {
            this.i++;
        }
    }

    private static boolean isBlank(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }

    private char peek() {
        if (this.i >= this.s.length()) {
            throw this.error("the document ends before its time");
        }
        return this.s.charAt(this.i);
    }

    private boolean looksAt(String t) {
        return this.s.startsWith(t, this.i);
    }

    private void require(String t) {
        if (!this.looksAt(t)) {
            throw this.error("se expected `" + t + "`");
        }
        this.i += t.length();
    }

    private void skipPast(String t) {
        int end = this.s.indexOf(t, this.i);
        if (end < 0) {
            throw this.error("falta `" + t + "`");
        }
        this.i = end + t.length();
    }

    private RuntimeException error(String complaint) {
        int line = 1;
        for (int k = 0; k < this.i && k < this.s.length(); k++) {
            if (this.s.charAt(k) == '\n') {
                line++;
            }
        }
        return new IllegalArgumentException("XML line " + line + ": " + complaint);
    }
}
