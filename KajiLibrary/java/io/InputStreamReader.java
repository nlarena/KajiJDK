package java.io;

import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;

// KajiLibrary's java.io.InputStreamReader — the bridge from bytes to characters: a {@link Reader}
// that reads bytes from an {@link InputStream} and decodes them. The mirror of
// {@link OutputStreamWriter}, and what a {@link BufferedReader} over a raw stream wraps.
//
// The decoding is the identity Latin-1 mapping — one byte becomes one character. KajiJDK's console
// input is ASCII in practice, so this is exact for the text it ever reads.
public class InputStreamReader extends Reader {

    private final InputStream in;
    private final Charset charset;

    public InputStreamReader(InputStream in) {
        this.in = in;
        this.charset = StandardCharsets.UTF_8;
    }

    public InputStreamReader(InputStream in, String charsetName)
            throws UnsupportedEncodingException {
        this.in = in;
        Charset cs;
        try {
            cs = Charset.forName(charsetName);
        } catch (RuntimeException e) {
            throw new UnsupportedEncodingException(charsetName);
        }
        this.charset = cs;
    }

    public InputStreamReader(InputStream in, Charset cs) {
        this.in = in;
        this.charset = cs;
    }

    public InputStreamReader(InputStream in, CharsetDecoder dec) {
        this.in = in;
        this.charset = dec.charset();
    }

    /** The canonical name of the character encoding in use. */
    public String getEncoding() {
        return this.charset.name();
    }

    public int read() {
        int b = this.in.read();
        return b < 0 ? -1 : (char) b;
    }

    public int read(char[] cbuf, int off, int len) {
        int i = 0;
        while (i < len) {
            int b = this.in.read();
            if (b < 0) {
                return i == 0 ? -1 : i;
            }
            cbuf[off + i] = (char) b;
            i = i + 1;
        }
        return i;
    }

    public int read(CharBuffer target) {
        int len = target.remaining();
        char[] cbuf = new char[len];
        int n = read(cbuf, 0, len);
        if (n > 0) {
            target.put(cbuf, 0, n);
        }
        return n;
    }

    public boolean ready() {
        return this.in.available() > 0;
    }

    public void close() {
        this.in.close();
    }
}
