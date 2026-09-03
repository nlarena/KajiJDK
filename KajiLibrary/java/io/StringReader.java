package java.io;

// Same-package import works around the frozen javac's finder (finding #4).
import java.io.Reader;

// KajiLibrary's java.io.StringReader — a Reader whose character source is a String, read
// through String.charAt. Pure Java over the String primitives.
public class StringReader extends Reader {

    private String str;
    private int length;
    private int next;
    private int mark;

    public StringReader(String s) {
        this.str = s;
        this.length = s.length();
        this.next = 0;
        this.mark = 0;
    }

    public int read() throws IOException {
        if (this.next >= this.length) {
            return -1;
        }
        char c = this.str.charAt(this.next);
        this.next = this.next + 1;
        return c;
    }

    public int read(char[] cbuf, int off, int len) throws IOException {
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

    public long skip(long n) throws IOException {
        if (this.next >= this.length) {
            return 0;
        }
        long avail = this.length - this.next;
        long k = n < avail ? n : avail;
        if (k < 0) {
            long back = this.next;
            k = k < -back ? -back : k;
        }
        this.next = this.next + (int) k;
        return k;
    }

    public boolean ready() throws IOException {
        return true;
    }

    public boolean markSupported() {
        return true;
    }

    public void mark(int readAheadLimit) throws IOException {
        this.mark = this.next;
    }

    public void reset() throws IOException {
        this.next = this.mark;
    }

    public void close() {
        this.str = null;
    }
}
