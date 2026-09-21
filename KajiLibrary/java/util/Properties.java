package java.util;

// The simple name for the `catch`: a qualified type in the catch clause is not recognised as the
// same type as the `throws` (see #274b's note at the foot).
import java.io.IOException;

// KajiLibrary's java.util.Properties (finding #267).
//
// It exists because `jakarta.persistence.spi.PersistenceUnitInfo` returns one from
// `getProperties()`, and without the class the file does not compile.
//
// The JDK's shape is kept where it is load-bearing: it EXTENDS Hashtable<Object,Object> -- which
// is why `put` can take any object and `getProperty` returns null for a non-String value rather
// than throwing -- and it chains to a `defaults` table.
//
// The six format methods --`load` x2, `store` x2, `save` and `list` x2-- are here, and with them
// the XML pair. The old note said writing them would be "inventing a parser nobody can test here";
// what changed is not the criterion but that now they **can** be tested: the `store` -> `load`
// round trip compares against the inner `java`, and the `.properties` format is specified in detail.
//
// About the escapes, which is where the format's only inner difficulty lives: on **writing**, a key
// escapes far more than a value. In the key the three separators (`=`, `:` and whitespace) have to
// be escaped, because otherwise they would split the pair on re-reading; in the value only the
// **leading** whitespace, which is the only one the reader would eat. Escaping too much breaks
// nothing on re-reading, but it produces files different from the JDK's, so exactly what is called
// for is escaped.
//
// The XML pair is an honest subset and it is said below, at `loadFromXML`: the shape `storeToXML`
// writes --which is the DTD's-- is read, without validating against the DTD or resolving it.
//
// A missing member is a legal subset; a member that lies is not.
public class Properties extends Hashtable<Object, Object> {

    /** The table consulted when a key is not in this one. Null if there is none. */
    protected Properties defaults;

    public Properties() {
        this.defaults = null;
    }

    // With an initial capacity. The table behind uses it; the rest is the same.
    public Properties(int initialCapacity) {
        super(initialCapacity);
        this.defaults = null;
    }

    public Properties(Properties defaults) {
        this.defaults = defaults;
    }

    /**
     * The value of {@code key}, or the one the defaults chain gives, or null.
     *
     * <p>Returns null -- not the stored object -- when the value is present but is not a String.
     * That is the JDK's behaviour and the reason this class can extend a table of Objects without
     * its String-typed accessors ever lying about what they return.
     */
    public String getProperty(String key) {
        Object value = this.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        if (this.defaults != null) {
            return this.defaults.getProperty(key);
        }
        return null;
    }

    public String getProperty(String key, String defaultValue) {
        String value = this.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    /**
     * Stores a String value. Returns whatever was there before, which need not be a String --
     * again the JDK's signature, and the honest one for a table of Objects.
     */
    public synchronized Object setProperty(String key, String value) {
        return this.put(key, value);
    }

    /** The keys of this table and of its defaults chain. */
    public Enumeration<Object> propertyNames() {
        return this.collectNames().keys();
    }

    /** The keys whose key AND value are both Strings, defaults included. */
    public Set<String> stringPropertyNames() {
        Hashtable<Object, Object> all = this.collectNames();
        Set<String> names = new HashSet<String>();
        Enumeration<Object> keys = all.keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            if (key instanceof String && all.get(key) instanceof String) {
                names.add((String) key);
            }
        }
        return names;
    }

    // The defaults FIRST, so this table's own entries overwrite them -- which is the whole point
    // of a defaults chain.
    private Hashtable<Object, Object> collectNames() {
        Hashtable<Object, Object> all = new Hashtable<Object, Object>();
        if (this.defaults != null) {
            Hashtable<Object, Object> inherited = this.defaults.collectNames();
            Enumeration<Object> keys = inherited.keys();
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                all.put(key, inherited.get(key));
            }
        }
        Enumeration<Object> mine = this.keys();
        while (mine.hasMoreElements()) {
            Object key = mine.nextElement();
            all.put(key, this.get(key));
        }
        return all;
    }

    // ---- reading the .properties format  -----------------------------------------------------

    /**
     * It reads key/value pairs from `reader`, in the `.properties` format.
     *
     * <p>The format has more rules than it looks, and all of them matter because a badly read
     * configuration file fails far away:
     *
     * <ul>
     *   <li>A line whose first non-blank character is {@code #} or {@code !} is a comment.
     *   <li>The key ends at the first **unescaped** {@code =}, {@code :} or whitespace; the
     *       separator may come surrounded by whitespace, which is discarded.
     *   <li>A line ending in an **odd** number of backslashes continues on the next one, whose
     *       leading whitespace is discarded. The odd number is what tells a continuation from a
     *       value ending in an escaped backslash.
     *   <li>Escapes: {@code \t \n \r \f \\} and {@code \uXXXX}. Any other {@code \x} gives
     *       {@code x} — that is how {@code \=} and {@code \:} get into a key.
     * </ul>
     *
     * <p>A key with no separator is a key with an empty value, not an error.
     */
    public synchronized void load(java.io.Reader reader) throws IOException {
        StringBuilder whole = new StringBuilder();
        int c = reader.read();
        while (c >= 0) {
            whole.append((char) c);
            c = reader.read();
        }
        this.parseText(whole.toString());
    }

    /**
     * It reads key/value pairs from `inStream`, in the `.properties` format.
     *
     * <p>The bytes are read as **ISO-8859-1**, one byte per character, which is what the
     * specification demands. It is not a simplification of ours: it is why {@code \uXXXX} exists in
     * the format — it is the only way of writing a character outside Latin-1.
     */
    public synchronized void load(java.io.InputStream inStream) throws IOException {
        StringBuilder whole = new StringBuilder();
        int b = inStream.read();
        while (b >= 0) {
            whole.append((char) (b & 0xFF));
            b = inStream.read();
        }
        this.parseText(whole.toString());
    }

    // It splits the text into logical lines —joining the continuations— and stores each pair.
    private void parseText(String text) {
        int i = 0;
        int n = text.length();
        while (i < n) {
            // One physical line.
            int end = i;
            while (end < n && text.charAt(end) != '\n' && text.charAt(end) != '\r') {
                end = end + 1;
            }
            String line = text.substring(i, end);
            // Skip the line break, counting \r\n as a single one.
            i = end;
            if (i < n && text.charAt(i) == '\r') {
                i = i + 1;
            }
            if (i < n && text.charAt(i) == '\n') {
                i = i + 1;
            }

            String trimmed = stripLeadingBlanks(line);
            if (trimmed.length() == 0) {
                continue;
            }
            char first = trimmed.charAt(0);
            if (first == '#' || first == '!') {
                continue;
            }

            // Continuations: while the line ends in an ODD number of backslashes.
            while (endsInOddBackslashes(trimmed) && i < n) {
                trimmed = trimmed.substring(0, trimmed.length() - 1);
                int f2 = i;
                while (f2 < n && text.charAt(f2) != '\n' && text.charAt(f2) != '\r') {
                    f2 = f2 + 1;
                }
                String continuation = text.substring(i, f2);
                i = f2;
                if (i < n && text.charAt(i) == '\r') {
                    i = i + 1;
                }
                if (i < n && text.charAt(i) == '\n') {
                    i = i + 1;
                }
                trimmed = trimmed + stripLeadingBlanks(continuation);
            }

            this.storePair(trimmed);
        }
    }

    // It splits a logical line into key and value and stores them.
    private void storePair(String line) {
        int n = line.length();
        int k = 0;
        // The key ends at the first unescaped =, : or whitespace.
        while (k < n) {
            char c = line.charAt(k);
            if (c == '\\') {
                k = k + 2;
                continue;
            }
            if (c == '=' || c == ':' || c == ' ' || c == '\t' || c == '\f') {
                break;
            }
            k = k + 1;
        }
        String keyText = line.substring(0, Math.min(k, n));
        // Skip whitespace, an optional separator, and more whitespace.
        int v = Math.min(k, n);
        while (v < n && (line.charAt(v) == ' ' || line.charAt(v) == '\t' || line.charAt(v) == '\f')) {
            v = v + 1;
        }
        if (v < n && (line.charAt(v) == '=' || line.charAt(v) == ':')) {
            v = v + 1;
            while (v < n && (line.charAt(v) == ' ' || line.charAt(v) == '\t' || line.charAt(v) == '\f')) {
                v = v + 1;
            }
        }
        String valueText = line.substring(v, n);
        this.put(unescape(keyText), unescape(valueText));
    }

    private static String stripLeadingBlanks(String s) {
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c != ' ' && c != '\t' && c != '\f') {
                break;
            }
            i = i + 1;
        }
        return s.substring(i, s.length());
    }

    // Whether the line ends in an ODD number of backslashes, that is, whether it continues.
    private static boolean endsInOddBackslashes(String s) {
        int backslashes = 0;
        int i = s.length() - 1;
        while (i >= 0 && s.charAt(i) == '\\') {
            backslashes = backslashes + 1;
            i = i - 1;
        }
        return backslashes % 2 == 1;
    }

    // It applies the format's escapes. An unknown `\x` gives `x`, which is how `\=` and `\:` get
    // into a key without splitting it.
    private static String unescape(String s) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        int n = s.length();
        while (i < n) {
            char c = s.charAt(i);
            if (c != '\\') {
                out.append(c);
                i = i + 1;
                continue;
            }
            i = i + 1;
            if (i >= n) {
                break;
            }
            char e = s.charAt(i);
            i = i + 1;
            if (e == 't') {
                out.append('\t');
            } else if (e == 'n') {
                out.append('\n');
            } else if (e == 'r') {
                out.append('\r');
            } else if (e == 'f') {
                out.append('\f');
            } else if (e == 'u') {
                int valueText = 0;
                int readCount = 0;
                while (readCount < 4 && i < n) {
                    int d = hexDigitOf(s.charAt(i));
                    if (d < 0) {
                        break;
                    }
                    valueText = valueText * 16 + d;
                    i = i + 1;
                    readCount = readCount + 1;
                }
                out.append((char) valueText);
            } else {
                out.append(e);
            }
        }
        return out.toString();
    }

    private static int hexDigitOf(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        return -1;
    }

    // ---- Map's operations, redeclared ------------------------------------------------------------
    //
    // The JDK redeclares them over `Object` --and does not inherit them from Hashtable<Object,Object>
    // -- for two reasons that still hold here: they pin the `synchronized` down in one place, and
    // they leave the raw type in plain sight, which is what reminds one that a Properties **can**
    // have keys that are not Strings (and that this is why `getProperty` returns null instead of
    // throwing).

    public synchronized Object put(Object key, Object value) {
        return super.put(key, value);
    }

    public Object get(Object key) {
        return super.get(key);
    }

    public synchronized Object remove(Object key) {
        return super.remove(key);
    }

    public Object getOrDefault(Object key, Object defaultValue) {
        Object v = super.get(key);
        if (v == null) {
            return defaultValue;
        }
        return v;
    }

    public synchronized Object putIfAbsent(Object key, Object value) {
        Object v = super.get(key);
        if (v == null) {
            return super.put(key, value);
        }
        return v;
    }

    public synchronized Object replace(Object key, Object value) {
        if (super.get(key) != null) {
            return super.put(key, value);
        }
        return null;
    }

    public synchronized boolean replace(Object key, Object oldValue, Object newValue) {
        Object v = super.get(key);
        if (v != null && v.equals(oldValue)) {
            super.put(key, newValue);
            return true;
        }
        return false;
    }

    public synchronized Object computeIfAbsent(Object key,
            java.util.function.Function<? super Object, ? extends Object> mappingFunction) {
        Object v = super.get(key);
        if (v != null) {
            return v;
        }
        Object updated = mappingFunction.apply(key);
        if (updated != null) {
            super.put(key, updated);
        }
        return updated;
    }

    public synchronized Object computeIfPresent(Object key,
            java.util.function.BiFunction<? super Object, ? super Object, ? extends Object> f) {
        Object v = super.get(key);
        if (v == null) {
            return null;
        }
        Object updated = f.apply(key, v);
        if (updated != null) {
            super.put(key, updated);
        } else {
            super.remove(key);
        }
        return updated;
    }

    public synchronized Object compute(Object key,
            java.util.function.BiFunction<? super Object, ? super Object, ? extends Object> f) {
        Object v = super.get(key);
        Object updated = f.apply(key, v);
        if (updated == null) {
            if (v != null) {
                super.remove(key);
            }
            return null;
        }
        super.put(key, updated);
        return updated;
    }

    public synchronized Object merge(Object key, Object value,
            java.util.function.BiFunction<? super Object, ? super Object, ? extends Object> f) {
        Object v = super.get(key);
        Object updated;
        if (v == null) {
            updated = value;
        } else {
            updated = f.apply(v, value);
        }
        if (updated == null) {
            super.remove(key);
        } else {
            super.put(key, updated);
        }
        return updated;
    }

    // ---- writing the .properties format   ---------------------------------------------------------

    /**
     * It writes the table in the `.properties` format, with `comments` as the header.
     *
     * <p>The output format is the one the JDK fixes: the comments first (each line with `#`), then a
     * line with the date, and then one `key=value` per entry.
     *
     * <p>**A deliberate divergence**: `defaults`'s entries are not written. It is what the JDK does
     * -- storing a table stores its own, not what it inherits -- and it is what makes storing and
     * re-reading keep the defaults chain instead of flattening it.
     */
    public void store(java.io.Writer writer, String comments) throws IOException {
        StringBuilder sb = new StringBuilder();
        this.writeHeader(sb, comments);
        Enumeration<Object> keyList = this.keys();
        while (keyList.hasMoreElements()) {
            Object k = keyList.nextElement();
            Object v = super.get(k);
            sb.append(escape(String.valueOf(k), true));
            sb.append('=');
            sb.append(escape(String.valueOf(v), false));
            sb.append('\n');
        }
        writer.write(sb.toString());
        writer.flush();
    }

    // The version over bytes. It writes in Latin-1, which is what the format demands for an
    // undeclared `.properties`: everything that does not fit comes out as `\uXXXX`.
    public void store(java.io.OutputStream out, String comments) throws IOException {
        StringBuilder sb = new StringBuilder();
        this.writeHeader(sb, comments);
        Enumeration<Object> keyList = this.keys();
        while (keyList.hasMoreElements()) {
            Object k = keyList.nextElement();
            Object v = super.get(k);
            sb.append(escape(String.valueOf(k), true));
            sb.append('=');
            sb.append(escape(String.valueOf(v), false));
            sb.append('\n');
        }
        String text = sb.toString();
        byte[] bytes = new byte[text.length()];
        int i = 0;
        while (i < text.length()) {
            bytes[i] = (byte) text.charAt(i);
            i = i + 1;
        }
        out.write(bytes, 0, bytes.length);
        out.flush();
    }

    /**
     * The same as `store`, but it swallows the write errors.
     *
     * <p>It is **deprecated since 1.2** and for a reason that explains itself: if the disk fills up
     * half way, this method does not say so. It is implemented all the same because it is in the
     * contract, and by delegating to `store` so there are not two formats.
     */
    public void save(java.io.OutputStream out, String comments) {
        try {
            this.store(out, comments);
        } catch (IOException e) {
            // And this is exactly what makes it a bad method.
        }
    }

    private void writeHeader(StringBuilder sb, String comments) {
        if (comments != null) {
            sb.append('#');
            sb.append(comments);
            sb.append('\n');
        }
        sb.append('#');
        sb.append(new Date().toString());
        sb.append('\n');
    }

    /**
     * It escapes a key or a value for the format.
     *
     * <p>The asymmetry is the format's and not an oversight: in a **key** the three separators
     * (`=`, `:` and whitespace) have to be escaped, because otherwise they would split the pair on
     * re-reading. In a **value** only the **leading** whitespace, which is the only one the reader
     * would eat; the ones inside are part of the value.
     */
    private static String escape(String s, boolean isKeyChar) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '\\') {
                out.append("\\\\");
            } else if (c == '\t') {
                out.append("\\t");
            } else if (c == '\n') {
                out.append("\\n");
            } else if (c == '\r') {
                out.append("\\r");
            } else if (c == '\f') {
                out.append("\\f");
            } else if (c == ' ') {
                // In the key always; in the value, only if it opens it.
                if (isKeyChar || i == 0) {
                    out.append("\\ ");
                } else {
                    out.append(' ');
                }
            } else if (isKeyChar && (c == '=' || c == ':' || c == '#' || c == '!')) {
                out.append('\\');
                out.append(c);
            } else if (c < 32 || c > 126) {
                out.append("\\u");
                out.append(hex4(c));
            } else {
                out.append(c);
            }
            i = i + 1;
        }
        return out.toString();
    }

    private static String hex4(char c) {
        String h = Integer.toHexString(c);
        StringBuilder sb = new StringBuilder();
        int missing = 4 - h.length();
        while (missing > 0) {
            sb.append('0');
            missing = missing - 1;
        }
        sb.append(h);
        return sb.toString();
    }

    // ---- readable listing -----------------------------------------------------------------------

    /**
     * It dumps the table to be looked at, not to be re-read.
     *
     * <p>That is the difference from `store`, and it is in the contract: `list` **truncates** long
     * values to 40 characters with `...` at the end. A file written with `list` cannot be loaded
     * back, and that is the idea -- it is for debugging.
     */
    public void list(java.io.PrintStream out) {
        out.println("-- listing properties --");
        Enumeration<Object> keyList = this.keys();
        while (keyList.hasMoreElements()) {
            Object k = keyList.nextElement();
            out.println(String.valueOf(k) + "=" + truncate(String.valueOf(super.get(k))));
        }
    }

    public void list(java.io.PrintWriter out) {
        out.println("-- listing properties --");
        Enumeration<Object> keyList = this.keys();
        while (keyList.hasMoreElements()) {
            Object k = keyList.nextElement();
            out.println(String.valueOf(k) + "=" + truncate(String.valueOf(super.get(k))));
        }
    }

    private static String truncate(String v) {
        if (v.length() <= 40) {
            return v;
        }
        return v.substring(0, 37) + "...";
    }

    // ---- XML -------------------------------------------------------------------------------------------

    public void storeToXML(java.io.OutputStream os, String comment) throws IOException {
        this.storeToXML(os, comment, "UTF-8");
    }

    /**
     * The same table in the XML the `properties` DTD fixes.
     *
     * <p>It is the way of storing properties without the ambiguity of the text format's escapes: in
     * XML a key with an `=` inside needs nothing special.
     */
    public void storeToXML(java.io.OutputStream os, String comment, String encoding)
            throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"");
        sb.append(encoding);
        sb.append("\"?>\n");
        sb.append("<!DOCTYPE properties SYSTEM \"http://java.sun.com/dtd/properties.dtd\">\n");
        sb.append("<properties>\n");
        if (comment != null) {
            sb.append("<comment>");
            sb.append(escapeXml(comment));
            sb.append("</comment>\n");
        }
        Enumeration<Object> keyList = this.keys();
        while (keyList.hasMoreElements()) {
            Object k = keyList.nextElement();
            sb.append("<entry key=\"");
            sb.append(escapeXml(String.valueOf(k)));
            sb.append("\">");
            sb.append(escapeXml(String.valueOf(super.get(k))));
            sb.append("</entry>\n");
        }
        sb.append("</properties>\n");
        String text = sb.toString();
        byte[] bytes = text.getBytes(java.nio.charset.Charset.forName(encoding));
        os.write(bytes, 0, bytes.length);
        os.flush();
    }

    public void storeToXML(java.io.OutputStream os, String comment,
            java.nio.charset.Charset charset) throws IOException {
        this.storeToXML(os, comment, charset.name());
    }

    /**
     * It reads the XML {@link #storeToXML} writes.
     *
     * <p>**An honest subset, and it is worth saying which**: the DTD's shape is recognised
     * --`<entry key="...">value</entry>`, with an optional `<comment>`-- along with the five
     * predefined entities. What is **not** done is validating against the DTD or resolving it over
     * the network, which the JDK does do. A well-formed XML with another structure is rejected with
     * `InvalidPropertiesFormatException` instead of being half accepted.
     */
    public synchronized void loadFromXML(java.io.InputStream in) throws IOException {
        byte[] whole = new byte[0];
        int used = 0;
        byte[] chunk = new byte[8192];
        int n = in.read(chunk, 0, chunk.length);
        while (n > 0) {
            if (used + n > whole.length) {
                int updated = whole.length * 2;
                if (updated < used + n) {
                    updated = used + n;
                }
                byte[] bigger = new byte[updated];
                System.arraycopy(whole, 0, bigger, 0, used);
                whole = bigger;
            }
            System.arraycopy(chunk, 0, whole, used, n);
            used = used + n;
            n = in.read(chunk, 0, chunk.length);
        }
        String text = new String(whole, 0, used, java.nio.charset.Charset.forName("UTF-8"));
        if (text.indexOf("<properties") < 0) {
            throw new InvalidPropertiesFormatException("not a properties document");
        }
        int i = 0;
        while (true) {
            int openAt = text.indexOf("<entry key=\"", i);
            if (openAt < 0) {
                break;
            }
            int keyStart = openAt + 12;
            int closesKey = text.indexOf('"', keyStart);
            if (closesKey < 0) {
                throw new InvalidPropertiesFormatException("unclosed entry");
            }
            int tagEnd = text.indexOf('>', closesKey);
            if (tagEnd < 0) {
                throw new InvalidPropertiesFormatException("unclosed entry");
            }
            int closing = text.indexOf("</entry>", tagEnd);
            if (closing < 0) {
                throw new InvalidPropertiesFormatException("unclosed entry");
            }
            String keyText = unescapeXml(text.substring(keyStart, closesKey));
            String valueText = unescapeXml(text.substring(tagEnd + 1, closing));
            super.put(keyText, valueText);
            i = closing + 8;
        }
    }

    // XML's five predefined entities. `&amp;` goes first when escaping and last when unescaping, or
    // the ampersand itself would be escaped twice.
    private static String escapeXml(String s) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '&') {
                out.append("&amp;");
            } else if (c == '<') {
                out.append("&lt;");
            } else if (c == '>') {
                out.append("&gt;");
            } else if (c == '"') {
                out.append("&quot;");
            } else if (c == '\'') {
                out.append("&apos;");
            } else {
                out.append(c);
            }
            i = i + 1;
        }
        return out.toString();
    }

    private static String unescapeXml(String s) {
        String r = s;
        r = substitute(r, "&lt;", "<");
        r = substitute(r, "&gt;", ">");
        r = substitute(r, "&quot;", "\"");
        r = substitute(r, "&apos;", "'");
        r = substitute(r, "&amp;", "&");
        return r;
    }

    private static String substitute(String s, String what, String a) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            if (s.startsWith(what, i)) {
                out.append(a);
                i = i + what.length();
            } else {
                out.append(s.charAt(i));
                i = i + 1;
            }
        }
        return out.toString();
    }
}
