package java.io;

// KajiLibrary's java.io.PipedInputStream -- the reading end of a pipe between two threads.
//
// ===============================================================================================
// WHAT IT IS AND WHY IT CAN BE WRITTEN IN FULL
// ===============================================================================================
//
// A circular buffer with waiting. It touches neither the disk nor the network nor the operating
// system: all it needs is a monitor, and `wait`/`notifyAll` already work on this VM. That is why
// this class is implemented complete and without concessions, unlike the file streams.
//
// The buffer is circular with the classic convention: `out` is the next byte to read, `in` the next
// place to write, and **`in == -1` means empty**. That sentinel value is needed because with two
// indices alone `in == out` would be ambiguous --full and empty would look the same-- and the
// alternative (leaving one place unused) would waste space. With the sentinel, `in == out` means
// **full** unambiguously.
//
// ===============================================================================================
// THE WAITS AND THE NOTIFICATIONS
// ===============================================================================================
//
// Just like the JDK: it waits with `wait(1000)` and notifies with `notifyAll()`. The deadline is
// not there so as not to miss a notification --every notification is in place-- but so as to be
// able to **check whether the thread on the other side is still alive**: an end whose thread dies
// without closing sends no notification, and without waking up every so often the other one would
// wait for ever instead of breaking with "Pipe broken".
//
// The `notifyAll()` is at every point where **the state changes**: after leaving data (both
// `receive`s), after consuming (both `read`s), when waiting for room and when closing. The one in
// `receive` is the one that matters and the one that was missing: without it, the reader only heard
// that there was data when its own deadline expired, and a large transfer got stuck with the writer
// unable to get into the monitor. It was fixed together with the VM bug that was hiding it --see
// finding #471, where a `wait` with an expired deadline left the thread inside the monitor's wait
// set.
//
// **The exceptions are the JDK's and they are checked.** It was not always so: while
// `InputStream.read()` did not declare `throws IOException`, an override could not either --JLS
// 8.4.8.3 forbids widening the checked ones-- and "Pipe not connected", "Pipe broken" and "Write
// end dead" came out wrapped in an `UncheckedIOException`. The base declares it now, so the wrapper
// went: leaving it was the worst of both worlds, a signature promising `IOException` and a body
// throwing something no `catch (IOException)` catches.
public class PipedInputStream extends InputStream {

    boolean closedByWriter = false;

    volatile boolean closedByReader = false;

    boolean connected = false;

    // Who reads and who writes, so as to be able to ask whether the one on the other side is still
    // alive. It is the only way of telling "it has not written anything yet" from "it has died and
    // is never going to write".
    Thread readSide;
    Thread writeSide;

    private static final int DEFAULT_PIPE_SIZE = 1024;

    /** The buffer's size when no other is asked for. */
    protected static final int PIPE_SIZE = DEFAULT_PIPE_SIZE;

    /** The circular buffer. */
    protected byte[] buffer;

    /** Where the next byte is written; **-1 if the pipe is empty**. */
    protected int in = -1;

    /** Where the next byte is read from. */
    protected int out = 0;

    /** Connects this end to `src` with the default-sized buffer. */
    public PipedInputStream(PipedOutputStream src) throws IOException {
        this(src, DEFAULT_PIPE_SIZE);
    }

    /**
     * Connects this end to `src`.
     *
     * @param pipeSize the buffer's size
     * @throws IllegalArgumentException if `pipeSize` is not positive
     */
    public PipedInputStream(PipedOutputStream src, int pipeSize) throws IOException {
        this.initBuffer(pipeSize);
        this.connect(src);
    }

    /** Unconnected: a `connect` is needed before using it. */
    public PipedInputStream() {
        this.initBuffer(DEFAULT_PIPE_SIZE);
    }

    /**
     * Unconnected, with a buffer of the given size.
     *
     * @throws IllegalArgumentException if `pipeSize` is not positive
     */
    public PipedInputStream(int pipeSize) {
        this.initBuffer(pipeSize);
    }

    private void initBuffer(int pipeSize) {
        if (pipeSize <= 0) {
            throw new IllegalArgumentException("Pipe Size <= 0");
        }
        this.buffer = new byte[pipeSize];
    }

    /**
     * Connects this end to the given `PipedOutputStream`.
     *
     * @throws IOException if either of the two ends was already connected
     */
    public void connect(PipedOutputStream src) throws IOException {
        src.connect(this);
    }

    /**
     * It receives a byte from the writer. **It blocks if the pipe is full.**
     *
     * @throws IOException if the pipe is broken, closed or unconnected
     */
    protected synchronized void receive(int b) throws IOException {
        this.checkCanReceive();
        this.writeSide = Thread.currentThread();
        if (this.in == this.out) {
            this.waitForRoom();
        }
        if (this.in < 0) {
            this.in = 0;
            this.out = 0;
        }
        this.buffer[this.in] = (byte) (b & 0xFF);
        this.in = this.in + 1;
        if (this.in >= this.buffer.length) {
            this.in = 0;
        }
        // The buffer went from empty to holding something: the reader has to be woken. Without this
        // notification the reader only hears when its own deadline expires, and with deadline-less
        // waits it never hears at all.
        this.notifyAll();
    }

    // It receives a block. It is package-private because only `PipedOutputStream` calls it.
    synchronized void receive(byte[] b, int off, int len) throws IOException {
        this.checkCanReceive();
        this.writeSide = Thread.currentThread();
        int bytesToTransfer = len;
        int from = off;
        while (bytesToTransfer > 0) {
            if (this.in == this.out) {
                this.waitForRoom();
            }
            int nextTransferAmount = 0;
            if (this.out < this.in) {
                // The data is in one stretch: the free room runs from `in` to the end.
                nextTransferAmount = this.buffer.length - this.in;
            } else if (this.in < this.out) {
                if (this.in == -1) {
                    // Empty: the whole buffer can be written from the start.
                    this.in = 0;
                    this.out = 0;
                    nextTransferAmount = this.buffer.length - this.in;
                } else {
                    // The free room runs from `in` to `out`.
                    nextTransferAmount = this.out - this.in;
                }
            }
            if (nextTransferAmount > bytesToTransfer) {
                nextTransferAmount = bytesToTransfer;
            }
            System.arraycopy(b, from, this.buffer, this.in, nextTransferAmount);
            bytesToTransfer = bytesToTransfer - nextTransferAmount;
            from = from + nextTransferAmount;
            this.in = this.in + nextTransferAmount;
            if (this.in >= this.buffer.length) {
                this.in = 0;
            }
            // One per stretch and not one at the end: if the block does not fit at once, the reader
            // has to be able to consume what is already there in order to make room for the rest.
            this.notifyAll();
        }
    }

    private void checkCanReceive() throws IOException {
        if (!this.connected) {
            throw new IOException("Pipe not connected");
        }
        if (this.closedByWriter || this.closedByReader) {
            throw new IOException("Pipe closed");
        }
        if (this.readSide != null && !this.readSide.isAlive()) {
            throw new IOException("Read end dead");
        }
    }

    // It waits for the reader to make room. The `notifyAll` inside is not a courtesy: if the reader
    // is asleep waiting for data and the writer is asleep waiting for room, somebody has to wake
    // the other.
    private void waitForRoom() throws IOException {
        while (this.in == this.out) {
            this.checkCanReceive();
            this.notifyAll();
            try {
                this.wait(1000);
            } catch (InterruptedException ex) {
                throw new InterruptedIOException();
            }
        }
    }

    // It reports that the writer has closed. Whatever is left in the buffer can still be read: only
    // when it empties will the reader see end of stream.
    synchronized void receivedLast() {
        this.closedByWriter = true;
        this.notifyAll();
    }

    /**
     * Reads one byte. **It blocks until there is one**, or until the writer closes.
     *
     * @return the byte, or -1 if it has run out
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
        // Two rounds of grace before declaring the pipe broken: the writer may have finished right
        // after leaving data, and in that case it has to be handed over.
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
        int ret = this.buffer[this.out] & 0xFF;
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
     * Reads up to `len` bytes. It blocks until there is **at least one**, and then returns whatever
     * there is without waiting to fill the array.
     *
     * @return how many were read, or -1 if it has run out
     */
    public synchronized int read(byte[] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return 0;
        }

        // The first one goes through `read()`, which is the one that waits and the one that reports
        // end of stream.
        int c = this.read();
        if (c < 0) {
            return -1;
        }
        b[off] = (byte) c;
        int rlen = 1;
        // From here on it waits no longer: `in >= 0` while anything is left in the buffer.
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
            System.arraycopy(this.buffer, this.out, b, off + rlen, available);
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

    /** How many bytes can be read without blocking. */
    public synchronized int available() throws IOException {
        if (this.in < 0) {
            return 0;
        }
        if (this.in == this.out) {
            return this.buffer.length;
        }
        if (this.in > this.out) {
            return this.in - this.out;
        }
        return this.in + this.buffer.length - this.out;
    }

    /** Closes the reading end. A writer that goes on writing is going to fail. */
    public void close() throws IOException {
        this.closedByReader = true;
        synchronized (this) {
            this.in = -1;
        }
    }
}
