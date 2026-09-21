package java.io;

// KajiLibrary's java.io.StringBufferInputStream -- a `String`'s characters, served as bytes.
//
// **Deprecated since JDK 1.1, and one has to understand why so as not to reproduce the mistake by
// using it.** It converts each `char` to a byte by keeping the low 8 bits, that is, it **does not
// encode: it truncates**. For ASCII text it coincides with UTF-8 by chance; for anything else --an
// n-tilde, an accent, an ideograph-- it produces a byte representing that character in no encoding
// at all. The right replacement is `StringReader` if characters are wanted, or
// `new ByteArrayInputStream(s.getBytes(cs))` if bytes really are.
//
// The truncation **is reproduced on purpose**. It is this class's specified behaviour, and a
// program using it will be compensating for it somehow; "fixing" it here by encoding in UTF-8 would
// change the bytes under that program's feet, which is exactly the kind of surprise this library
// does not want to spring. A deprecated class is implemented as it is stated, not as it would be
// right.
//
// The three fields are `protected` and part of the public contract: a subclass may look at them and
// even move them, so they cannot be made private nor change meaning.
public class StringBufferInputStream extends InputStream {

    /** The text the bytes are read from. */
    protected String buffer;

    /** The next character to read. */
    protected int pos;

    /** How many characters `buffer` has. */
    protected int count;

    public StringBufferInputStream(String s) {
        this.buffer = s;
        this.count = s.length();
        this.pos = 0;
    }

    // `& 0xFF` over the char: here is the truncation the note above talks about.
    public synchronized int read() {
        if (this.pos >= this.count) {
            return -1;
        }
        int b = this.buffer.charAt(this.pos) & 0xFF;
        this.pos = this.pos + 1;
        return b;
    }

    public synchronized int read(byte[] b, int off, int len) {
        if (b == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        }
        if (this.pos >= this.count) {
            return -1;
        }
        int left = this.count - this.pos;
        int n = len;
        if (n > left) {
            n = left;
        }
        if (n <= 0) {
            return 0;
        }
        for (int i = 0; i < n; i++) {
            b[off + i] = (byte) this.buffer.charAt(this.pos + i);
        }
        this.pos = this.pos + n;
        return n;
    }

    public synchronized long skip(long n) {
        long k = n;
        if (k < 0) {
            return 0;
        }
        long remaining = this.count - this.pos;
        if (k > remaining) {
            k = remaining;
        }
        this.pos = this.pos + (int) k;
        return k;
    }

    public synchronized int available() {
        return this.count - this.pos;
    }

    // It goes back to the start of the text, not to a mark: this class has no marks.
    public synchronized void reset() {
        this.pos = 0;
    }
}
