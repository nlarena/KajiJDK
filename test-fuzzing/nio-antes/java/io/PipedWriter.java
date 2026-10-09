package java.io;

// KajiLibrary's java.io.PipedWriter -- the writing end of a pipe of characters.
//
// `PipedOutputStream`'s mirror: almost no state of its own, all the work on the `PipedReader`'s
// side. The two ends have to be in different threads, for the usual reason -- writing until the
// buffer fills from the same thread that would have to empty it hangs.
//
// The exceptions are the JDK's and checked, as in `PipedInputStream`; see the note over there for
// why for a while they were not.
public class PipedWriter extends Writer {

    private PipedReader sink;

    private boolean closed = false;

    /** Connects this end to the given reader. */
    public PipedWriter(PipedReader snk) throws IOException {
        this.connect(snk);
    }

    /** Unconnected: a `connect` is needed before writing. */
    public PipedWriter() {
    }

    /**
     * Connects this end to the given reader and leaves the pipe empty.
     *
     * @throws IOException if either of the two ends was already connected
     */
    public synchronized void connect(PipedReader snk) throws IOException {
        if (snk == null) {
            throw new NullPointerException();
        }
        if (this.sink != null || snk.connected) {
            throw new IOException("Already connected");
        }
        this.sink = snk;
        snk.in = -1;
        snk.out = 0;
        snk.connected = true;
    }

    /** Writes one character. **It blocks if the pipe is full.** */
    public void write(int c) throws IOException {
        if (this.sink == null) {
            throw new IOException("Pipe not connected");
        }
        this.sink.receive(c);
    }

    /** Writes `len` characters. **It blocks until they all fit.** */
    public void write(char[] cbuf, int off, int len) throws IOException {
        if (this.sink == null) {
            throw new IOException("Pipe not connected");
        }
        if (cbuf == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || len > cbuf.length - off) {
            throw new IndexOutOfBoundsException();
        }
        this.sink.receive(cbuf, off, len);
    }

    // It wakes the reader; there is nothing stored on this side to flush.
    public synchronized void flush() throws IOException {
        if (this.sink != null) {
            if (this.sink.closedByReader || this.closed) {
                throw new IOException("Pipe closed");
            }
            synchronized (this.sink) {
                this.sink.notifyAll();
            }
        }
    }

    /**
     * Closes the writing end.
     *
     * <p>The reader finishes reading whatever was left in the buffer before seeing end of stream.
     */
    public void close() throws IOException {
        this.closed = true;
        if (this.sink != null) {
            this.sink.receivedLast();
        }
    }
}
