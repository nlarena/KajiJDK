package java.io;

// KajiLibrary's java.io.LineNumberInputStream -- it counts lines over a stream of bytes.
//
// **Deprecated since JDK 1.1**, and for the same reason as `StringBufferInputStream`: it counts
// lines over *bytes*, not over characters, so it only works with encodings in which one byte is one
// character. In UTF-16 the interleaved zeros ruin its count. The replacement is `LineNumberReader`,
// which does the same thing one level up, where there are characters already.
//
// What it does do well is **normalize the three line endings**: `\r`, `\n` and `\r\n` all come out
// as a single `\n`. That forces it to look one byte ahead when it sees a `\r` --one has to know
// whether what follows is a `\n` so as not to count two lines where there is one-- and that
// look-ahead byte is kept in `pushBack`. That is the class's whole trick.
//
// The fields are package-private, as in the JDK: they are not contract, but `mark`/`reset` have to
// be able to store and restore them together.
public class LineNumberInputStream extends FilterInputStream {

    // The byte read in excess while looking ahead after a `\r`, or -1 if there is none.
    int pushBack = -1;

    int lineNumber;

    int markLineNumber = 0;

    int markPushBack = -1;

    public LineNumberInputStream(InputStream in) {
        super(in);
    }

    // A lone `\r` and a `\r\n` are both worth one line and both come out as `\n`. To tell them
    // apart the next byte has to be read: if it is a `\n` it is discarded --the line has been
    // counted already-- and if not, it is kept for the next read.
    public int read() throws IOException {
        int c = this.pushBack;
        if (c != -1) {
            this.pushBack = -1;
        } else {
            c = this.in.read();
        }
        if (c == '\r') {
            this.pushBack = this.in.read();
            if (this.pushBack == '\n') {
                this.pushBack = -1;
            }
            this.lineNumber = this.lineNumber + 1;
            return '\n';
        }
        if (c == '\n') {
            this.lineNumber = this.lineNumber + 1;
            return '\n';
        }
        return c;
    }

    // Byte by byte and not in blocks, because the line-ending translation may consume two bytes
    // from below for each one that comes out. It is slow and it is what the JDK does; the class is
    // deprecated.
    public int read(byte[] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return 0;
        }
        int c = this.read();
        if (c == -1) {
            return -1;
        }
        b[off] = (byte) c;
        int i = 1;
        while (i < len) {
            c = this.read();
            if (c == -1) {
                break;
            }
            b[off + i] = (byte) c;
            i = i + 1;
        }
        return i;
    }

    // Skipping goes through `read` too: skipping raw bytes would count the lines wrongly.
    public long skip(long n) throws IOException {
        long skipped = 0;
        while (skipped < n) {
            if (this.read() == -1) {
                break;
            }
            skipped = skipped + 1;
        }
        return skipped;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public int getLineNumber() {
        return this.lineNumber;
    }

    // **Half**, and it is no arithmetic slip: in the worst case everything coming is `\r\n`, and
    // each pair of bytes from below turns into a single byte from here. `available` promises a
    // floor, not an estimate, so it has to assume the worst case. The `+1` is the look-ahead byte,
    // which is read already and certainly comes out.
    public int available() throws IOException {
        int below = this.in.available() / 2;
        if (this.pushBack == -1) {
            return below;
        }
        return below + 1;
    }

    // The line number and the look-ahead byte are stored together with the mark below: restoring
    // the position without restoring both would leave the count out of step.
    public void mark(int readlimit) {
        this.markLineNumber = this.lineNumber;
        this.markPushBack = this.pushBack;
        this.in.mark(readlimit);
    }

    public void reset() throws IOException {
        this.lineNumber = this.markLineNumber;
        this.pushBack = this.markPushBack;
        this.in.reset();
    }
}
