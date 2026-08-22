package java.util.zip;

import java.time.LocalDateTime;

// One member of a zip archive — its metadata, not its bytes. The same object is used for reading
// (filled in from the archive) and for writing (filled in by the caller), which is why almost
// every field has both a getter and a setter and why "unset" has to be representable: a size of
// -1 means "not known yet", not "empty".
//
// OMITTED (subset): the `FileTime` accessors — `setLastModifiedTime`, `getLastModifiedTime`,
// `setLastAccessTime`, `getLastAccessTime`, `setCreationTime`, `getCreationTime` — because
// `java.nio.file.attribute.FileTime` does not exist here. The `long` and `LocalDateTime` forms
// of the modification time do, and they are the ones a zip actually stores.
//
// `clone()` is also omitted: `Object.clone` is a VM intrinsic KajiLibrary's `Object` does not
// declare. The copy constructor does the same job.
public class ZipEntry implements Cloneable {

    public static final int STORED = 0;
    public static final int DEFLATED = 8;

    private final String name;
    private long time;
    private long crc;
    private long size;
    private long csize;
    private int method;
    private byte[] extra;
    private String comment;

    public ZipEntry(String name) {
        this.name = name;
        this.time = -1;
        this.crc = -1;
        this.size = -1;
        this.csize = -1;
        this.method = -1;
    }

    public ZipEntry(ZipEntry other) {
        this.name = other.name;
        this.time = other.time;
        this.crc = other.crc;
        this.size = other.size;
        this.csize = other.csize;
        this.method = other.method;
        this.extra = other.extra;
        this.comment = other.comment;
    }

    public String getName() {
        return name;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public long getTime() {
        return time;
    }

    // A zip stores the modification time as an MS-DOS date and time: two 16-bit words with the
    // year counted from 1980, and seconds divided by two — which is why a zip timestamp is only
    // ever even. `LocalDateTime` is the honest type for it, because the format records NO time
    // zone at all; the `long` form has to invent one, and that is the older, lossier API.
    public void setTimeLocal(LocalDateTime dateTime) {
        this.time = toDosTime(dateTime);
    }

    public LocalDateTime getTimeLocal() {
        return fromDosTime(this.time);
    }

    static long toDosTime(LocalDateTime dateTime) {
        int year = dateTime.getYear() - 1980;
        long dosDate = ((long) year << 9) | ((long) dateTime.getMonthValue() << 5)
                | (long) dateTime.getDayOfMonth();
        long dosTime = ((long) dateTime.getHour() << 11) | ((long) dateTime.getMinute() << 5)
                | ((long) (dateTime.getSecond() / 2));
        return (dosDate << 16) | dosTime;
    }

    static LocalDateTime fromDosTime(long dos) {
        int date = (int) ((dos >> 16) & 0xffff);
        int time = (int) (dos & 0xffff);
        int year = ((date >> 9) & 0x7f) + 1980;
        int month = (date >> 5) & 0x0f;
        int day = date & 0x1f;
        int hour = (time >> 11) & 0x1f;
        int minute = (time >> 5) & 0x3f;
        int second = (time & 0x1f) * 2;
        return LocalDateTime.of(year, month, day, hour, minute, second);
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getSize() {
        return size;
    }

    public long getCompressedSize() {
        return csize;
    }

    public void setCompressedSize(long csize) {
        this.csize = csize;
    }

    public void setCrc(long crc) {
        this.crc = crc;
    }

    public long getCrc() {
        return crc;
    }

    public void setMethod(int method) {
        this.method = method;
    }

    public int getMethod() {
        return method;
    }

    public void setExtra(byte[] extra) {
        this.extra = extra;
    }

    public byte[] getExtra() {
        return extra;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getComment() {
        return comment;
    }

    // A directory is not a flag in the format — it is a name ending in `/` with no content. The
    // convention IS the representation.
    public boolean isDirectory() {
        boolean dir = false;
        if (name.length() > 0) {
            dir = name.charAt(name.length() - 1) == '/';
        }
        return dir;
    }

    public String toString() {
        return name;
    }

    public int hashCode() {
        return name.hashCode();
    }
}
