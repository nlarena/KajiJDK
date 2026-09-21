package java.io;

// KajiLibrary's java.io.PipedReader -- the reading end of a pipe of **characters**.
//
// The same circular buffer as `PipedInputStream` with `char` instead of `byte`, and for the same
// reason `Reader` and `InputStream` exist separately: passing text through a pipe of bytes forces
// encoding on one side and decoding on the other, and if both ends are Java that is pure work --and
// a source of characters split down the middle when a multi-byte character falls right on the
// buffer's edge.
//
// Everything said in `PipedInputStream` holds just as well: the `in == -1` sentinel for telling
// empty from full, the waits with a one-second deadline so as to be able to detect that the other
// end has died, and the synchronization concentrated on this side.
//
// The exceptions are the JDK's and checked, as in `PipedInputStream`; see the note over there for
// why for a while they were not.
public class PipedReader extends Reader {

    boolean closedByWriter = false;

    boolean closedByReader = false;

    boolean connected = false;

    Thread readSide;
    Thread writeSide;

    private static final int DEFAULT_PIPE_SIZE = 1024;

    char[] buffer;

    /** Where the next character is written; **-1 if the pipe is empty**. */
    int in = -1;

    /** Where the next character is read from. */
    int out = 0;

    /** Connects this end to `src` with the default-sized buffer. */
    public PipedReader(PipedWriter src) throws IOException {
        this(src, DEFAULT_PIPE_SIZE);
    }

    /**
     * Connects this end to `src`.
     *
     * @throws IllegalArgumentException if `pipeSize` is not positive
     */
    public PipedReader(PipedWriter src, int pipeSize) throws IOException {
        this.initBuffer(pipeSize);
        this.connect(src);
    }

    /** Unconnected: a `connect` is needed before using it. */
    public PipedReader() {
        this.initBuffer(DEFAULT_PIPE_SIZE);
    }

    /**
     * Unconnected, with a buffer of the given size.
     *
     * @throws IllegalArgumentException if `pipeSize` is not positive
     */
    public PipedReader(int pipeSize) {
        this.initBuffer(pipeSize);
    }

    private void initBuffer(int pipeSize) {
        if (pipeSize <= 0) {
            throw new IllegalArgumentException("Pipe size <= 0");
        }
        this.buffer = new char[pipeSize];
    }

    /**
     * Connects this end to the given `PipedWriter`.
     *
     * @throws IOException if either of the two ends was already connected
     */
    public void connect(PipedWriter src) throws IOException {
        src.connect(this);
    }

    /** It receives a character from the writer. **It blocks if the pipe is full.** */
    synchronized void receive(int c) throws IOException {
        if (!this.connected) {
            throw new IOException("Pipe not connected");
        }
        if (this.closedByWriter || this.closedByReader) {
            throw new IOException("Pipe closed");
        }
        if (this.readSide != null && !this.readSide.isAlive()) {
            throw new IOException("Read end dead");
        }

        this.writeSide = Thread.currentThread();
        while (this.in == this.out) {
            if (this.readSide != null && !this.readSide.isAlive()) {
                throw new IOException("Pipe broken");
            }
            this.notifyAll();
            try {
                this.wait(1000);
            } catch (InterruptedException ex) {
                throw new InterruptedIOException();
            }
        }
        if (this.in < 0) {
            this.in = 0;
            this.out = 0;
        }
        this.buffer[this.in] = (char) c;
        this.in = this.in + 1;
        if (this.in >= this.buffer.length) {
            this.in = 0;
        }
    }

    synchronized void receive(char[] c, int off, int len) throws IOException {
        int from = off;
        int left = len;
        while (left > 0) {
            this.receive(c[from]);
            from = from + 1;
            left = left - 1;
        }
    }

    synchronized void receivedLast() {
        this.closedByWriter = true;
        this.notifyAll();
    }

    /**
     * Reads one character. **It blocks until there is one**, or until the writer closes.
     *
     * @return the character, or -1 if it has run out
     */
    public synchronized int read() throws IOException {
        if (!this.connected) {
            throw new IOException("Pipe not connected");
        }
        if (this.closedByReader) {
            throw new IOException("Pipe closed");
        }
        if (this.writeSide != null && !this.writeSide.isAlive()
                && !this.closedByWriter && this.in < 0) {
            throw new IOException("Write end dead");
        }

        this.readSide = Thread.currentThread();
        int trials = 2;
        while (this.in < 0) {
            if (this.closedByWriter) {
                return -1;
            }
            if (this.writeSide != null && !this.writeSide.isAlive()) {
                trials = trials - 1;
                if (trials < 0) {
                    throw new IOException("Pipe broken");
                }
            }
            this.notifyAll();
            try {
                this.wait(1000);
            } catch (InterruptedException ex) {
                throw new InterruptedIOException();
            }
        }
        int ret = this.buffer[this.out];
        this.out = this.out + 1;
        if (this.out >= this.buffer.length) {
            this.out = 0;
        }
        if (this.in == this.out) {
            this.in = -1;
        }
        return ret;
    }

    /**
     * Reads up to `len` characters. It blocks until there is **at least one**.
     *
     * @return how many were read, or -1 if it has run out
     */
    public synchronized int read(char[] cbuf, int off, int len) throws IOException {
        if (cbuf == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || len > cbuf.length - off) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return 0;
        }

        int c = this.read();
        if (c < 0) {
            return -1;
        }
        cbuf[off] = (char) c;
        int rlen = 1;
        while (this.in >= 0 && rlen < len) {
            int available;
            if (this.in > this.out) {
                available = this.in - this.out;
            } else {
                available = this.buffer.length - this.out;
            }
            if (available > len - rlen) {
                available = len - rlen;
            }
            System.arraycopy(this.buffer, this.out, cbuf, off + rlen, available);
            this.out = this.out + available;
            rlen = rlen + available;
            if (this.out >= this.buffer.length) {
                this.out = 0;
            }
            if (this.in == this.out) {
                this.in = -1;
            }
        }
        return rlen;
    }

    /** Whether there is at least one character ready to be read without blocking. */
    public synchronized boolean ready() throws IOException {
        if (!this.connected) {
            throw new IOException("Pipe not connected");
        }
        if (this.closedByReader) {
            throw new IOException("Pipe closed");
        }
        return this.in >= 0;
    }

    /** Closes the reading end. */
    public void close() throws IOException {
        this.in = -1;
        this.closedByReader = true;
    }
}
