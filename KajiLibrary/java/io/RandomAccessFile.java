package java.io;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

// KajiLibrary's java.io.RandomAccessFile -- reading and writing a file in any order.
//
// It is `java.io`'s only class that is not a stream: the rest go forwards and this one moves about.
// Everything that sets it apart comes from that -- `seek`, `getFilePointer`, `setLength` -- and so
// does its implementing `DataInput` **and** `DataOutput` at once, which no stream does.
//
// <h2>What it is built on, and why on that</h2>
//
// **It delegates entirely to a `java.nio.channels.FileChannel`.** It stores neither the position
// nor the bytes: it asks for them. The reason is not saving code but keeping `getChannel()` from
// being able to lie.
//
// `getChannel()`'s contract says the channel's position and the file pointer are **the same
// number**: moving one moves the other. With a position of its own here and another in the channel,
// that contract has to be upheld by hand in nine methods, and the day one of them forgets, the
// caller finds out by reading from the wrong place -- silently, because reading from the wrong
// offset still returns bytes. With no position of its own there is nothing to keep in step: there
// is a single number.
//
// What is inherited from that choice, for better and for worse, is documented in `FileChannel`:
// every read goes to the disk and every write rewrites the whole file. It is O(n) per operation
// --writing one byte at the end of a one-megabyte file moves a megabyte-- and in exchange, when
// `write` returns, the bytes **are there**. For this class the price weighs more than for the
// others, because the typical use of a random-access file is precisely many small scattered writes.
// Whoever has that and cares about speed wants a buffer of their own, not this class.
//
// <h2>The `rws` and `rwd` modes are accepted because they are honoured</h2>
//
// Both ask for every write to reach the device before returning --`rwd` the data, `rws` data and
// metadata-- and here that already happens always, by the above. They are not ignored: they are
// honoured from the start. The only difference from the JDK is that they cost nothing.
//
// <h2>What is not the same as the JDK, said outright</h2>
//
// `getFD()` returns an **invalid** `FileDescriptor` --`valid()` gives false-- because this VM does
// not model descriptors; it is the same thing `FileInputStream` and `FileOutputStream` do, and it
// is explained there. It is declared all the same because the answer is checkable: whoever asks
// `valid()` gets a "no" and not an invented handle that is no use afterwards.
public class RandomAccessFile implements DataOutput, DataInput, Closeable {

    private final FileChannel channel;

    // Whether the mode allows writing. It is kept apart from the channel because the refusal has to
    // come out as an `IOException` --what this class's contract promises-- and not as the unchecked
    // `NonWritableChannelException` the channel would throw: the caller wrote a
    // `catch (IOException)` and that one would slip straight past them.
    private final boolean writable;

    private final String path;

    private boolean closed = false;

    /**
     * Opens `name` in the given mode.
     *
     * @throws NullPointerException if `name` or `mode` is `null`
     * @throws IllegalArgumentException if the mode is none of the four
     * @throws FileNotFoundException if it does not exist and the mode does not create it, or if it
     *     is a directory
     */
    public RandomAccessFile(String name, String mode) throws FileNotFoundException {
        this(name == null ? null : new File(name), mode);
    }

    /**
     * Opens `file` in the given mode.
     *
     * <p>`"r"` opens for reading and **does not create**; the three `"rw*"` create the file if it
     * is missing and **do not truncate** it if it was there. The second is what sets this class
     * apart from `FileOutputStream`, and it is deliberate: a random-access file is opened precisely
     * in order to modify parts of what is already there.
     *
     * @throws NullPointerException if `file` or `mode` is `null`
     * @throws IllegalArgumentException if the mode is none of the four
     * @throws FileNotFoundException if it does not exist and the mode does not create it, or if it
     *     is a directory
     */
    public RandomAccessFile(File file, String mode) throws FileNotFoundException {
        if (file == null) {
            throw new NullPointerException();
        }
        if (mode == null) {
            throw new NullPointerException();
        }
        boolean writes;
        if (mode.equals("r")) {
            writes = false;
        } else if (mode.equals("rw") || mode.equals("rws") || mode.equals("rwd")) {
            writes = true;
        } else {
            throw new IllegalArgumentException(
                "Illegal mode \"" + mode + "\" must be one of \"r\", \"rw\", \"rws\", or \"rwd\"");
        }
        this.writable = writes;
        this.path = file.getPath();

        // A directory is rejected apart and with a message of its own, just as in
        // `FileInputStream`: "it is a directory" and "it is not there" send one looking in
        // different places.
        if (file.isDirectory()) {
            throw new FileNotFoundException(this.path + " (Is a directory)");
        }
        FileChannel c;
        try {
            if (writes) {
                c = FileChannel.open(Path.of(this.path), StandardOpenOption.READ,
                        StandardOpenOption.WRITE, StandardOpenOption.CREATE);
            } else {
                c = FileChannel.open(Path.of(this.path), StandardOpenOption.READ);
            }
        } catch (NoSuchFileException ex) {
            throw new FileNotFoundException(this.path + " (No such file or directory)");
        } catch (IOException ex) {
            throw new FileNotFoundException(this.path + " (" + ex.getMessage() + ")");
        }
        this.channel = c;
    }

    // ---- the file as such ----------------------------------------------------------------------

    /** The descriptor. Invalid on purpose; see the class note. */
    public final FileDescriptor getFD() throws IOException {
        this.requireOpen();
        return new FileDescriptor();
    }

    /**
     * The channel, which **shares the position** with this object.
     *
     * <p>It is neither a copy nor a view: it is the one this class works on. That is why `seek(5)`
     * leaves the channel at 5 and `channel.position(7)` leaves `getFilePointer()` at 7, which is
     * exactly what the contract promises.
     */
    public final FileChannel getChannel() {
        return this.channel;
    }

    public long getFilePointer() throws IOException {
        this.requireOpen();
        return this.channel.position();
    }

    /**
     * Moves the pointer to `pos`.
     *
     * <p>It may go past the end: it does not fail and does not grow the file. It is what allows
     * writing a hole -- `seek` beyond and writing grows it filling with zeros -- and reading there
     * gives end of file.
     *
     * @throws IOException if `pos` is negative
     */
    public void seek(long pos) throws IOException {
        if (pos < 0) {
            throw new IOException("Negative seek offset");
        }
        this.requireOpen();
        this.channel.position(pos);
    }

    public long length() throws IOException {
        this.requireOpen();
        return this.channel.size();
    }

    /**
     * Sets the file's length.
     *
     * <p>Shortening **clamps the pointer** if it was past the new end; growing does not move it.
     * The bytes that appear on growing are zeros.
     *
     * @throws IOException if `newLength` is negative or if the file was opened read-only
     */
    public void setLength(long newLength) throws IOException {
        if (newLength < 0) {
            throw new IOException("Negative length");
        }
        this.requireWritable();
        long currentSize = this.channel.size();
        if (newLength < currentSize) {
            this.channel.truncate(newLength);
        } else if (newLength > currentSize) {
            // A single zero byte at the last position: the channel fills the gap in between, which
            // is the same mechanism by which one writes past the end.
            this.channel.write(ByteBuffer.wrap(new byte[1]), newLength - 1);
        }
    }

    public void close() throws IOException {
        if (this.closed) {
            return;             // closing twice is no error, and the contract says so
        }
        this.closed = true;
        this.channel.close();
    }

    // ---- raw reading ---------------------------------------------------------------------------

    /** The next byte as 0..255, or -1 at the end. */
    public int read() throws IOException {
        this.requireOpen();
        byte[] one = new byte[1];
        int n = this.channel.read(ByteBuffer.wrap(one));
        if (n <= 0) {
            return -1;
        }
        return one[0] & 0xFF;
    }

    public int read(byte[] b, int off, int len) throws IOException {
        this.requireOpen();
        if (b == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            // Zero and not -1 even at the end: no byte was asked for, so there was nothing that
            // could not be given. The JDK tells these two apart, and a loop confusing "I asked for
            // nothing" with "there is no more" ends before its time.
            return 0;
        }
        return this.channel.read(ByteBuffer.wrap(b, off, len));
    }

    public int read(byte[] b) throws IOException {
        return this.read(b, 0, b.length);
    }

    public final void readFully(byte[] b) throws IOException {
        this.readFully(b, 0, b.length);
    }

    /**
     * It fills the whole stretch or throws.
     *
     * <p>The difference from `read` is the end contract: `read` returns whatever there is,
     * `readFully` demands everything. It is what is needed in order to read an `int` -- four bytes
     * or nothing, because three bytes of an integer are not an integer.
     *
     * @throws EOFException if the file runs out first
     */
    public final void readFully(byte[] b, int off, int len) throws IOException {
        int readSoFar = 0;
        while (readSoFar < len) {
            int n = this.read(b, off + readSoFar, len - readSoFar);
            if (n < 0) {
                throw new EOFException();
            }
            readSoFar = readSoFar + n;
        }
    }

    /**
     * It skips up to `n` bytes, without going past the end.
     *
     * <p>It returns how many it really skipped, which may be fewer than `n` and is zero for a
     * negative `n`.
     */
    public int skipBytes(int n) throws IOException {
        if (n <= 0) {
            return 0;
        }
        long pos = this.getFilePointer();
        long len = this.length();
        long fresh = pos + n;
        if (fresh > len) {
            fresh = len;
        }
        this.seek(fresh);
        return (int) (fresh - pos);
    }

    // ---- raw writing ---------------------------------------------------------------------------

    public void write(int b) throws IOException {
        this.requireWritable();
        byte[] one = new byte[1];
        one[0] = (byte) b;
        this.channel.write(ByteBuffer.wrap(one));
    }

    public void write(byte[] b) throws IOException {
        this.write(b, 0, b.length);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        this.requireWritable();
        if (b == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return;
        }
        this.channel.write(ByteBuffer.wrap(b, off, len));
    }

    // ---- DataInput -----------------------------------------------------------------------------
    //
    // Everything below is format, not access: big-endian, two's complement, IEEE 754 and modified
    // UTF-8, exactly like `DataInputStream`. It is written over `read()` and not over the channel
    // so that the pointer's advance happens in one place.

    public final boolean readBoolean() throws IOException {
        return this.readUnsignedByte() != 0;
    }

    public final byte readByte() throws IOException {
        return (byte) this.readUnsignedByte();
    }

    public final int readUnsignedByte() throws IOException {
        int c = this.read();
        if (c < 0) {
            throw new EOFException();
        }
        return c;
    }

    public final short readShort() throws IOException {
        return (short) this.readUnsignedShort();
    }

    public final int readUnsignedShort() throws IOException {
        int a = this.readUnsignedByte();
        int b = this.readUnsignedByte();
        return (a << 8) | b;
    }

    public final char readChar() throws IOException {
        return (char) this.readUnsignedShort();
    }

    public final int readInt() throws IOException {
        int a = this.readUnsignedByte();
        int b = this.readUnsignedByte();
        int c = this.readUnsignedByte();
        int d = this.readUnsignedByte();
        return (a << 24) | (b << 16) | (c << 8) | d;
    }

    public final long readLong() throws IOException {
        long alta = this.readInt() & 0xFFFFFFFFL;
        long low = this.readInt() & 0xFFFFFFFFL;
        return (alta << 32) | low;
    }

    public final float readFloat() throws IOException {
        return Float.intBitsToFloat(this.readInt());
    }

    public final double readDouble() throws IOException {
        return Double.longBitsToDouble(this.readLong());
    }

    /**
     * One line, ended by `\n`, `\r` or `\r\n`; `null` if it was at the end already.
     *
     * <p>**Each byte is turned into a char as it stands**, without decoding. It is what the
     * contract says and that is why this class is no use for reading text that is not one byte per
     * character: an accent in UTF-8 is two bytes and comes out as two chars different from it. For
     * text there is `BufferedReader` over an `InputStreamReader`, which does decode.
     */
    public final String readLine() throws IOException {
        StringBuilder sb = new StringBuilder();
        boolean anything = false;
        while (true) {
            int c = this.read();
            if (c < 0) {
                break;
            }
            anything = true;
            if (c == '\n') {
                break;
            }
            if (c == '\r') {
                // Look at the next byte and give it back if it was not the pair's `\n`: without
                // this, a file with lone `\r`s would lose the first character of every line.
                long before = this.getFilePointer();
                int sig = this.read();
                if (sig != '\n' && sig >= 0) {
                    this.seek(before);
                }
                break;
            }
            sb.append((char) c);
        }
        if (!anything) {
            return null;
        }
        return sb.toString();
    }

    public final String readUTF() throws IOException {
        return DataInputStream.readUTF(this);
    }

    // ---- DataOutput ----------------------------------------------------------------------------

    public final void writeBoolean(boolean v) throws IOException {
        this.write(v ? 1 : 0);
    }

    public final void writeByte(int v) throws IOException {
        this.write(v);
    }

    public final void writeShort(int v) throws IOException {
        this.write((v >>> 8) & 0xFF);
        this.write(v & 0xFF);
    }

    public final void writeChar(int v) throws IOException {
        this.writeShort(v);
    }

    public final void writeInt(int v) throws IOException {
        this.write((v >>> 24) & 0xFF);
        this.write((v >>> 16) & 0xFF);
        this.write((v >>> 8) & 0xFF);
        this.write(v & 0xFF);
    }

    public final void writeLong(long v) throws IOException {
        this.writeInt((int) (v >>> 32));
        this.writeInt((int) v);
    }

    public final void writeFloat(float v) throws IOException {
        this.writeInt(Float.floatToIntBits(v));
    }

    public final void writeDouble(double v) throws IOException {
        this.writeLong(Double.doubleToLongBits(v));
    }

    /** One byte per character, keeping the low eight bits. See `readLine`'s note. */
    public final void writeBytes(String s) throws IOException {
        int i = 0;
        while (i < s.length()) {
            this.write(s.charAt(i) & 0xFF);
            i = i + 1;
        }
    }

    /** Two bytes per character, big-endian, and **with no length in front**: it is not
     * self-delimiting. */
    public final void writeChars(String s) throws IOException {
        int i = 0;
        while (i < s.length()) {
            this.writeChar(s.charAt(i));
            i = i + 1;
        }
    }

    /**
     * The text in modified UTF-8, with two bytes of length in front.
     *
     * @throws UTFDataFormatException if the encoding goes past 65535 bytes -- the length goes in
     *     two bytes and there is nowhere to put more
     */
    public final void writeUTF(String s) throws IOException {
        int len = s.length();
        int utf = 0;
        int i = 0;
        while (i < len) {
            int c = s.charAt(i);
            if (c >= 0x0001 && c <= 0x007F) {
                utf = utf + 1;
            } else if (c > 0x07FF) {
                utf = utf + 3;
            } else {
                utf = utf + 2;
            }
            i = i + 1;
        }
        if (utf > 65535) {
            throw new UTFDataFormatException("encoded string too long: " + utf + " bytes");
        }
        this.writeShort(utf);
        i = 0;
        while (i < len) {
            int c = s.charAt(i);
            if (c >= 0x0001 && c <= 0x007F) {
                this.write(c);
            } else if (c > 0x07FF) {
                this.write(0xE0 | ((c >> 12) & 0x0F));
                this.write(0x80 | ((c >> 6) & 0x3F));
                this.write(0x80 | (c & 0x3F));
            } else {
                this.write(0xC0 | ((c >> 6) & 0x1F));
                this.write(0x80 | (c & 0x3F));
            }
            i = i + 1;
        }
    }

    // ---- guards --------------------------------------------------------------------------------

    private void requireOpen() throws IOException {
        if (this.closed) {
            throw new IOException("Stream Closed");
        }
    }

    private void requireWritable() throws IOException {
        this.requireOpen();
        if (!this.writable) {
            throw new IOException("Access denied");
        }
    }
}
