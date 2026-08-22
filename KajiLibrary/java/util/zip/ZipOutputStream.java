package java.util.zip;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

// Writes the ZIP container format. Worth separating from everything else in this package: DEFLATE
// is a byte stream with no notion of files, and ZIP is the archive format that carries many
// deflated streams plus an index. They are routinely conflated because one file extension covers
// both.
//
// The shape of the format is a consequence of it being designed for tape and floppies. Each entry
// is written with a LOCAL HEADER in front of its data, and the whole archive ends with a CENTRAL
// DIRECTORY repeating every entry's metadata plus the offset of its local header. That
// duplication is the point: a reader can list an archive by seeking to the end and reading the
// directory, without touching the entries; a writer that cannot seek can still stream the
// entries out and append the directory last. It is also why a truncated zip is often still
// partially recoverable.
//
// The `throws IOException` clauses are omitted throughout (finding #104).
public class ZipOutputStream extends DeflaterOutputStream {

    public static final int STORED = ZipEntry.STORED;
    public static final int DEFLATED = ZipEntry.DEFLATED;

    private static final int LOCAL_SIG = 0x04034b50;
    private static final int CENTRAL_SIG = 0x02014b50;
    private static final int END_SIG = 0x06054b50;
    private static final int DESCRIPTOR_SIG = 0x08074b50;
    // Bit 3 of the general-purpose flags: "the sizes and CRC are not in the local header, look
    // for a data descriptor after the data". It is what makes streaming possible at all.
    private static final int FLAG_DESCRIPTOR = 8;

    private final List<ZipEntry> written;
    private final List<Long> offsets;
    private final CRC32 crc;

    private String comment;
    private int method;
    private ZipEntry current;
    private long position;
    private long entryStart;
    private long entrySize;
    private boolean finished;

    public ZipOutputStream(OutputStream out) {
        super(out, new Deflater(Deflater.DEFAULT_COMPRESSION, true));
        this.written = new ArrayList<ZipEntry>();
        this.offsets = new ArrayList<Long>();
        this.crc = new CRC32();
        this.method = DEFLATED;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setMethod(int method) {
        this.method = method;
    }

    public void setLevel(int level) {
        def.setLevel(level);
    }

    public void putNextEntry(ZipEntry entry) {
        if (current != null) {
            closeEntry();
        }
        current = entry;
        if (entry.getMethod() == -1) {
            entry.setMethod(method);
        }
        crc.reset();
        entrySize = 0;
        entryStart = position;
        writeLocalHeader(entry);
    }

    public void closeEntry() {
        if (current != null) {
            if (current.getMethod() == DEFLATED) {
                // Flush the compressed stream for this entry, then start a fresh deflater: each
                // entry is an independent deflate stream inside the archive.
                //
                // El delta de `getBytesWritten` alrededor del flush NO es opcional: `deflate()`
                // escribe directo a `out` sin pasar por los `write*` de esta clase, asi que sin
                // esto los bytes del bloque final quedan fuera de `position` y el tamano
                // comprimido del descriptor sale corto. El lector del JDK lo caza al instante
                // ("invalid entry compressed size, expected 15 but got 20"); el gate jamas.
                long before = def.getBytesWritten();
                def.finish();
                deflate();
                position = position + (def.getBytesWritten() - before);
                def.reset();
            }
            current.setCrc(crc.getValue());
            current.setSize(entrySize);
            current.setCompressedSize(position - entryStart - localHeaderSize(current));
            writeDataDescriptor(current);
            written.add(current);
            offsets.add(Long.valueOf(entryStart));
            current = null;
        }
    }

    public void write(byte[] b, int off, int len) {
        if (current.getMethod() == STORED) {
            // A stored entry goes out untouched — no deflater in the path at all.
            out.write(b, off, len);
            position = position + (long) len;
        } else {
            long before = def.getBytesWritten();
            def.setInput(b, off, len);
            deflate();
            position = position + (def.getBytesWritten() - before);
        }
        crc.update(b, off, len);
        entrySize = entrySize + (long) len;
    }

    public void finish() {
        if (!finished) {
            finished = true;
            if (current != null) {
                closeEntry();
            }
            long directoryStart = position;
            int i = 0;
            while (i < written.size()) {
                writeCentralEntry(written.get(i), offsets.get(i).longValue());
                i = i + 1;
            }
            writeEnd(directoryStart, position - directoryStart);
        }
    }

    public void close() {
        finish();
        out.close();
    }

    // ---- el formato, campo por campo ----

    private int localHeaderSize(ZipEntry entry) {
        return 30 + entry.getName().length();
    }

    private void writeLocalHeader(ZipEntry entry) {
        writeInt(LOCAL_SIG);
        writeShort(20);                    // version needed: 2.0, which is where deflate appears
        writeShort(FLAG_DESCRIPTOR);       // sizes unknown until the data has been written
        writeShort(entry.getMethod());
        writeInt((int) dosTimeOf(entry));
        writeInt(0);                       // crc, filled in by the data descriptor
        writeInt(0);                       // compressed size, idem
        writeInt(0);                       // uncompressed size, idem
        writeShort(entry.getName().length());
        writeShort(0);                     // no extra field
        writeName(entry.getName());
    }

    private void writeDataDescriptor(ZipEntry entry) {
        writeInt(DESCRIPTOR_SIG);
        writeInt((int) entry.getCrc());
        writeInt((int) entry.getCompressedSize());
        writeInt((int) entry.getSize());
    }

    private void writeCentralEntry(ZipEntry entry, long offset) {
        writeInt(CENTRAL_SIG);
        writeShort(20);                    // version made by
        writeShort(20);                    // version needed
        writeShort(FLAG_DESCRIPTOR);
        writeShort(entry.getMethod());
        writeInt((int) dosTimeOf(entry));
        writeInt((int) entry.getCrc());
        writeInt((int) entry.getCompressedSize());
        writeInt((int) entry.getSize());
        writeShort(entry.getName().length());
        writeShort(0);                     // extra length
        writeShort(0);                     // comment length
        writeShort(0);                     // disk number
        writeShort(0);                     // internal attributes
        writeInt(0);                       // external attributes
        writeInt((int) offset);            // where the local header lives
        writeName(entry.getName());
    }

    private void writeEnd(long directoryStart, long directorySize) {
        writeInt(END_SIG);
        writeShort(0);                     // this disk
        writeShort(0);                     // disk with the directory
        writeShort(written.size());        // entries on this disk
        writeShort(written.size());        // entries total
        writeInt((int) directorySize);
        writeInt((int) directoryStart);
        int commentLen = 0;
        if (comment != null) {
            commentLen = comment.length();
        }
        writeShort(commentLen);
        if (comment != null) {
            writeName(comment);
        }
    }

    private long dosTimeOf(ZipEntry entry) {
        long dos = 0;
        if (entry.getTime() != -1) {
            dos = entry.getTime();
        }
        return dos;
    }

    // Everything in the zip format is LITTLE-endian — the opposite of the class file, and of
    // gzip's trailer being little-endian while zlib's is big-endian. Mixing them up produces
    // fields that look plausible and are wrong.
    private void writeInt(int value) {
        out.write(value & 0xff);
        out.write((value >> 8) & 0xff);
        out.write((value >> 16) & 0xff);
        out.write((value >> 24) & 0xff);
        position = position + 4;
    }

    private void writeShort(int value) {
        out.write(value & 0xff);
        out.write((value >> 8) & 0xff);
        position = position + 2;
    }

    private void writeName(String s) {
        int i = 0;
        while (i < s.length()) {
            out.write(s.charAt(i) & 0xff);
            i = i + 1;
        }
        position = position + (long) s.length();
    }
}
