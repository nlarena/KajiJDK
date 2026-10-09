package java.util.jar;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A JAR's manifest: a main section of attributes, and then zero or more per-entry sections.
 *
 * <p>This class is the only thing a JAR has and a ZIP does not, and that is why it is where all the
 * package's work is. The rest --`JarFile`, `JarEntry`, the two streams-- leans on `java.util.zip`,
 * which was already whole.
 *
 * <h2>The format, in the part that can be got wrong</h2>
 *
 * <p><b>Lines are cut at 72 bytes</b> and the continuation starts with a space. The cut is by
 * **UTF-8 bytes**, not by characters: the first line carries 72 bytes of content and each
 * continuation carries the space plus 71.
 *
 * <p>And here is what to look at before writing the code, because intuition says the opposite:
 * <b>the JDK splits multibyte characters down the middle</b>. It was checked against JDK 25 with a
 * value of 60 tilde-n --two bytes each-- and the first line ends in a lone `c3` that the continuation
 * completes with its `b1`. That is correct, and the reason is the reader: the continuation is joined
 * at the **byte** level and only then decoded as UTF-8, so the character is reassembled before
 * anybody looks at it. A writer that refused to split characters would also be readable, but it
 * would not give the same bytes as the JDK; giving the same ones was chosen.
 *
 * <p>The counterpart is obligatory: <b>the reader here joins bytes, never `String`s</b>. Decoding
 * each physical line separately and concatenating the texts would break every value with a split
 * character, and it would do it in silence --with a `?` in the middle--. That is why {@link #read}
 * works over `byte[]`.
 *
 * <p>The other rule is the sections': a blank line closes the main one, and every following section
 * has to start with `Name: `. A section with no `Name` is an error, not an anonymous section.
 *
 * <h2>What is left out, and why</h2>
 *
 * <p>Nothing of the public surface. Of the members the JDK declares and are not here, all are
 * **package-private** --the two internal constructors, `getTrustedAttributes`, `getErrorPosition`--;
 * by the contract rule, the internals are free. `getTrustedAttributes` in particular only makes
 * sense with signature verification, which this package does not do (see {@link JarFile}'s
 * header).
 */
public class Manifest implements Cloneable {

    private final Attributes attr = new Attributes();

    // A `LinkedHashMap` and not a `HashMap` like the JDK's: the sections' order is not contract, but
    // that writing the same manifest twice gives the same bytes is one in practice, and with a
    // `HashMap` it was not.
    private final Map<String, Attributes> entries = new LinkedHashMap<String, Attributes>();

    /** An empty manifest. */
    public Manifest() {
    }

    /**
     * A manifest read from that stream.
     *
     * @throws IOException if the stream has no well-formed manifest
     */
    public Manifest(InputStream is) throws IOException {
        read(is);
    }

    /** A copy of `man`. */
    public Manifest(Manifest man) {
        this.attr.putAll(man.getMainAttributes());
        for (Map.Entry<String, Attributes> e : man.getEntries().entrySet()) {
            this.entries.put(e.getKey(), new Attributes(e.getValue()));
        }
    }

    /** The main section's attributes. */
    public Attributes getMainAttributes() {
        return this.attr;
    }

    /**
     * The per-entry sections, indexed by the name their `Name` line gives.
     *
     * <p>It is the live map: modifying it modifies the manifest.
     */
    public Map<String, Attributes> getEntries() {
        return this.entries;
    }

    /** That entry's attributes, or `null` if the manifest has no section for it. */
    public Attributes getAttributes(String name) {
        return getEntries().get(name);
    }

    /** It empties the main section and every per-entry section. */
    public void clear() {
        this.attr.clear();
        this.entries.clear();
    }

    public boolean equals(Object o) {
        if (!(o instanceof Manifest)) {
            return false;
        }
        Manifest m = (Manifest) o;
        return this.attr.equals(m.attr) && this.entries.equals(m.entries);
    }

    public int hashCode() {
        return this.attr.hashCode() + this.entries.hashCode();
    }

    /** A copy. */
    public Object clone() {
        return new Manifest(this);
    }

    // ---- writing --------------------------------------------------------------------------------

    /**
     * It writes the manifest in the format any JAR tool reads.
     *
     * <p>If the main section has neither `Manifest-Version` nor `Signature-Version` no attribute is
     * written: see the note in `Attributes.writeMain`, where the reason is.
     */
    public void write(OutputStream out) throws IOException {
        DataOutputStream dataOut = new DataOutputStream(out);
        this.attr.writeMain(dataOut);
        for (Map.Entry<String, Attributes> e : this.entries.entrySet()) {
            println72(dataOut, "Name: " + e.getKey());
            e.getValue().write(dataOut);
        }
        dataOut.flush();
    }

    /**
     * It writes a logical line folded at 72 bytes.
     *
     * <p>The first byte is written on its own and then blocks of 71 follow: that way the first line
     * comes to 1 + 71 = 72 bytes and each continuation to 1 (the space) + 71 = 72. It is exactly the
     * JDK's split, including that a multibyte character can be broken at the cut.
     */
    static void println72(OutputStream out, String line) throws IOException {
        if (!line.isEmpty()) {
            byte[] bytes = line.getBytes(StandardCharsets.UTF_8);
            int length = bytes.length;
            out.write(bytes[0]);
            int pos = 1;
            while (length - pos > 71) {
                out.write(bytes, pos, 71);
                pos = pos + 71;
                println(out);
                out.write(' ');
            }
            out.write(bytes, pos, length - pos);
        }
        println(out);
    }

    /** The format's line ending, which is CRLF and not the system's. */
    static void println(OutputStream out) throws IOException {
        out.write('\r');
        out.write('\n');
    }

    // ---- reading --------------------------------------------------------------------------------

    /**
     * It reads a manifest from that stream.
     *
     * <p>The whole stream is read into memory before parsing. It is the same decision `ZipFile` took
     * in this library and for the same reason: a manifest does not reach megabytes, and in exchange
     * no half-consumed state is left if the parse fails.
     *
     * <p>What is read is **merged** with whatever the manifest already had, which is what the JDK's
     * javadoc says. It does not replace.
     */
    public void read(InputStream is) throws IOException {
        byte[] data = readAll(is);
        List<byte[]> logicalLines = new ArrayList<byte[]>();
        List<Integer> lineNumbers = new ArrayList<Integer>();
        unfold(data, logicalLines, lineNumbers);

        // It is not cleared: the JDK's contract says what is read is **merged** with what was there.
        int i = 0;
        // The main section: up to the first blank line.
        while (i < logicalLines.size() && logicalLines.get(i).length != 0) {
            readHeader(this.attr, logicalLines.get(i), lineNumbers.get(i).intValue());
            i = i + 1;
        }
        // And then one section per entry. Extra blank lines are ignored.
        while (i < logicalLines.size()) {
            if (logicalLines.get(i).length == 0) {
                i = i + 1;
                continue;
            }
            int num = lineNumbers.get(i).intValue();
            String[] pair = splitHeader(logicalLines.get(i), num);
            if (!pair[0].equalsIgnoreCase("Name")) {
                throw new IOException("invalid manifest format (line " + num + ")");
            }
            String name = pair[1];
            Attributes section = this.entries.get(name);
            if (section == null) {
                section = new Attributes();
                this.entries.put(name, section);
            }
            i = i + 1;
            while (i < logicalLines.size() && logicalLines.get(i).length != 0) {
                readHeader(section, logicalLines.get(i), lineNumbers.get(i).intValue());
                i = i + 1;
            }
        }
    }

    /**
     * It splits the content into **logical** lines: every physical line starting with a space is
     * appended to the previous one, without that space.
     *
     * <p>It is appended at the byte level. See the class's header: it is what lets the writer split
     * UTF-8 characters at the 72 cut.
     *
     * <p>All three line endings --CRLF, LF and bare CR-- count, because the JDK accepts all three.
     */
    private static void unfold(byte[] data, List<byte[]> logicalLines, List<Integer> lineNumbers)
            throws IOException {
        int pos = 0;
        int num = 0;
        while (pos < data.length) {
            num = num + 1;
            int end = pos;
            while (end < data.length && data[end] != '\n' && data[end] != '\r') {
                end = end + 1;
            }
            int next = end;
            if (next < data.length) {
                if (data[next] == '\r' && next + 1 < data.length
                        && data[next + 1] == '\n') {
                    next = next + 2;
                } else {
                    next = next + 1;
                }
            }
            int length = end - pos;
            if (length > 0 && data[pos] == ' ') {
                // A continuation. There has to be a non-empty logical line ahead of it: otherwise
                // the manifest starts with a space and there is nothing to append it to.
                if (logicalLines.isEmpty() || logicalLines.get(logicalLines.size() - 1).length == 0) {
                    throw new IOException("misplaced continuation line (line " + num + ")");
                }
                byte[] previous = logicalLines.get(logicalLines.size() - 1);
                byte[] joined = new byte[previous.length + length - 1];
                System.arraycopy(previous, 0, joined, 0, previous.length);
                System.arraycopy(data, pos + 1, joined, previous.length, length - 1);
                logicalLines.set(logicalLines.size() - 1, joined);
            } else {
                byte[] line = new byte[length];
                System.arraycopy(data, pos, line, 0, length);
                logicalLines.add(line);
                lineNumbers.add(Integer.valueOf(num));
            }
            pos = next;
        }
    }

    /** It splits `name: value` into its two halves, with the errors the JDK gives. */
    private static String[] splitHeader(byte[] line, int num) throws IOException {
        int i = 0;
        while (i < line.length && line[i] != ':') {
            i = i + 1;
        }
        // The `:` **and** the space behind it are needed: `A:one` is invalid to the JDK.
        if (i >= line.length || i + 1 >= line.length || line[i + 1] != ' ') {
            throw new IOException("invalid header field (line " + num + ")");
        }
        String name = new String(line, 0, i, StandardCharsets.UTF_8);
        String value = new String(line, i + 2, line.length - i - 2, StandardCharsets.UTF_8);
        return new String[] { name, value };
    }

    private static void readHeader(Attributes target, byte[] line, int num) throws IOException {
        String[] pair = splitHeader(line, num);
        try {
            target.putValue(pair[0], pair[1]);
        } catch (IllegalArgumentException e) {
            throw new IOException("invalid header field name: " + pair[0] + " (line " + num + ")");
        }
    }

    private static byte[] readAll(InputStream is) throws IOException {
        if (is == null) {
            throw new NullPointerException("is");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n = is.read(buf, 0, buf.length);
        while (n > 0) {
            out.write(buf, 0, n);
            n = is.read(buf, 0, buf.length);
        }
        return out.toByteArray();
    }
}
