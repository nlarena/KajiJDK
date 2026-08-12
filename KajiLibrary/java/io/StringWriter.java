package java.io;

// Same-package import works around the frozen javac's finder (finding #4).
import java.io.Writer;

// KajiLibrary's java.io.StringWriter — a Writer that accumulates everything written into a
// StringBuilder, retrievable via toString(). Pure Java, and a nice dogfood: it drives our
// own StringBuilder (append/toString).
public class StringWriter extends Writer {

    private StringBuilder buf;

    public StringWriter() {
        this.buf = new StringBuilder();
    }

    public void write(int c) {
        this.buf.append((char) c);
    }

    public void write(char[] cbuf, int off, int len) {
        for (int i = 0; i < len; i++) {
            this.buf.append(cbuf[off + i]);
        }
    }

    public void write(String str) {
        this.buf.append(str);
    }

    public void flush() {
    }

    public void close() {
    }

    public String toString() {
        return this.buf.toString();
    }
}
