package java.util.zip;

import java.io.InputStream;

// Reads the ZIP container as a STREAM: entry after entry, front to back, without ever seeking.
// That constraint is what separates it from `ZipFile`, and it has a real consequence — a
// streaming reader cannot use the central directory at the end of the archive, so it has to
// trust each entry's local header, and when the local header says "sizes unknown" (flag bit 3)
// it must find the end of the data by other means.
//
// An entry written with a **data descriptor** is read just the same: the end of the data is marked
// by the deflate stream itself --the inflater knows when it finished-- and the 16-byte descriptor
// that follows is skipped, taking from it the CRC and the sizes the local header did not carry.
//
// The one delicate part is that the inflater **reads ahead**: when it finishes, the bytes it did not
// consume have already left the stream below. `Inflater.getRemaining()` says how many they are, and
// they are handed back to a tiny push-back queue so that the descriptor and the next header are read
// whole. Without that, everything after starts off by a few bytes.
//
// This header used to end by saying the `throws IOException` clauses were omitted throughout because
// of finding #104. #104 is closed and every method that should declare the clause declares it.
public class ZipInputStream extends InflaterInputStream {

    private static final int LOCAL_SIG = 0x04034b50;
    private static final int DESCRIPTOR_SIG = 0x08074b50;

    private ZipEntry current;
    private long remaining;
    private boolean entryEof;
    // Whether the current entry carried its sizes in a descriptor **after** the data.
    private boolean hasDescriptor;
    // The bytes the inflater read ahead and have to be looked at again. It is a tiny queue --never
    // larger than the fill buffer-- and it exists because the inflater consumes in blocks.
    private byte[] pushBack = new byte[0];
    private int pushBackAt;

    // The charset the **entry names** are decoded with. UTF-8 by default, which is what the JDK says
    // and what any modern tool produces.
    private final java.nio.charset.Charset charset;

    /** It reads the archive, decoding the names in UTF-8. */
    public ZipInputStream(InputStream in) {
        this(in, java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * It reads the archive, decoding the names with `charset`.
     *
     * <p>It exists because **the ZIP format does not say what encoding the names are in**. There is a
     * flag bit promising UTF-8, but plenty of old archives do not set it and store the name in the
     * code page of the system that created it. Without being able to choose, those names are read
     * wrongly and there is no way of fixing it from outside.
     *
     * @throws NullPointerException if `charset` is `null`
     */
    public ZipInputStream(InputStream in, java.nio.charset.Charset charset) {
        super(in, new Inflater(true));
        if (charset == null) {
            throw new NullPointerException("charset");
        }
        this.charset = charset;
    }

    // Advances to the next entry and returns its metadata, or null at the end of the archive.
    public ZipEntry getNextEntry() throws java.io.IOException {
        closeEntry();
        ZipEntry entry = null;
        int sig = readInt();
        if (sig == LOCAL_SIG) {
            readShort();                       // version needed
            int flags = readShort();
            int method = readShort();
            long dosTime = (long) readInt() & 0xffffffffL;
            long crc = (long) readInt() & 0xffffffffL;
            long csize = (long) readInt() & 0xffffffffL;
            long size = (long) readInt() & 0xffffffffL;
            int nameLen = readShort();
            int extraLen = readShort();
            String name = readString(nameLen);
            skipBytes(extraLen);
            entry = createZipEntry(name);
            entry.setMethod(method);
            entry.setTime(dosTime);
            if ((flags & 8) == 0) {
                entry.setCrc(crc);
                entry.setCompressedSize(csize);
                entry.setSize(size);
                remaining = csize;
                hasDescriptor = false;
            } else {
                // The sizes come in a descriptor **after** the data. The end is known from the
                // deflate stream, not from a counter.
                remaining = -1;
                hasDescriptor = true;
            }
            current = entry;
            entryEof = false;
            // The inflater starts from scratch for each entry: each is a deflate stream of its own.
            inf.reset();
        }
        return entry;
    }

    public void closeEntry() throws java.io.IOException {
        if (current != null) {
            if (remaining > 0) {
                skipBytes((int) remaining);
            } else if (remaining < 0) {
                // Unknown size: it is drained to the end of the deflate stream. It is the only thing
                // that says where the data ends when the local header did not.
                byte[] scratch = new byte[512];
                int n = read(scratch, 0, scratch.length);
                while (n > 0) {
                    n = read(scratch, 0, scratch.length);
                }
            }
            ZipEntry justClosed = current;
            current = null;
            remaining = 0;
            entryEof = true;
            if (hasDescriptor) {
                pushBackLeftovers();
                readDescriptor(justClosed);
                hasDescriptor = false;
            }
        }
    }

    public int available() throws java.io.IOException {
        int n = 1;
        if (entryEof || current == null) {
            n = 0;
        }
        return n;
    }

    public int read() throws java.io.IOException {
        byte[] one = new byte[1];
        int n = read(one, 0, 1);
        int result = -1;
        if (n == 1) {
            result = one[0] & 0xff;
        }
        return result;
    }

    public int read(byte[] b, int off, int len) throws java.io.IOException {
        int result = -1;
        if (current != null && !entryEof) {
            if (current.getMethod() == ZipEntry.STORED) {
                int want = len;
                if (remaining >= 0 && (long) want > remaining) {
                    want = (int) remaining;
                }
                if (want == 0) {
                    entryEof = true;
                } else {
                    int n = in.read(b, off, want);
                    if (n == -1) {
                        entryEof = true;
                    } else {
                        remaining = remaining - (long) n;
                        result = n;
                    }
                }
            } else {
                int n = readInflated(b, off, len);
                if (n == -1) {
                    entryEof = true;
                } else {
                    result = n;
                }
            }
        }
        return result;
    }

    public long skip(long n) throws java.io.IOException {
        byte[] scratch = new byte[512];
        long skipped = 0;
        boolean done = false;
        while (skipped < n && !done) {
            long left = n - skipped;
            int want = scratch.length;
            if (left < (long) want) {
                want = (int) left;
            }
            int got = read(scratch, 0, want);
            if (got == -1) {
                done = true;
            } else {
                skipped = skipped + (long) got;
            }
        }
        return skipped;
    }

    // The seam a subclass overrides to get its own entry type back from `getNextEntry`.
    protected ZipEntry createZipEntry(String name) {
        return new ZipEntry(name);
    }

    // ---- field reading, little-endian like the whole format ----

    /**
     * The inflater's fill, **starting from the push-back queue**.
     *
     * <p>It is the other half of the descriptor fix. The bytes the inflater read ahead were left in
     * the queue; if the fill ignored them and went straight to the stream, the next entry would start
     * by skipping exactly those bytes. With two entries in a row it shows at once: the second reads
     * zero.
     */
    protected void fill() throws java.io.IOException {
        int pendingBytes = this.pushBack.length - this.pushBackAt;
        if (pendingBytes <= 0) {
            len = in.read(buf, 0, buf.length);
            if (len > 0) {
                inf.setInput(buf, 0, len);
            }
            return;
        }
        int howManyBytes = pendingBytes;
        if (howManyBytes > buf.length) {
            howManyBytes = buf.length;
        }
        System.arraycopy(this.pushBack, this.pushBackAt, buf, 0, howManyBytes);
        this.pushBackAt = this.pushBackAt + howManyBytes;
        int total = howManyBytes;
        // If there is room left, it is topped up from the stream: a larger block gives the inflater
        // more to work with and saves a round trip.
        if (total < buf.length) {
            int more = in.read(buf, total, buf.length - total);
            if (more > 0) {
                total = total + more;
            }
        }
        len = total;
        inf.setInput(buf, 0, len);
    }

    // Every field read goes through here: first what the inflater handed back, then the stream.
    // Without this single entry point, half the fields would be read from the wrong place right
    // after a compressed entry.
    private int readByte() throws java.io.IOException {
        if (this.pushBackAt < this.pushBack.length) {
            int b = this.pushBack[this.pushBackAt] & 0xff;
            this.pushBackAt = this.pushBackAt + 1;
            return b;
        }
        return in.read();
    }

    // The bytes the inflater took from the stream and did not consume go back into the queue.
    private void pushBackLeftovers() {
        int leftOver = inf.getRemaining();
        if (leftOver <= 0) {
            return;
        }
        byte[] fresh = new byte[leftOver];
        System.arraycopy(buf, len - leftOver, fresh, 0, leftOver);
        this.pushBack = fresh;
        this.pushBackAt = 0;
    }

    // The 16-byte record that follows the data when the local header did not carry the sizes. The
    // signature is **optional** in the format, so it is looked at and only consumed if it is there.
    private void readDescriptor(ZipEntry entry) throws java.io.IOException {
        int firstOne = this.readInt();
        long crc;
        if (firstOne == DESCRIPTOR_SIG) {
            crc = (long) this.readInt() & 0xffffffffL;
        } else {
            crc = (long) firstOne & 0xffffffffL;
        }
        long csize = (long) this.readInt() & 0xffffffffL;
        long size = (long) this.readInt() & 0xffffffffL;
        if (entry != null) {
            entry.setCrc(crc);
            entry.setCompressedSize(csize);
            entry.setSize(size);
        }
    }

    private int readInt() throws java.io.IOException {
        int b0 = readByte();
        int b1 = readByte();
        int b2 = readByte();
        int b3 = readByte();
        int value = 0;
        if (b3 != -1) {
            value = (b0 & 0xff) | ((b1 & 0xff) << 8) | ((b2 & 0xff) << 16) | ((b3 & 0xff) << 24);
        }
        return value;
    }

    private int readShort() throws java.io.IOException {
        int b0 = readByte();
        int b1 = readByte();
        return (b0 & 0xff) | ((b1 & 0xff) << 8);
    }

    // The **bytes** are read and only then decoded. The String used to be built character by
    // character with `(char) (b & 0xff)`, which is Latin-1 in disguise: a UTF-8 name with accents
    // came back split into two characters per letter.
    private String readString(int len) throws java.io.IOException {
        byte[] raw = new byte[len];
        int i = 0;
        while (i < len) {
            raw[i] = (byte) readByte();
            i = i + 1;
        }
        return new String(raw, this.charset);
    }

    private void skipBytes(int count) throws java.io.IOException {
        int i = 0;
        while (i < count) {
            readByte();
            i = i + 1;
        }
    }
}
