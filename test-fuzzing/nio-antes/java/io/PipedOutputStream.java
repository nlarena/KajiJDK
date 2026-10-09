package java.io;

// KajiLibrary's java.io.PipedOutputStream -- the writing end of a pipe between two threads.
//
// It has almost no state of its own: it keeps whoever it writes to and passes everything on. The
// buffer, the waiting and the synchronization live on the reader's side (`PipedInputStream`), which
// is where they have to be -- there is a single buffer and a single monitor, and putting them on
// one side alone is what avoids having to take two locks and order them.
//
// **The two ends have to be in different threads.** Writing and reading from the same thread hangs
// as soon as the buffer fills up: the writer waits for room, and the only one that could make room
// is the very thread that is waiting. It is no defect of this implementation, it is what a pipe is.
//
// The exceptions are the JDK's and checked, as in `PipedInputStream`; see the note over there for
// why for a while they were not.
public class PipedOutputStream extends OutputStream {

    private PipedInputStream sink;

    /** Connects this end to the given reader. */
    public PipedOutputStream(PipedInputStream snk) throws IOException {
        this.connect(snk);
    }

    /** Unconnected: a `connect` is needed before writing. */
    public PipedOutputStream() {
    }

    /**
     * Connects this end to the given reader and leaves the pipe empty.
     *
     * @throws IOException if either of the two ends was already connected
     */
    public synchronized void connect(PipedInputStream snk) throws IOException {
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

    /** Writes one byte. **It blocks if the pipe is full.** */
    public void write(int b) throws IOException {
        if (this.sink == null) {
            throw new IOException("Pipe not connected");
        }
        this.sink.receive(b);
    }

    /** Writes `len` bytes. **It blocks until they all fit.** */
    public void write(byte[] b, int off, int len) throws IOException {
        if (this.sink == null) {
            throw new IOException("Pipe not connected");
        }
        if (b == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return;
        }
        this.sink.receive(b, off, len);
    }

    // It flushes nothing --there is nothing stored on this side-- but **wakes the reader**. It
    // serves when the writer has left data and wants the other side to see it now, without waiting
    // for the `wait`'s one-second deadline.
    public synchronized void flush() throws IOException {
        if (this.sink != null) {
            synchronized (this.sink) {
                this.sink.notifyAll();
            }
        }
    }

    /**
     * Closes the writing end.
     *
     * <p>The reader does **not** see end of stream straight away: it first finishes reading
     * whatever was left in the buffer, and only when that empties does the -1 reach it. Closing
     * does not discard what was written.
     */
    public void close() throws IOException {
        if (this.sink != null) {
            this.sink.receivedLast();
        }
    }
}
