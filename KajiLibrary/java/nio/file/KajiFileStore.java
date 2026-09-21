package java.nio.file;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;

/**
 * The {@link FileStore} of a volume of the default filesystem.
 *
 * <p>It exists now that `jdk.internal.io.Fs` knows how to ask about a volume's space. Before it did
 * not: the eight members that ask for the volume's data would have had to return `""` and `0`, and
 * a zero in `getUsableSpace()` is not "I do not know" but "nothing fits" -- a concrete and false
 * answer, of the kind that makes a program decide not to write.
 *
 * <h2>What is known and what is not</h2>
 *
 * <p>The three spaces are real, and there are **three** and not two: usable is what this user can
 * write and unallocated is what the volume has left. With a quota in place they differ.
 *
 * <p>{@link #type} returns `"unknown"`. **It is not a filler**: the native asks about space, not
 * about the filesystem's type, and returning `"ntfs"` because we are on Windows would be guessing
 * -- a network-mounted volume or a FAT drive would answer the same and would be wrong. The string
 * `"unknown"` is the one the JDK itself uses when it cannot determine one, so it invents no new
 * format.
 *
 * <p>{@link #isReadOnly} returns `false`. It is an **honest lower bound**: there is nothing to ask
 * it with, and saying `true` would make a program refuse to write where it can. An attempt to write
 * on a read-only volume fails all the same, with its `IOException`, which is where the truth turns
 * up anyway.
 */
final class KajiFileStore extends FileStore {

    private final String pathOf;
    private final String storeName;

    KajiFileStore(String pathOf, String storeName) {
        this.pathOf = pathOf;
        this.storeName = storeName;
    }

    public String name() {
        return this.storeName;
    }

    /**
     * The name of the volume that contains that absolute path.
     *
     * <p>On Windows it is the letter with its colon (`C:`); on any other system, the root (`/`). It
     * is not the name the user gave the volume --that cannot be asked-- but the one that identifies
     * it, which is what `name()` promises: "its form is system dependent; it may not be unique".
     */
    static String volumeName(String pathOf) {
        if (pathOf.length() >= 2 && pathOf.charAt(1) == ':') {
            return pathOf.substring(0, 2);
        }
        return "/";
    }

    /** Always `"unknown"`. See the class's note on why nothing is guessed. */
    public String type() {
        return "unknown";
    }

    /** Always `false`. See the class's note. */
    public boolean isReadOnly() {
        return false;
    }

    /**
     * The volume's total size.
     *
     * @throws IOException if it could not be found out
     */
    public long getTotalSpace() throws IOException {
        return KajiFileStore.require(jdk.internal.io.Fs.diskTotal(this.pathOf), "total");
    }

    /**
     * What this user can write.
     *
     * @throws IOException if it could not be found out
     */
    public long getUsableSpace() throws IOException {
        return KajiFileStore.require(jdk.internal.io.Fs.diskUsable(this.pathOf), "utilizable");
    }

    /**
     * The volume's unallocated bytes.
     *
     * @throws IOException if it could not be found out
     */
    public long getUnallocatedSpace() throws IOException {
        return KajiFileStore.require(jdk.internal.io.Fs.diskUnallocated(this.pathOf), "unallocated");
    }

    // The native's -1 means "could not", not a size. Turning it into the exception the signature
    // declares is the only thing that lets the caller tell the two apart.
    private static long require(long v, String which) throws IOException {
        if (v < 0L) {
            throw new IOException("could not read the " + which + " space of the volume");
        }
        return v;
    }

    /**
     * Only {@link BasicFileAttributeView}.
     *
     * <p>It is the only view this filesystem implements, and answering for the others would be
     * promising attributes that afterwards cannot be read.
     */
    public boolean supportsFileAttributeView(Class<? extends FileAttributeView> type) {
        return type == BasicFileAttributeView.class;
    }

    /** Only `"basic"`. See the other form. */
    public boolean supportsFileAttributeView(String name) {
        return "basic".equals(name);
    }

    /**
     * Always `null`.
     *
     * <p>A view of the **volume's** attributes --not a file's-- is what this method returns, and
     * there is none: `Fs` knows about space and nothing else. `null` is what the contract defines
     * for "not supported", so saying it that way loses nothing.
     */
    public <V extends FileStoreAttributeView> V getFileStoreAttributeView(Class<V> type) {
        if (type == null) {
            throw new NullPointerException("type");
        }
        return null;
    }

    /**
     * The three spaces, by name.
     *
     * <p>The names are the ones the JDK defines (`totalSpace`, `usableSpace`, `unallocatedSpace`),
     * and any other is `UnsupportedOperationException` -- not `null`. The difference matters:
     * `null` would be "that attribute is worth nothing" and what happens is that that attribute
     * does not exist.
     *
     * <p>The exception is `UnsupportedOperationException` and not `IllegalArgumentException`
     * because it is what the JDK answers: checked by running the same case with the real `java`,
     * which throws `UnsupportedOperationException: 'x' not recognized`. The wrong expectation was
     * mine.
     *
     * @throws IOException if it could not be found out
     * @throws UnsupportedOperationException if the attribute is not one of the three
     */
    public Object getAttribute(String attribute) throws IOException {
        if ("totalSpace".equals(attribute)) {
            return Long.valueOf(this.getTotalSpace());
        }
        if ("usableSpace".equals(attribute)) {
            return Long.valueOf(this.getUsableSpace());
        }
        if ("unallocatedSpace".equals(attribute)) {
            return Long.valueOf(this.getUnallocatedSpace());
        }
        throw new UnsupportedOperationException("'" + attribute + "' not recognized");
    }

    public String toString() {
        return this.storeName;
    }
}
