package java.util.zip;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

// **Random** access to a compressed archive: listing the entries without reading them, and then
// opening only the one wanted. That is the whole difference from `ZipInputStream`, and it rests
// entirely on being able to **seek**: it goes to the end of the file, reads the central directory,
// and from there knows each entry's offset.
//
// **This used not to be possible and the constructor threw.** The note that was here said "when the
// file intrinsics arrive, this becomes a seek plus the same field decoding". They arrived
// --`jdk.internal.io.Fs`-- and that is exactly what it does now.
//
// The difference from the JDK, and it is this library's usual one: **the file is read whole in one
// go**, there is no open descriptor and no position. A one-gigabyte ZIP fits in memory; in exchange
// there is no state that can be left dangling, and `close()` cannot lose anything because nothing is
// pending. When real streaming is needed, the door is adding a handle in `Fs`: the methods here talk
// to a `byte[]`, not to the intrinsic, so they never find out.
//
// The format, in the part that matters: at the end of the file there is an **EOCD** record saying
// how many entries there are and where the central directory starts; the directory is a list of
// headers, one per entry, each with the offset of its **local** header; and the local header says
// how long the name and the extra field are, which is all that is needed to know where the data
// begins.
//
// The EOCD is searched for **from the end backwards** because there may be an archive comment after
// it, of variable length: there is no way of knowing where it starts without looking for its
// signature.
//
// **How an error is signalled**: the JDK declares `throws IOException` on the constructors and on
// `getInputStream`. Here it is wrapped in an `UncheckedIOException`, which is the convention the
// library already set in `FileInputStream`/`FileOutputStream`: this tree's `java.io` bases were
// written with no `throws`, and an override cannot widen the checked exceptions (JLS 8.4.8.3). The
// reason is not lost -- the original `ZipException` goes inside.
public class ZipFile implements Closeable {

    public static final int OPEN_READ = 0x1;
    public static final int OPEN_DELETE = 0x4;

    private static final int EOCD_SIG = 0x06054b50;
    private static final int CEN_SIG = 0x02014b50;
    private static final int LOC_SIG = 0x04034b50;

    private final String name;
    private final Charset charset;
    private final byte[] data;
    private final List<ZipEntry> entries;
    // The offset of each entry's **local** header, parallel to `entries`.
    private final List<Long> offsets;
    private boolean closedFlag;

    /** It opens the file by that name, with the entry names in UTF-8. */
    public ZipFile(String name) {
        this(name, StandardCharsets.UTF_8);
    }

    /** It opens the file by that name, decoding the names with `charset`. */
    public ZipFile(String name, Charset charset) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        if (charset == null) {
            throw new NullPointerException("charset");
        }
        this.name = name;
        this.charset = charset;
        byte[] readSoFar = jdk.internal.io.Fs.readAllBytes(name);
        if (readSoFar == null) {
            throw new java.io.UncheckedIOException(
                    new java.io.FileNotFoundException(name + " (it could not be read)"));
        }
        this.data = readSoFar;
        this.entries = new ArrayList<ZipEntry>();
        this.offsets = new ArrayList<Long>();
        this.readDirectory();
    }

    /** It opens that file. */
    public ZipFile(File file) {
        this(file.getPath(), StandardCharsets.UTF_8);
    }

    /** It opens that file with that charset. */
    public ZipFile(File file, Charset charset) {
        this(file.getPath(), charset);
    }

    /**
     * It opens that file with those modes.
     *
     * <p>`OPEN_DELETE` is accepted and **not honoured**: it asks for the file to be deleted on
     * closing, and since here it is read whole on opening, deleting it afterwards would change
     * nothing of what was already read -- but it would delete a file the caller may still want. Not
     * deleting, and saying so, is preferred.
     *
     * @throws IllegalArgumentException if `mode` is not a combination of `OPEN_READ`/`OPEN_DELETE`
     */
    public ZipFile(File file, int mode) {
        this(file, mode, StandardCharsets.UTF_8);
    }

    /** The one above, with a charset. */
    public ZipFile(File file, int mode, Charset charset) {
        this(checkMode(file, mode).getPath(), charset);
    }

    private static File checkMode(File file, int mode) {
        if ((mode & ~(OPEN_READ | OPEN_DELETE)) != 0 || (mode & OPEN_READ) == 0) {
            throw new IllegalArgumentException("Illegal mode: 0x" + Integer.toHexString(mode));
        }
        return file;
    }

    // ---- reading the format -----------------------------------------------------------------------

    private int u16(int at) {
        return (this.data[at] & 0xff) | ((this.data[at + 1] & 0xff) << 8);
    }

    private int u32(int at) {
        return (this.data[at] & 0xff) | ((this.data[at + 1] & 0xff) << 8)
                | ((this.data[at + 2] & 0xff) << 16) | ((this.data[at + 3] & 0xff) << 24);
    }

    private long u32Unsigned(int at) {
        return (long) this.u32(at) & 0xffffffffL;
    }

    private void readDirectory() {
        int eocd = this.findEocd();
        if (eocd < 0) {
            throw new java.io.UncheckedIOException(
                    new ZipException("not a ZIP file: the end-of-central-directory record was not found"));
        }
        int howMany = this.u16(eocd + 10);
        int cenStart = (int) this.u32Unsigned(eocd + 16);
        int at = cenStart;
        int i = 0;
        while (i < howMany && at + 46 <= this.data.length) {
            if (this.u32(at) != CEN_SIG) {
                throw new java.io.UncheckedIOException(
                        new ZipException("directorio central corrupto en " + at));
            }
            int methodOf = this.u16(at + 10);
            int dosTime = this.u32(at + 12);
            long crc = this.u32Unsigned(at + 16);
            long csize = this.u32Unsigned(at + 20);
            long size = this.u32Unsigned(at + 24);
            int nameLength = this.u16(at + 28);
            int extraLength = this.u16(at + 30);
            int commentLength = this.u16(at + 32);
            long localOffset = this.u32Unsigned(at + 42);

            byte[] rawName = new byte[nameLength];
            System.arraycopy(this.data, at + 46, rawName, 0, nameLength);
            ZipEntry entryOf = new ZipEntry(new String(rawName, this.charset));
            entryOf.setMethod(methodOf);
            entryOf.setTime(dosTime);
            entryOf.setCrc(crc);
            entryOf.setCompressedSize(csize);
            entryOf.setSize(size);
            if (commentLength > 0) {
                byte[] c = new byte[commentLength];
                System.arraycopy(this.data, at + 46 + nameLength + extraLength, c, 0,
                        commentLength);
                entryOf.setComment(new String(c, this.charset));
            }
            this.entries.add(entryOf);
            this.offsets.add(Long.valueOf(localOffset));
            at = at + 46 + nameLength + extraLength + commentLength;
            i = i + 1;
        }
    }

    // It is searched for **back to front** because after the EOCD there may be an archive comment of
    // variable length, and there is no way of knowing where it starts without looking for its
    // signature. The comment is at most 65535 long, so there is no need to look further back.
    private int findEocd() {
        int min = this.data.length - 22 - 65535;
        if (min < 0) {
            min = 0;
        }
        int at = this.data.length - 22;
        while (at >= min) {
            if (this.u32(at) == EOCD_SIG) {
                return at;
            }
            at = at - 1;
        }
        return -1;
    }

    // ---- the public surface -----------------------------------------------------------------------

    public String getName() {
        return this.name;
    }

    /** The archive's comment, or `null` if it has none. */
    public String getComment() {
        this.checkOpen();
        int eocd = this.findEocd();
        if (eocd < 0) {
            return null;
        }
        int length = this.u16(eocd + 20);
        if (length == 0) {
            return null;
        }
        byte[] c = new byte[length];
        System.arraycopy(this.data, eocd + 22, c, 0, length);
        return new String(c, this.charset);
    }

    public ZipEntry getEntry(String entryName) {
        this.checkOpen();
        int i = this.indexFor(entryName);
        if (i < 0) {
            return null;
        }
        return this.entries.get(i);
    }

    private int indexFor(String entryName) {
        int i = 0;
        while (i < this.entries.size()) {
            if (this.entries.get(i).getName().equals(entryName)) {
                return i;
            }
            i = i + 1;
        }
        return -1;
    }

    /**
     * A stream with that entry's **decompressed** content.
     *
     * <p>The seek to the data needs the **local** header and not the directory's: the name's and the
     * extra field's lengths can differ between the two, and the one that counts for knowing where
     * the bytes start is the local one's.
     *
     * @throws NullPointerException if `entry` is `null`
     */
    public InputStream getInputStream(ZipEntry entry) throws java.io.IOException {
        this.checkOpen();
        if (entry == null) {
            throw new NullPointerException("entry");
        }
        int i = this.indexFor(entry.getName());
        if (i < 0) {
            return null;
        }
        int local = (int) this.offsets.get(i).longValue();
        if (this.u32(local) != LOC_SIG) {
            throw new java.io.UncheckedIOException(
                    new ZipException("cabecera local corrupta en " + local));
        }
        int nameLength = this.u16(local + 26);
        int extraLength = this.u16(local + 28);
        int dataAt = local + 30 + nameLength + extraLength;
        ZipEntry e = this.entries.get(i);
        int compressed = (int) e.getCompressedSize();
        byte[] rawName = new byte[compressed];
        System.arraycopy(this.data, dataAt, rawName, 0, compressed);
        if (e.getMethod() == ZipEntry.STORED) {
            return new ByteArrayInputStream(rawName);
        }
        return new InflaterInputStream(new ByteArrayInputStream(rawName), new Inflater(true));
    }

    public Enumeration<ZipEntry> entries() {
        this.checkOpen();
        return new ZipEntryEnumeration(this.entries);
    }

    /** The entries as a stream. It is the modern form of `entries()`. */
    public java.util.stream.Stream<ZipEntry> stream() {
        this.checkOpen();
        return this.entries.stream();
    }

    public int size() {
        this.checkOpen();
        return this.entries.size();
    }

    /**
     * It closes the archive.
     *
     * <p>There is nothing to release --the content is already in memory-- but the object is left
     * **closed**, and using it afterwards fails. That is not ceremony: it is what makes code written
     * against this class behave the same the day there is a real descriptor.
     */
    public void close() throws java.io.IOException {
        this.closedFlag = true;
    }

    private void checkOpen() {
        if (this.closedFlag) {
            throw new IllegalStateException("zip file closed");
        }
    }

    public String toString() {
        return this.name;
    }
}

// The enumeration over the entry list. Top-level and package-private: a nested type is what
// finding #101 trips over, and the gate skips a class with no JDK counterpart.
class ZipEntryEnumeration implements Enumeration<ZipEntry> {

    private final List<ZipEntry> entries;
    private int at;

    ZipEntryEnumeration(List<ZipEntry> entries) {
        this.entries = entries;
        this.at = 0;
    }

    public boolean hasMoreElements() {
        return this.at < this.entries.size();
    }

    public ZipEntry nextElement() {
        if (this.at >= this.entries.size()) {
            throw new java.util.NoSuchElementException();
        }
        ZipEntry e = this.entries.get(this.at);
        this.at = this.at + 1;
        return e;
    }
}
