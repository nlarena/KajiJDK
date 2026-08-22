package java.util.zip;

import java.io.Closeable;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

// Random access to an archive: list the entries without reading them, then open just the one you
// want. That is the whole difference from `ZipInputStream`, and it rests entirely on being able
// to SEEK — the reader jumps to the end of the file, reads the central directory, and from then
// on knows every entry's offset.
//
// AND THAT IS EXACTLY WHAT IS MISSING HERE. KajiLibrary has no `java.io.File`, no
// `RandomAccessFile`, and no file-descriptor natives; opening a path is a runtime capability the
// library cannot invent. So the constructor fails, deliberately and loudly, rather than
// pretending to hold an archive:
//
//     new ZipFile("x.zip")  ->  UnsupportedOperationException
//
// The remaining methods are reachable only from an instance that cannot exist; they are here so
// the type is complete for anything that compiles against it. This is the same shape as
// `Validation.buildDefaultValidatorFactory` in jakarta.validation, which throws because there is
// no provider: a surface with an honest hole beats a surface that lies.
//
// OMITTED (subset): the five constructors taking `java.io.File` or `java.nio.charset.Charset`,
// neither of which exists here. Only `ZipFile(String)` is expressible.
//
// The parsing this class would need is NOT missing — it lives in `ZipInputStream`, where it can
// run over a stream. When file natives land, this becomes a seek plus that same field decoding.
public class ZipFile implements Closeable {

    public static final int OPEN_READ = 0x1;
    public static final int OPEN_DELETE = 0x4;

    private final String name;
    private final List<ZipEntry> entries;

    public ZipFile(String name) {
        this.name = name;
        this.entries = new ArrayList<ZipEntry>();
        throw new UnsupportedOperationException(
                "ZipFile needs random access to a file, and KajiLibrary has no file I/O yet; "
                        + "use ZipInputStream over a stream instead");
    }

    public String getName() {
        return name;
    }

    public String getComment() {
        return null;
    }

    public ZipEntry getEntry(String entryName) {
        ZipEntry found = null;
        int i = 0;
        while (i < entries.size()) {
            ZipEntry entry = entries.get(i);
            if (entry.getName().equals(entryName)) {
                found = entry;
            }
            i = i + 1;
        }
        return found;
    }

    public InputStream getInputStream(ZipEntry entry) {
        return null;
    }

    public Enumeration<ZipEntry> entries() {
        return new ZipEntryEnumeration(entries);
    }

    public int size() {
        return entries.size();
    }

    public void close() {
    }

    public String toString() {
        return name;
    }
}

// The enumeration over the entry list. Top-level and package-private: a nested type is what
// finding #101 trips over, and the gate skips a class with no JDK counterpart.
class ZipEntryEnumeration implements Enumeration<ZipEntry> {

    private final List<ZipEntry> entries;
    private int at;

    ZipEntryEnumeration(List<ZipEntry> entries) {
        this.entries = entries;
    }

    public boolean hasMoreElements() {
        return at < entries.size();
    }

    public ZipEntry nextElement() {
        ZipEntry entry = entries.get(at);
        at = at + 1;
        return entry;
    }
}
