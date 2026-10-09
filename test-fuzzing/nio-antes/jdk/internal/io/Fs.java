package jdk.internal.io;

// The seam between Java and the disk: six natives and nothing else.
//
// **Why there are so few.** Everything that can be written in Java is written in Java --
// `File.getParent`, `Scanner`, `Formatter`, the streams-- because there it is read, tested and
// corrected without recompiling the VM. Going down to Rust is the exception, and this list is
// exactly where there is no alternative: a Java program cannot open a file by its own means.
//
// **The file is read or written whole in one go.** There is no open descriptor, nor position, nor
// `close` that can be missing. It is a real limitation --a file of one gigabyte fits in memory
// twice-- and in exchange there is no state that can be left hanging, which is the kind of error
// that is hardest to find in a VM. When real streaming is needed, the door is adding a handle down
// here; nothing that is above has to find out.
//
// **None of them throws.** They return `null`, `false` or zero, and the caller decides which
// exception corresponds: the native has nothing with which to tell "it does not exist" from "I do
// not have permission", and guessing wrong would be worse than saying nothing.
//
// This class is `jdk.internal` and not API: nobody from outside should name it.
public final class Fs {

    private Fs() {
    }

    /** Flag of `stat`: the path exists. */
    public static final int EXISTS = 1;

    /** It is an ordinary file. */
    public static final int IS_FILE = 2;

    /** It is a directory. */
    public static final int IS_DIRECTORY = 4;

    /** It can be read. */
    public static final int CAN_READ = 8;

    /** It can be written. */
    public static final int CAN_WRITE = 16;

    /** The bytes of the file, or `null` if it could not be read. */
    public static native byte[] readAllBytes(String path);

    /** It writes the bytes; `append` decides whether it adds or overwrites. `true` if it could. */
    public static native boolean writeAllBytes(String path, byte[] bytes, boolean append);

    /**
     * The metadata, in the flags above.
     *
     * <p>They go together and not in five calls because they come from **one single** query to the
     * system: asking for them separately would touch the disk five times and --worse-- could give
     * answers from different moments if something changes in between.
     */
    public static native int stat(String path);

    /** The size in bytes, or 0 if it cannot be known. */
    public static native long size(String path);

    /**
     * It deletes a file or an **empty** directory.
     *
     * <p>Empty on purpose: `File.delete()` does not delete recursively, and doing it here would
     * turn a `delete()` on the wrong directory into a loss of data.
     */
    public static native boolean delete(String path);

    /** It creates a directory; `all` decides whether the missing parents are created as well. */
    public static native boolean mkdir(String path, boolean all);

    /**
     * The **simple** names of the entries of a directory, or `null` if it could not be read.
     *
     * <p>It is the native that was missing in order to be able to **walk** the disk and not only
     * touch loose files. With it come in the nine methods of `java.nio.file` that enumerate
     * --`list`, `walk`, `find`, `walkFileTree`, the three `newDirectoryStream`-- and the five
     * `list`/`listFiles` of `java.io.File`, which until now always returned `null`.
     *
     * <p>Simple names and not complete paths, as `File.list()` does: whoever wants the path puts it
     * together with the directory they already have. Returning it ready-made would force the native
     * to choose a separator and to decide whether it normalises, and those two are decisions of the
     * Java side.
     *
     * <p>`null` --and not an empty array-- when it fails, so that it is told apart from a directory
     * that exists and is empty. It is the same distinction `File.list()` makes, and losing it would
     * turn an error into a result.
     *
     * <p>The order is the one the file system gives and it is **not sorted**: the contract says
     * explicitly that there is no guarantee of order.
     */
    public static native String[] list(String path);

    /**
     * The **canonical** path of that route: absolute, resolved and with no links; `null` if it does
     * not exist.
     *
     * <p>It is the only thing that answers whether two different paths name the same file.
     * Comparing the strings is not enough: on Windows `C:\A.TXT` and `C:\a.txt` are the same file.
     */
    public static native String canonical(String path);

    /**
     * The date of last modification, in milliseconds since the epoch; `Long.MIN_VALUE` if it could
     * not be read.
     *
     * <p>The sentinel is not zero on purpose: zero **is** a valid date --the epoch-- and it was the
     * one that was returned when there was nothing with which to read the real one. Confusing "I do
     * not know" with "1 January 1970" is exactly the error this value avoids.
     */
    public static native long mtime(String path);

    /** It sets the date of last modification, in milliseconds since the epoch. */
    public static native boolean setMtime(String path, long millis);

    /**
     * The total size, in bytes, of the volume that contains that path; **-1 if it could not be
     * known**.
     *
     * <p>The sentinel is not zero for the same reason as in {@link #mtime}: zero **is** a valid
     * answer --a volume with no space-- and confusing it with "I do not know" is the error this
     * value avoids. The Java side translates the -1 into the `IOException` `FileStore` declares.
     */
    public static native long diskTotal(String path);

    /**
     * What **this user** can write on that volume, in bytes; -1 if it could not be known.
     *
     * <p>It is not the same as {@link #diskUnallocated}, and the difference matters where there are
     * quotas: the usable is what the quota leaves, the unallocated is what the volume has. With no
     * quota the two coincide.
     */
    public static native long diskUsable(String path);

    /**
     * The unallocated bytes of the volume; -1 if it could not be known. See {@link #diskUsable}.
     */
    public static native long diskUnallocated(String path);

    /**
     * The roots of the file system: `C:\`, `D:\`, ... on Windows; `/` on the rest.
     *
     * <p>The system is asked on each call and it is not kept: a drive that is connected adds a
     * root, and a cached list would be old just when somebody looks at it to see what there is.
     */
    public static native String[] roots();

    /**
     * Takes a system lock over that region of the file.
     *
     * <p>It is a lock **between processes**, which is what a file lock is for: two different
     * virtual machines over the same file exclude each other. It is not a lock between the threads
     * of one machine; `java.util.concurrent` is where those live.
     *
     * <p>The region may run past the end of the file, and it may be open-ended: a `size` of zero
     * means "from `position` to wherever the file grows", which is how the JDK spells a lock over
     * everything still to come.
     *
     * @param path the file
     * @param position the first byte
     * @param size how many bytes, or 0 for however far the file grows
     * @param shared true for a lock other readers may share
     * @param wait true to wait until the region frees up, false to return at once
     * @return the lock token, zero or greater; -1 when the lock could not be taken, and -2 when
     *     this system has no file locks
     */
    public static native int lock(String path, long position, long size, boolean shared,
            boolean wait);

    /**
     * Releases a lock.
     *
     * @param token the identifier {@link #lock} returned
     * @return whether there was a lock to release
     */
    public static native boolean unlock(int token);

    /** {@link #mapOpen} mode: read only. */
    public static final int MAP_READ_ONLY = 0;

    /** {@link #mapOpen} mode: writes reach the file and other mappers. */
    public static final int MAP_READ_WRITE = 1;

    /** {@link #mapOpen} mode: writes stay in this process, copy-on-write. */
    public static final int MAP_PRIVATE = 2;

    /**
     * Maps a region of a file into memory.
     *
     * <p>The region may start anywhere; the system can only begin a mapping at a multiple of its
     * allocation granularity, so the base is rounded down and the difference added back. What comes
     * out addresses exactly the region that was asked for.
     *
     * @param path the file
     * @param mode one of {@link #MAP_READ_ONLY}, {@link #MAP_READ_WRITE}, {@link #MAP_PRIVATE}
     * @param position the first byte of the file to map
     * @param size how many bytes
     * @return the mapping token, zero or greater; -1 when it could not be mapped, -2 when this
     *     platform cannot map at all
     */
    public static native int mapOpen(String path, int mode, long position, int size);

    /**
     * Writes the mapped bytes back to the file and waits for them to land.
     *
     * @param token the mapping
     * @return whether it worked
     */
    public static native boolean mapForce(int token);

    /**
     * Takes the mapping down, flushing a writable one first.
     *
     * @param token the mapping
     * @return whether there was a mapping to close
     */
    public static native boolean mapClose(int token);

    /**
     * One byte of the mapping.
     *
     * @param token the mapping
     * @param index the byte, counted from the start of the mapped region
     * @return the byte, 0 to 255, or -1 if the token or the index is not right
     */
    public static native int mapGet(int token, int index);

    /**
     * Writes one byte of the mapping.
     *
     * @param token the mapping
     * @param index the byte, counted from the start of the mapped region
     * @param value the byte to write; only its low eight bits are used
     * @return whether it was written
     */
    public static native boolean mapPut(int token, int index, int value);

    /**
     * Copies a run of the mapping into an array.
     *
     * <p>It exists so that reading a mapped file is one native call per buffer instead of one per
     * byte, which is the whole cost of {@link #mapGet} in a loop.
     *
     * @param token the mapping
     * @param index the first byte of the mapping to read
     * @param dst where to put them
     * @param off the first element of `dst` to write
     * @param len how many bytes
     * @return whether it worked
     */
    public static native boolean mapRead(int token, int index, byte[] dst, int off, int len);

    /**
     * Copies a run of an array into the mapping.
     *
     * @param token the mapping
     * @param index the first byte of the mapping to write
     * @param src where to take them from
     * @param off the first element of `src` to read
     * @param len how many bytes
     * @return whether it worked
     */
    public static native boolean mapWrite(int token, int index, byte[] src, int off, int len);
}
