package java.io;

import jdk.internal.io.Fs;

// KajiLibrary's java.io.FileOutputStream -- a stream of bytes written to a file.
//
// **What is written gathers in memory and goes to the disk on every `flush`, `close` or when the
// buffer fills up.** It is `FileInputStream`'s counterpart, for the same reason: the native writes
// the whole file at once because this VM has no open descriptors.
//
// Two consequences worth knowing:
//
//   - **it has to be closed or flushed.** A `FileOutputStream` abandoned without `close()` loses
//     whatever was left in the buffer. In the JDK closing is advisable too, but there the operating
//     system ends up writing what it has already been handed; here nothing is handed over until the
//     `flush`.
//   - the file is **truncated on construction** (except in `append` mode), like the JDK's: opening
//     for writing erases what was there, even if nothing is written afterwards.
//
// **`getChannel()` shares the position with the stream**, as the contract demands: writing through
// one moves the other, and moving the channel changes where the stream writes. Asking for it has a
// price, and that is why it is not paid until somebody asks: from then on the dumping stops being
// an append at the end and starts writing at the channel's position, which in this VM costs
// rewriting the whole file. Whoever never calls `getChannel()` writes exactly as before. The how is
// in `StreamChannels`.
//
// **The errors come out as a checked `IOException`**, just as in the JDK. There was a time when
// they did not: the package's bases (`InputStream`/`OutputStream`/`Closeable`) did not declare
// `throws IOException`, an override cannot widen the checked exceptions of what it overrides (JLS
// 8.4.8.3), and what failed here came out wrapped in an `UncheckedIOException`. The bases declare
// it now, so the wrapper was taken out: a caller's `catch (IOException e)` has to catch this, and
// with the unchecked one it went straight past them and killed their thread.
public class FileOutputStream extends OutputStream {

    // Past this it dumps by itself, so that writing a large file does not hold it whole twice in
    // memory. It does not change what is seen: the file ends up the same.
    private static final int LIMIT = 1 << 16;

    // Package-private and not private: `StreamChannels.ForOutput` **shares** these with this
    // stream, which is what `getChannel()` is about. See `StreamChannels`'s header.
    final String path;
    byte[] buf = new byte[256];
    int used = 0;
    boolean closed = false;

    /** This stream's channel, created on demand. One only: the contract says "the unique
     * object". */
    private StreamChannels.ForOutput channel;
    // Whether what comes next is appended to what has already been dumped. It starts in the mode
    // asked for and goes to `true` after the first dump: the second chunk has to be appended even
    // if the stream is not an appending one.
    private boolean appending;

    /**
     * Opens `name` for writing, **truncating** whatever was there.
     *
     * @throws FileNotFoundException if it cannot be written to
     */
    public FileOutputStream(String name) throws FileNotFoundException {
        this(name == null ? null : new File(name), false);
    }

    public FileOutputStream(String name, boolean append) throws FileNotFoundException {
        this(name == null ? null : new File(name), append);
    }

    public FileOutputStream(File file) throws FileNotFoundException {
        this(file, false);
    }

    /**
     * Opens `file` for writing.
     *
     * @param append whether what is written is appended at the end instead of replacing the
     *     contents
     * @throws FileNotFoundException if it is a directory, or cannot be written to
     */
    public FileOutputStream(File file, boolean append) throws FileNotFoundException {
        if (file == null) {
            throw new NullPointerException();
        }
        if (file.isDirectory()) {
            throw new FileNotFoundException(file.getPath() + " (Is a directory)");
        }
        this.path = file.getPath();
        this.appending = append;
        if (!append) {
            // Truncate **now**, not on the first write: opening for writing erases what was there
            // even if nothing is written afterwards, and it is what the JDK's does.
            if (!Fs.writeAllBytes(this.path, new byte[0], false)) {
                throw new FileNotFoundException(this.path + " (Permission denied)");
            }
            this.appending = true;
        }
    }

    /** Opens by descriptor. This library does not model descriptors; see the class note. */
    public FileOutputStream(FileDescriptor fdObj) {
        if (fdObj == null) {
            throw new NullPointerException();
        }
        this.path = null;
        this.appending = true;
    }

    public void write(int b) throws IOException {
        this.checkOpen();
        this.ensure(1);
        this.buf[this.used] = (byte) b;
        this.used = this.used + 1;
        if (this.used >= LIMIT) {
            this.flush();
        }
    }

    public void write(byte[] b, int off, int len) throws IOException {
        this.checkOpen();
        if (b == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || off + len > b.length) {
            throw new IndexOutOfBoundsException();
        }
        this.ensure(len);
        System.arraycopy(b, off, this.buf, this.used, len);
        this.used = this.used + len;
        if (this.used >= LIMIT) {
            this.flush();
        }
    }

    public void write(byte[] b) throws IOException {
        this.write(b, 0, b.length);
    }

    /** It dumps to the disk whatever is in the buffer. */
    public void flush() throws IOException {
        if (this.used == 0 || this.path == null) {
            return;
        }
        if (this.channel != null) {
            // There is a channel already: the one in charge is **its** position and not the end of
            // the file, so the dumping has to go through it. Otherwise a backwards `position()`
            // followed by a `write` on the stream would append at the end instead of writing where
            // it was asked to.
            this.channel.flushPending();
            return;
        }
        byte[] chunk = new byte[this.used];
        System.arraycopy(this.buf, 0, chunk, 0, this.used);
        if (!Fs.writeAllBytes(this.path, chunk, this.appending)) {
            throw new IOException("Could not write to " + this.path);
        }
        this.appending = true;
        this.used = 0;
    }

    public void close() throws IOException {
        if (!this.closed) {
            this.flush();
            this.closed = true;
        }
        // Closing the stream closes its channel: they are the same thing seen two ways.
        if (this.channel != null && this.channel.isOpen()) {
            this.channel.close();
        }
    }

    /**
     * This stream's channel, **with the same position**: writing through the stream moves the
     * channel and moving the channel changes where the stream writes. The why of its being a single
     * number, and the price of asking for it, are in `StreamChannels`.
     *
     * <p>Asking for it changes how this stream dumps: from appending at the end it moves to writing
     * at the channel's position, which in this VM costs rewriting the whole file. Whoever does not
     * ask for it writes as before.
     */
    public java.nio.channels.FileChannel getChannel() {
        if (this.channel == null) {
            this.channel = new StreamChannels.ForOutput(this);
        }
        return this.channel;
    }

    /** The descriptor. This library does not model them; see the class note. */
    public final FileDescriptor getFD() throws IOException {
        return new FileDescriptor();
    }

    private void ensure(int extra) {
        if (this.used + extra <= this.buf.length) {
            return;
        }
        int fresh = this.buf.length * 2;
        while (fresh < this.used + extra) {
            fresh = fresh * 2;
        }
        byte[] mas = new byte[fresh];
        System.arraycopy(this.buf, 0, mas, 0, this.used);
        this.buf = mas;
    }

    private void checkOpen() throws IOException {
        if (this.closed) {
            throw new IOException("Stream Closed");
        }
    }
}
