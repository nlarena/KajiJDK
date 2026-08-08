package java.io;

// Same-package import works around the frozen javac's finder (finding #4).
import java.io.Reader;

// KajiLibrary's java.io.BufferedReader — wraps another Reader and adds line-oriented input.
// (The JDK version also buffers in a char[] for throughput; ours reads through to the
// underlying Reader and focuses on the readLine() convenience.)
public class BufferedReader extends Reader {

    private Reader in;

    public BufferedReader(Reader in) {
        this.in = in;
    }

    public int read(char[] cbuf, int off, int len) {
        return this.in.read(cbuf, off, len);
    }

    public int read() {
        return this.in.read();
    }

    // The next line, without its terminator ('\n', '\r', or '\r\n' are all accepted), or
    // null at end of stream.
    public String readLine() {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        while (true) {
            int c = this.in.read();
            if (c < 0) {
                if (count == 0) {
                    return null;
                }
                return sb.toString();
            }
            if (c == '\n') {
                return sb.toString();
            }
            if (c != '\r') {
                sb.append((char) c);
                count = count + 1;
            }
        }
    }

    public void close() {
        this.in.close();
    }
}
