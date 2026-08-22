package java.io;

// Same-package import works around the frozen javac's finder (finding #4).
import java.io.Writer;

// KajiLibrary's java.io.BufferedWriter — buffering for character output, plus newLine().
//
// It extends Writer rather than FilterWriter, which looks inconsistent until you see why:
// FilterWriter forwards, and a buffer must NOT forward — every write has to land in the
// array first. There would be nothing left of the inherited behaviour to keep. So it
// holds the wrapped Writer in a private field and delegates explicitly, which is the same
// composition, just without pretending to be a pass-through.
//
// newLine() lives here rather than on Writer because writing a line terminator is only
// meaningful for text you are already batching: it is the natural partner of BufferedReader
// .readLine() on the other side of the file.
public class BufferedWriter extends Writer {

    // The wrapped sink. Private, exactly as in the JDK: subclasses have no business
    // writing around the buffer, which is the one thing that would break the invariant.
    private Writer out;

    // Characters written by the caller but not yet pushed to `out`: cb[0..nextChar).
    private char[] cb;
    private int nextChar;

    public BufferedWriter(Writer out) {
        this.out = out;
        // Spelled out rather than shared as a `static final int` (finding #112).
        this.init(8192);
    }

    public BufferedWriter(Writer out, int size) {
        this.out = out;
        this.init(size);
    }

    private void init(int size) {
        this.cb = new char[size];
        this.nextChar = 0;
    }

    public void write(int c) {
        if (this.nextChar >= this.cb.length) {
            this.flushBuffer();
        }
        this.cb[this.nextChar] = (char) c;
        this.nextChar = this.nextChar + 1;
    }

    public void write(char[] cbuf, int off, int len) {
        // Same bargain as BufferedOutputStream: a chunk that would fill the buffer on its
        // own is handed straight to the sink instead of being copied twice.
        if (len >= this.cb.length) {
            this.flushBuffer();
            this.out.write(cbuf, off, len);
        } else {
            if (len > this.cb.length - this.nextChar) {
                this.flushBuffer();
            }
            System.arraycopy(cbuf, off, this.cb, this.nextChar, len);
            this.nextChar = this.nextChar + len;
        }
    }

    // Overridden (Writer's version would cut a char[] copy of the slice first) so that a
    // String written through a buffer costs one copy, into the buffer, and no more.
    public void write(String str, int off, int len) {
        for (int i = 0; i < len; i++) {
            this.write(str.charAt(off + i));
        }
    }

    // The separator is written as '\n' rather than the platform's: KajiLibrary has no
    // System.lineSeparator() yet, and reading a `static final` String constant off another
    // class is not an option either (finding #110 compiles that to a getfield that traps).
    public void newLine() {
        this.write('\n');
    }

    public void flush() {
        this.flushBuffer();
        this.out.flush();
    }

    private void flushBuffer() {
        if (this.nextChar > 0) {
            this.out.write(this.cb, 0, this.nextChar);
            this.nextChar = 0;
        }
    }

    public void close() {
        this.flushBuffer();
        this.out.close();
    }
}
