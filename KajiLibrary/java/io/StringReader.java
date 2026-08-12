package java.io;

// Same-package import works around the frozen javac's finder (finding #4).
import java.io.Reader;

// KajiLibrary's java.io.StringReader — a Reader whose character source is a String, read
// through String.charAt. Pure Java over the String primitives.
public class StringReader extends Reader {

    private String str;
    private int length;
    private int next;

    public StringReader(String s) {
        this.str = s;
        this.length = s.length();
        this.next = 0;
    }

    public int read(char[] cbuf, int off, int len) {
        if (this.next >= this.length) {
            return -1;
        }
        int n = len;
        if (this.length - this.next < n) {
            n = this.length - this.next;
        }
        for (int i = 0; i < n; i++) {
            cbuf[off + i] = this.str.charAt(this.next + i);
        }
        this.next = this.next + n;
        return n;
    }

    public void close() {
        this.str = null;
    }
}
