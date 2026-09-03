package java.io;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;

// KajiLibrary's java.io.OutputStreamWriter — the bridge from characters to bytes: a {@link Writer}
// that encodes each character and hands the bytes to an underlying {@link OutputStream}. It is the
// adapter a {@link PrintWriter} over a raw stream wraps.
//
// The encoding here is the identity low-byte mapping (Latin-1): each character's low 8 bits become
// one byte. KajiJDK's console is byte-for-byte ASCII in practice, so this is exact for the text it
// ever prints; a full multi-byte encoder is more than the one console sink needs.
public class OutputStreamWriter extends Writer {

    private final OutputStream out;
    private final Charset charset;

    public OutputStreamWriter(OutputStream out) {
        this.out = out;
        this.charset = StandardCharsets.UTF_8;
    }

    public OutputStreamWriter(OutputStream out, String charsetName)
            throws UnsupportedEncodingException {
        this.out = out;
        Charset cs;
        try {
            cs = Charset.forName(charsetName);
        } catch (RuntimeException e) {
            throw new UnsupportedEncodingException(charsetName);
        }
        this.charset = cs;
    }

    public OutputStreamWriter(OutputStream out, Charset cs) {
        this.out = out;
        this.charset = cs;
    }

    public OutputStreamWriter(OutputStream out, CharsetEncoder enc) {
        this.out = out;
        this.charset = enc.charset();
    }

    /** The canonical name of the character encoding in use. */
    public String getEncoding() {
        return this.charset.name();
    }

    public void write(int c) throws IOException {
        this.out.write(c & 0xFF);
    }

    public void write(char[] cbuf, int off, int len) throws IOException {
        int i = 0;
        while (i < len) {
            this.out.write(cbuf[off + i] & 0xFF);
            i = i + 1;
        }
    }

    public void write(String str, int off, int len) throws IOException {
        int i = 0;
        while (i < len) {
            this.out.write(str.charAt(off + i) & 0xFF);
            i = i + 1;
        }
    }

    public void flush() throws IOException {
        this.out.flush();
    }

    public void close() throws IOException {
        this.out.close();
    }

    public Writer append(CharSequence csq) throws IOException {
        String s = csq == null ? "null" : csq.toString();
        write(s, 0, s.length());
        return this;
    }

    public Writer append(CharSequence csq, int start, int end) throws IOException {
        CharSequence cs = csq == null ? "null" : csq;
        String s = cs.subSequence(start, end).toString();
        write(s, 0, s.length());
        return this;
    }
}
