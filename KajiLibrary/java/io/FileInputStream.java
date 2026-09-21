package java.io;

import jdk.internal.io.Fs;

// KajiLibrary's java.io.FileInputStream -- a stream of bytes read from a file.
//
// **The file is read whole on construction.** It is the visible difference from the JDK's, which
// opens a descriptor and reads as it is asked to, and it is worth keeping in mind:
//
//   - a one-gigabyte file goes into memory at once, not piece by piece;
//   - the changes another process makes to it **after** the stream is constructed are not seen --
//     what is read is the snapshot from the moment of opening;
//   - in exchange, `close()` can neither fail nor be missing, and there is no descriptor left
//     hanging.
//
// The reason is one level below, in `jdk.internal.io.Fs`: the native reads the whole file because
// there are no open handles in this VM. When there are, this class is rewritten without anything
// using it noticing -- `Scanner` and `Formatter` talk to `InputStream`, not to this.
//
// **`getChannel()` returns a channel over that same snapshot**, and it shares the position with the
// stream just as the contract demands: reading from one moves the other. It reads from the snapshot
// and not from the disk on purpose -- sharing the position but not the contents would be worse than
// sharing nothing -- so what was said above about what is not seen holds just as well when reading
// through the channel. The how is in `StreamChannels`.
//
// **The errors come out as a checked `IOException`**, just as in the JDK. There was a time when
// they did not: the package's bases (`InputStream`/`OutputStream`/`Closeable`) did not declare
// `throws IOException`, an override cannot widen the checked exceptions of what it overrides (JLS
// 8.4.8.3), and what failed here came out wrapped in an `UncheckedIOException`. The bases declare
// it now, so the wrapper was taken out: a caller's `catch (IOException e)` has to catch this, and
// with the unchecked one it went straight past them and killed their thread.
public class FileInputStream extends InputStream {

    // Package-private and not private: `StreamChannels.ForInput` **shares** these three with this
    // stream, which is what `getChannel()` is about. See `StreamChannels`'s header.
    final byte[] data;
    // `long` and not `int` even though the contents are a `byte[]`: the channel may position itself
    // past the end --it is legal, and what follows is that the reads give -1-- and `position()` has
    // to return what it was given. With an `int` it would have to be clamped and would stop being
    // the same number. The uses as an index are cast, always after checking the end.
    long pos;
    private long mark = -1;
    boolean closed = false;

    /** This stream's channel, created on demand. One only: the contract says "the unique
     * object". */
    private java.nio.channels.FileChannel channel;

    /**
     * Opens `name` for reading.
     *
     * @throws FileNotFoundException if it does not exist, is a directory, or cannot be read
     */
    public FileInputStream(String name) throws FileNotFoundException {
        this(name == null ? null : new File(name));
    }

    /**
     * Opens `file` for reading.
     *
     * @throws FileNotFoundException if it does not exist, is a directory, or cannot be read
     */
    public FileInputStream(File file) throws FileNotFoundException {
        if (file == null) {
            throw new NullPointerException();
        }
        // A directory is rejected apart and with a message of its own: reading one "fails" in a
        // different way from the file not being there, and confusing the two sends one looking in
        // the wrong place.
        if (file.isDirectory()) {
            throw new FileNotFoundException(file.getPath() + " (Is a directory)");
        }
        byte[] b = Fs.readAllBytes(file.getPath());
        if (b == null) {
            throw new FileNotFoundException(file.getPath() + " (No such file or directory)");
        }
        this.data = b;
        this.pos = 0;
    }

    /** Opens by descriptor. This library does not model descriptors; see the class note. */
    public FileInputStream(FileDescriptor fdObj) {
        if (fdObj == null) {
            throw new NullPointerException();
        }
        this.data = new byte[0];
        this.pos = 0;
    }

    public int read() throws IOException {
        this.checkOpen();
        if (this.pos >= this.data.length) {
            return -1;
        }
        int b = this.data[(int) this.pos] & 0xff;
        this.pos = this.pos + 1;
        return b;
    }

    public int read(byte[] b, int off, int len) throws IOException {
        this.checkOpen();
        if (b == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || off + len > b.length) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return 0;
        }
        if (this.pos >= this.data.length) {
            return -1;                       // end of stream, and **not** zero: two different things
        }
        int n = (int) (this.data.length - this.pos);
        if (n > len) {
            n = len;
        }
        System.arraycopy(this.data, (int) this.pos, b, off, n);
        this.pos = this.pos + n;
        return n;
    }

    public int read(byte[] b) throws IOException {
        return this.read(b, 0, b.length);
    }

    public long skip(long n) throws IOException {
        this.checkOpen();
        if (n <= 0L) {
            return 0L;
        }
        long remaining = this.data.length - this.pos;
        long skipped = n < remaining ? n : remaining;
        this.pos = this.pos + skipped;
        return skipped;
    }

    /**
     * How many bytes are left.
     *
     * <p>Here it is exact --the file is whole in memory-- whereas in the JDK it is an estimate. It
     * is one of the few things reading everything at once wins on.
     */
    public int available() throws IOException {
        this.checkOpen();
        return (int) (this.data.length - this.pos);
    }

    public boolean markSupported() {
        return true;
    }

    public synchronized void mark(int readlimit) {
        this.mark = this.pos;
    }

    public synchronized void reset() throws IOException {
        this.checkOpen();
        if (this.mark < 0) {
            throw new IOException("Resetting to invalid mark");
        }
        this.pos = this.mark;
    }

    // (`getChannel` is further down, next to `close`.)

    public void close() throws IOException {
        this.closed = true;
        // Closing the stream closes its channel: they are the same thing seen two ways.
        if (this.channel != null && this.channel.isOpen()) {
            this.channel.close();
        }
    }

    /**
     * This stream's channel, **with the same position**: reading from the stream moves the channel
     * and moving the channel changes where the stream reads from. It is not a copy kept in step, it
     * is a single number; the why is in `StreamChannels`.
     *
     * <p>It reads from the same snapshot as the stream --the one taken when it was constructed--
     * and not from the disk, for the same reason: sharing the position but not the contents would
     * be worse than sharing nothing. And it is read-only, like the JDK's.
     */
    public java.nio.channels.FileChannel getChannel() {
        if (this.channel == null) {
            this.channel = new StreamChannels.ForInput(this);
        }
        return this.channel;
    }

    /** The descriptor. This library does not model them; see the class note. */
    public final FileDescriptor getFD() throws IOException {
        return new FileDescriptor();
    }

    private void checkOpen() throws IOException {
        if (this.closed) {
            throw new IOException("Stream Closed");
        }
    }
}
