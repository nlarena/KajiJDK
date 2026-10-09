package java.io;

// Same-package imports work around the frozen javac's finder (finding #4).
import java.io.BufferedReader;
import java.io.Reader;

// KajiLibrary's java.io.LineNumberReader — a BufferedReader that also keeps count.
//
// It is the smallest possible illustration of why decorators are worth the indirection:
// "which line am I on" is one integer and a handful of comparisons, and nothing about it
// is specific to files, strings or sockets. As a decorator it is written once and any
// character source acquires it. A compiler front end, a config parser and a log tailer all
// want it, and none of them has to teach its input class about line numbers.
//
// The subtlety is what counts as a line ending. '\n', '\r' and the pair "\r\n" all end
// exactly one line, so after a '\r' the reader must remember that a following '\n' has
// already been paid for — that is the `skipLF` flag, and getting it wrong is how a file
// with Windows line endings ends up with twice as many lines as it has.
public class LineNumberReader extends BufferedReader {

    // Our own handle on the wrapped Reader. BufferedReader keeps its own (private) copy,
    // and the JDK reaches it with `super.read()`; our bytecode generator has no support
    // for `super.method()` calls, so the composition is spelled out instead — which is
    // what the wrapping was really doing all along.
    private Reader src;

    private int lineNumber;
    private int markedLineNumber;

    // True when the last character consumed was '\r', so a '\n' arriving next belongs to
    // the terminator we already counted.
    private boolean skipLF;
    private boolean markedSkipLF;

    // True when characters have been seen since the last terminator — i.e. a final line
    // with no terminator on it is in progress. A file that ends "…\nlast" contains that
    // last line, so end of stream has to count it; without this flag the character-by-
    // character reads would report one line fewer than readLine() does on the same input.
    private boolean pendingLine;
    private boolean markedPendingLine;

    public LineNumberReader(Reader in) {
        super(in);
        this.src = in;
        this.reinit();
    }

    // The size hint is the buffer the JDK sizes with it; ours has nothing to tune, so it
    // is accepted and ignored rather than dropped from the API.
    public LineNumberReader(Reader in, int size) {
        super(in);
        this.src = in;
        this.reinit();
    }

    private void reinit() {
        this.lineNumber = 0;
        this.markedLineNumber = 0;
        this.skipLF = false;
        this.markedSkipLF = false;
        this.pendingLine = false;
        this.markedPendingLine = false;
    }

    public int getLineNumber() {
        return this.lineNumber;
    }

    // Settable because "line 1" is a convention, not a fact: a reader positioned at the
    // start of an included file, or one that skipped a header, wants to say where it is.
    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public int read() throws IOException {
        int c = this.src.read();
        if (this.skipLF) {
            this.skipLF = false;
            if (c == '\n') {
                c = this.src.read();
            }
        }
        if (c == '\r') {
            this.skipLF = true;
            this.endLine();
        } else if (c == '\n') {
            this.endLine();
        } else if (c < 0) {
            this.endOfInput();
        } else {
            this.pendingLine = true;
        }
        return c;
    }

    private void endLine() {
        this.lineNumber = this.lineNumber + 1;
        this.pendingLine = false;
    }

    // End of stream closes off a final line that had no terminator. Clearing the flag is
    // what stops a caller that keeps reading past the end from counting it again.
    private void endOfInput() {
        if (this.pendingLine) {
            this.lineNumber = this.lineNumber + 1;
            this.pendingLine = false;
        }
    }

    // A bulk read cannot just delegate and forget: the terminators are inside the block
    // the caller asked for, so we scan what we just handed over and count them there.
    public int read(char[] cbuf, int off, int len) throws IOException {
        int n = this.src.read(cbuf, off, len);
        if (n < 0) {
            this.endOfInput();
            return n;
        }
        for (int i = 0; i < n; i++) {
            char c = cbuf[off + i];
            boolean alreadyCounted = false;
            if (this.skipLF) {
                this.skipLF = false;
                if (c == '\n') {
                    alreadyCounted = true;
                }
            }
            if (!alreadyCounted) {
                if (c == '\r') {
                    this.skipLF = true;
                    this.endLine();
                } else if (c == '\n') {
                    this.endLine();
                } else {
                    this.pendingLine = true;
                }
            }
        }
        return n;
    }

    // Reimplemented rather than inherited: BufferedReader.readLine() reads through its own
    // private handle on the source, so a line consumed there would never reach our counter.
    public String readLine() throws IOException {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        boolean atEnd = false;
        boolean done = false;
        while (!done) {
            int c = this.src.read();
            if (this.skipLF) {
                this.skipLF = false;
                if (c == '\n') {
                    c = this.src.read();
                }
            }
            if (c < 0) {
                atEnd = true;
                done = true;
            } else if (c == '\n') {
                done = true;
            } else if (c == '\r') {
                // Do not consume the possible '\n' yet — it may not have arrived. The flag
                // defers that decision to whoever reads next.
                this.skipLF = true;
                done = true;
            } else {
                sb.append((char) c);
                count = count + 1;
            }
        }
        if (atEnd && count == 0) {
            // Nothing left. If read() had consumed part of an unterminated last line,
            // this is where that line finally gets counted.
            this.endOfInput();
            return null;
        }
        this.endLine();
        return sb.toString();
    }

    // Deliberately character by character through read(): skipping on the source directly
    // would be faster and would silently lose every line ending inside the skipped range.
    public long skip(long n) throws IOException {
        long remaining = n;
        boolean atEnd = false;
        while (remaining > 0L && !atEnd) {
            if (this.read() < 0) {
                atEnd = true;
            } else {
                remaining = remaining - 1L;
            }
        }
        return n - remaining;
    }

    // The mark has to carry the counter with it, or resetting would rewind the text and
    // leave the line number where it had got to.
    public void mark(int readAheadLimit) throws IOException {
        this.src.mark(readAheadLimit);
        this.markedLineNumber = this.lineNumber;
        this.markedSkipLF = this.skipLF;
        this.markedPendingLine = this.pendingLine;
    }

    public void reset() throws IOException {
        this.src.reset();
        this.lineNumber = this.markedLineNumber;
        this.skipLF = this.markedSkipLF;
        this.pendingLine = this.markedPendingLine;
    }
}
