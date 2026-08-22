package java.util.zip;

import java.io.InputStream;

// Reads the ZIP container as a STREAM: entry after entry, front to back, without ever seeking.
// That constraint is what separates it from `ZipFile`, and it has a real consequence — a
// streaming reader cannot use the central directory at the end of the archive, so it has to
// trust each entry's local header, and when the local header says "sizes unknown" (flag bit 3)
// it must find the end of the data by other means.
//
// Here that means an entry written with a data descriptor can only be read when its size was
// also recorded in the local header. Archives produced by our own `ZipOutputStream` always use
// descriptors, so round-tripping through this class is limited to STORED entries with known
// sizes; the general case needs the deflate stream itself to signal its end, which our resumable
// inflater can report but the plumbing here does not yet use.
//
// The `throws IOException` clauses are omitted throughout (finding #104).
public class ZipInputStream extends InflaterInputStream {

    private static final int LOCAL_SIG = 0x04034b50;

    private ZipEntry current;
    private long remaining;
    private boolean entryEof;

    public ZipInputStream(InputStream in) {
        super(in, new Inflater(true));
    }

    // Advances to the next entry and returns its metadata, or null at the end of the archive.
    public ZipEntry getNextEntry() {
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
            } else {
                // Sizes live in a descriptor after the data — see the class comment.
                remaining = -1;
            }
            current = entry;
            entryEof = false;
        }
        return entry;
    }

    public void closeEntry() {
        if (current != null) {
            if (remaining > 0) {
                skipBytes((int) remaining);
            }
            current = null;
            remaining = 0;
            entryEof = true;
        }
    }

    public int available() {
        int n = 1;
        if (entryEof || current == null) {
            n = 0;
        }
        return n;
    }

    public int read() {
        byte[] one = new byte[1];
        int n = read(one, 0, 1);
        int result = -1;
        if (n == 1) {
            result = one[0] & 0xff;
        }
        return result;
    }

    public int read(byte[] b, int off, int len) {
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

    public long skip(long n) {
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

    // ---- lectura de campos, little-endian como todo el formato ----

    private int readInt() {
        int b0 = in.read();
        int b1 = in.read();
        int b2 = in.read();
        int b3 = in.read();
        int value = 0;
        if (b3 != -1) {
            value = (b0 & 0xff) | ((b1 & 0xff) << 8) | ((b2 & 0xff) << 16) | ((b3 & 0xff) << 24);
        }
        return value;
    }

    private int readShort() {
        int b0 = in.read();
        int b1 = in.read();
        return (b0 & 0xff) | ((b1 & 0xff) << 8);
    }

    private String readString(int len) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < len) {
            sb.append((char) (in.read() & 0xff));
            i = i + 1;
        }
        return sb.toString();
    }

    private void skipBytes(int count) {
        int i = 0;
        while (i < count) {
            in.read();
            i = i + 1;
        }
    }
}
