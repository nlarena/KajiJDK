package java.nio.file;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.spi.FileTypeDetector;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Stream;

import jdk.internal.io.Fs;

// KajiLibrary's java.nio.file.Files -- NIO.2's file operations.
//
// ============================================================================================
// **What is here and what is not, and why.** This is the class where the VM's ceiling shows most,
// so it is worth saying up front. All disk access goes through `jdk.internal.io.Fs`: read the whole
// file, write it whole (overwriting or appending), `stat` (exists / is a file / is a directory /
// readable / writable), size, delete, create a directory, list a directory, read and write the
// modification time, canonicalise a path, and the three disk-space figures.
//
// **All 70 of the JDK's methods are declared.** This note used to say 58 of them were, listing as
// absent the nine that enumerate a directory --`newDirectoryStream` (x3), `list`, `walk` (x2),
// `find`, `walkFileTree` (x2)-- plus `setLastModifiedTime`, `getFileStore` and `isSameFile`. Every
// one of those came back as its native arrived: `Fs.list`, `Fs.setMtime`, the three `disk*` and
// `Fs.canonical`. The count did not come back with them -- which is what happens to a note about
// what is missing once what is missing stops missing.
//
// `isSameFile` is worth a line of its own, because the old argument for leaving it out was a good
// one: comparing normalised paths would give `false` for two names of the same file, and there
// `false` is an assertion and not an "I do not know". It is not an edge case either -- on Windows
// `C:\A.TXT` and `C:\a.txt` are the same file and are not the same string, and since `user.dir` is
// `null` in this VM a relative path cannot be made absolute to compare against one that is. What
// was missing was identity, and `Fs.canonical` gives it: canonicalising resolves case, `.`/`..`,
// links and relatives against the current directory. Two paths are the same file if and only if
// they canonicalise the same.
//
// **And a note on the methods that are here and only know how to fail.** `createSymbolicLink`,
// `createLink`, `readSymbolicLink`, `getOwner`, `setOwner`, `getPosixFilePermissions`,
// `setPosixFilePermissions` and `setAttribute` exist and throw `UnsupportedOperationException`.
// **It is not a hole papered over**: in all eight cases the spec declares that exception for
// exactly this situation --"the implementation does not support symbolic links", "the attribute
// view is not available"-- so throwing it is *answering* the contract, not dodging it. A program
// that calls them gets the same exception it would get from a JDK over a filesystem that does not
// support them either.
//
// **And one on `isHidden`, which is here but does not answer what a Windows JDK would answer.** The
// spec leaves the definition of "hidden" to the provider, and KajiJDK's is POSIX's --the name
// starts with a dot-- which is also the one `java.io.File.isHidden()` already uses in this library.
// It does not come from the DOS bit because `stat` does not bring it. It is written out in the
// method with the two concrete differences it produces.
//
// **The attributes that are read.** `readAttributes` (x2), `getAttribute` and `getLastModifiedTime`
// work, over the `basic` view and nothing else. `stat`, `size` and `mtime` answer five of the nine
// attributes (`isRegularFile`, `isDirectory`, `isOther`, `size`, `lastModifiedTime`); the other two
// timestamps come out as the **epoch**, which is what `BasicFileAttributes`'s spec requires be
// returned when the filesystem does not support them -- not an invented zero; `fileKey()` gives
// `null`, also by spec; and the ninth, `isSymbolicLink()`, gives `false`. See `BasicAttrs`,
// below, where the detail is attribute by attribute.
//
// **Mind the asymmetry, which is real.** The attributes are **read** and yet
// `getFileAttributeView` returns `null` and `FileSystem.supportedFileAttributeViews()` gives empty.
// It is not a contradiction: a *view* is an object that reads **and writes**, and
// `BasicFileAttributeView` has `setTimes`, which sets all three timestamps at once and for which
// there is no native -- `Fs.setMtime` writes only the modification one.
//
// **And two behavioural differences to keep in mind in what is here.**
//
// The first: none of the creation operations is **atomic**. `createFile` is a `stat` followed by a
// write, and between the two somebody could create the file. The JDK does it in a single system
// call. Each affected method says so in its javadoc.
//
// The second: **not every opening method accepts the same `OpenOption`s**, and the difference is on
// purpose. This class's rule is a single one --an option that can be honoured is accepted, and one
// that could only be faked is rejected-- but it gives different results depending on what each
// method is built on, because they do not all have the same capabilities underneath:
//
//   - `newInputStream`, `newOutputStream`, `newBufferedReader` and `newBufferedWriter` go to
//     `java.io.FileInputStream` / `FileOutputStream`, which **accumulate and flush on close**.
//     There `SYNC` and `DSYNC` cannot be honoured, and `DELETE_ON_CLOSE` is not implemented: all
//     three are rejected, which is what `resolveOptions` does.
//   - `newByteChannel` (x2) goes to `java.nio.channels.FileChannel`, which **writes to disk on
//     every `write`**. There `SYNC` and `DSYNC` are already honoured by doing nothing and
//     `DELETE_ON_CLOSE` is implemented, so all three are accepted. `newByteChannel` does not go
//     through `resolveOptions`: it delegates the whole resolution to `FileChannel.open`.
//
// `SPARSE` is rejected on both sides, because sparse files are made on neither. Uniforming towards
// the strict criterion would mean rejecting in `newByteChannel` options that **are** honoured
// there, and uniforming towards the lax one would mean accepting in `newOutputStream` a `SYNC` that
// does not synchronise, which is exactly the false promise this library does not make.
// ============================================================================================
public final class Files {

    // It is a utility class: there is nothing to instantiate.
    private Files() {
    }

    // The generator of temporary names. Seeded from the high-resolution clock: it does not have to
    // be cryptographic --the JDK uses `SecureRandom` because a guessable name is an attack vector,
    // and that changes nothing here because the creation is not atomic anyway-- but two VMs
    // starting together must not generate the same sequence.
    private static final Random RANDOM = new Random(System.nanoTime());

    // The path as a string, checking the Path is this library's.
    private static String pathOf(Path path) {
        if (path == null) {
            throw new NullPointerException();
        }
        if (!(path instanceof KajiPath)) {
            throw new ProviderMismatchException(path.getClass().getName());
        }
        return path.toString();
    }

    // The opening options, already resolved. The array is walked once and fields are consulted
    // afterwards: walking it on every question would be O(n) per query and --worse-- would leave
    // the validation scattered.
    //
    // This holds for the methods that open a stream, **not** for `newByteChannel`: see the header's
    // note on why the two option criteria differ and which governs in each case.
    private static final class Opening {
        boolean readFrom;
        boolean writeTo;
        boolean appending;
        boolean creating;
        boolean createNew;
        boolean truncate;
    }

    private static Opening resolveOptions(OpenOption[] options, boolean forWriting) {
        Opening a = new Opening();
        int i = 0;
        while (i < options.length) {
            OpenOption o = options[i];
            if (o == null) {
                throw new NullPointerException();
            }
            if (o == StandardOpenOption.READ) {
                a.readFrom = true;
            } else if (o == StandardOpenOption.WRITE) {
                a.writeTo = true;
            } else if (o == StandardOpenOption.APPEND) {
                a.appending = true;
            } else if (o == StandardOpenOption.TRUNCATE_EXISTING) {
                a.truncate = true;
            } else if (o == StandardOpenOption.CREATE) {
                a.creating = true;
            } else if (o == StandardOpenOption.CREATE_NEW) {
                a.createNew = true;
            } else if (o == LinkOption.NOFOLLOW_LINKS) {
                // With no links in the model, not following them is the only thing that can be
                // done: accepting it promises nothing that is not honoured, and changes no flag.
            } else {
                // DELETE_ON_CLOSE, SPARSE, SYNC, DSYNC and any foreign OpenOption. They are
                // rejected rather than ignored: a `SYNC` that does not synchronise is the kind of
                // false promise that loses data.
                throw new UnsupportedOperationException(String.valueOf(o) + " not supported");
            }
            i = i + 1;
        }
        if (a.appending && a.readFrom) {
            throw new IllegalArgumentException("READ + APPEND not allowed");
        }
        if (a.appending && a.truncate) {
            throw new IllegalArgumentException("APPEND + TRUNCATE_EXISTING not allowed");
        }
        if (forWriting) {
            // By default: create, truncate and write, which is what the spec says when no option is
            // passed.
            if (!a.writeTo && !a.appending) {
                a.writeTo = true;
                if (options.length == 0) {
                    a.creating = true;
                    a.truncate = true;
                }
            }
        } else if (!a.readFrom && options.length == 0) {
            a.readFrom = true;
        }
        return a;
    }

    // It turns `stat`'s result into the exception that fits when it could not be read.
    private static IOException whyItWasNotRead(String p) {
        int st = Fs.stat(p);
        if ((st & Fs.EXISTS) == 0) {
            return new NoSuchFileException(p);
        }
        if ((st & Fs.IS_DIRECTORY) != 0) {
            return new IOException(p + " is a directory");
        }
        return new AccessDeniedException(p);
    }

    // ------------------------------------------------------------------------------------------
    // Opening
    // ------------------------------------------------------------------------------------------

    /**
     * A stream of bytes over `path`.
     *
     * <p>**The file is read whole on opening**, as everything in this VM is: what it returns is a
     * stream over a copy in memory, not a live window onto the file. See
     * `java.io.FileInputStream`'s note, which is what is underneath.
     *
     * @throws UnsupportedOperationException if an option this VM cannot honour is asked for
     * @throws NoSuchFileException if the file is not there
     */
    public static InputStream newInputStream(Path path, OpenOption... options) throws IOException {
        String p = pathOf(path);
        Opening a = resolveOptions(options, false);
        if (a.writeTo || a.appending || a.creating || a.createNew || a.truncate) {
            throw new UnsupportedOperationException("write options on newInputStream");
        }
        // `stat` is asked before opening so as to tell "does not exist" from "I have no
        // permission": `FileInputStream` can only say `FileNotFoundException`, which lumps the
        // two together.
        int st = Fs.stat(p);
        if ((st & Fs.EXISTS) == 0) {
            throw new NoSuchFileException(p);
        }
        if ((st & Fs.IS_DIRECTORY) != 0) {
            throw new IOException(p + " is a directory");
        }
        if ((st & Fs.CAN_READ) == 0) {
            throw new AccessDeniedException(p);
        }
        return new FileInputStream(p);
    }

    /**
     * A stream for writing into `path`.
     *
     * <p>With no options it is equivalent to `CREATE`, `TRUNCATE_EXISTING` and `WRITE`.
     *
     * <p>**The content reaches the disk only on closing**, not as it is written: underneath is
     * `java.io.FileOutputStream`, which accumulates and flushes in one go because the native writes
     * the whole file. A program that dies without closing the stream leaves nothing written.
     *
     * @throws FileAlreadyExistsException with `CREATE_NEW` if the file was already there
     * @throws NoSuchFileException with neither `CREATE` nor `CREATE_NEW` if the file was not
     */
    public static OutputStream newOutputStream(Path path, OpenOption... options)
            throws IOException {
        String p = pathOf(path);
        Opening a = resolveOptions(options, true);
        if (a.readFrom) {
            throw new UnsupportedOperationException("READ on newOutputStream");
        }
        boolean exists = (Fs.stat(p) & Fs.EXISTS) != 0;
        if (a.createNew && exists) {
            throw new FileAlreadyExistsException(p);
        }
        if (!exists && !a.creating && !a.createNew) {
            throw new NoSuchFileException(p);
        }
        return new FileOutputStream(p, a.appending);
    }

    /**
     * A channel with a position over `path`.
     *
     * <p>It is this class's only opening method that does **not** go through memory: underneath is
     * `java.nio.channels.FileChannel`, which goes to the disk on every read and every write. It
     * comes out expensive --O(n) per operation, because the native only knows how to read and write
     * the whole file-- and in exchange it is the only one that honours what it promises: when
     * `write` returns, the bytes are on the disk. `FileChannel`'s header explains the full deal.
     *
     * <p>**The options are resolved by `FileChannel.open`, not by this class.** That is why `SYNC`,
     * `DSYNC` and `DELETE_ON_CLOSE` are accepted here while `newInputStream` and `newOutputStream`
     * reject them: there they cannot be honoured and here they can. `SPARSE` is rejected on both.
     * The why is in the class's header.
     *
     * @throws ProviderMismatchException if `path` is not this library's
     * @throws IllegalArgumentException if the options contradict each other
     * @throws UnsupportedOperationException if an option this VM cannot honour is asked for
     * @throws NoSuchFileException if it does not exist and creating it was not asked for
     * @throws FileAlreadyExistsException with `CREATE_NEW` if it was already there
     */
    public static SeekableByteChannel newByteChannel(Path path, OpenOption... options)
            throws IOException {
        // The provider check is done here and not further down: `FileChannel.open` works with the
        // path as a string and does not care where it came from, but `Files`'s contract says a
        // foreign `Path` is `ProviderMismatchException`.
        pathOf(path);
        return FileChannel.open(path, options);
    }

    /**
     * Like the other, with the options in a set and initial attributes.
     *
     * <p>`attrs` has to arrive empty, for the same reason as in `createFile`: there is no native
     * that sets permissions on creation. `FileChannel.open` rejects it, so the check is not
     * repeated here --duplicating it is how one ends up with two different messages for the same
     * error.
     *
     * @throws UnsupportedOperationException if `attrs` carries anything
     */
    public static SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options,
            FileAttribute<?>... attrs) throws IOException {
        pathOf(path);
        return FileChannel.open(path, options, attrs);
    }

    /** A buffered reader over `path`, decoding with `cs`. */
    public static BufferedReader newBufferedReader(Path path, Charset cs) throws IOException {
        return new BufferedReader(new InputStreamReader(newInputStream(path), cs));
    }

    /** Like the other, in UTF-8. */
    public static BufferedReader newBufferedReader(Path path) throws IOException {
        return newBufferedReader(path, StandardCharsets.UTF_8);
    }

    /** A buffered writer over `path`, encoding with `cs`. */
    public static BufferedWriter newBufferedWriter(Path path, Charset cs, OpenOption... options)
            throws IOException {
        return new BufferedWriter(new OutputStreamWriter(newOutputStream(path, options), cs));
    }

    /** Like the other, in UTF-8. */
    public static BufferedWriter newBufferedWriter(Path path, OpenOption... options)
            throws IOException {
        return newBufferedWriter(path, StandardCharsets.UTF_8, options);
    }

    // ------------------------------------------------------------------------------------------
    // Creating
    // ------------------------------------------------------------------------------------------

    // It rejects the creation attributes. It is done in one place because the reason is a single
    // one: the native that creates takes no permission parameter.
    private static void noAttributes(FileAttribute<?>[] attrs) {
        if (attrs.length > 0) {
            throw new UnsupportedOperationException(
                    "KajiJDK cannot set attributes when creating a file");
        }
    }

    /**
     * It creates an empty file.
     *
     * <p>**It is not atomic, unlike the JDK's.** Here it is two steps --check it does not exist and
     * write zero bytes-- and between the two another process could create it; in that case this
     * method overwrites it instead of failing. The JDK does it in a single system call with
     * `O_EXCL`. When there is a native for exclusive creation, this is fixed in here and nobody
     * else finds out.
     *
     * @throws FileAlreadyExistsException if it already exists
     * @throws UnsupportedOperationException if any `FileAttribute` is passed
     */
    public static Path createFile(Path path, FileAttribute<?>... attrs) throws IOException {
        String p = pathOf(path);
        noAttributes(attrs);
        if ((Fs.stat(p) & Fs.EXISTS) != 0) {
            throw new FileAlreadyExistsException(p);
        }
        if (!Fs.writeAllBytes(p, new byte[0], false)) {
            throw new IOException("cannot create " + p);
        }
        return path;
    }

    /**
     * It creates a directory. The parent has to exist.
     *
     * @throws FileAlreadyExistsException if there is already something by that name
     * @throws NoSuchFileException if the parent directory is missing
     */
    public static Path createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        String p = pathOf(dir);
        noAttributes(attrs);
        if ((Fs.stat(p) & Fs.EXISTS) != 0) {
            throw new FileAlreadyExistsException(p);
        }
        Path parent = dir.toAbsolutePath().getParent();
        if (parent != null && (Fs.stat(parent.toString()) & Fs.EXISTS) == 0) {
            throw new NoSuchFileException(parent.toString());
        }
        if (!Fs.mkdir(p, false)) {
            throw new IOException("cannot create directory " + p);
        }
        return dir;
    }

    /**
     * It creates the directory and every missing parent.
     *
     * <p>Unlike `createDirectory`, it **does not fail if it already exists** -- that is the only
     * contract difference between the two and it is what makes this one useful for ensuring a path.
     *
     * @throws FileAlreadyExistsException if the path exists but is not a directory
     */
    public static Path createDirectories(Path dir, FileAttribute<?>... attrs) throws IOException {
        String p = pathOf(dir);
        noAttributes(attrs);
        int st = Fs.stat(p);
        if ((st & Fs.EXISTS) != 0) {
            if ((st & Fs.IS_DIRECTORY) == 0) {
                throw new FileAlreadyExistsException(p);
            }
            return dir;
        }
        if (!Fs.mkdir(p, true)) {
            throw new IOException("cannot create directories " + p);
        }
        return dir;
    }

    // A candidate name. The JDK uses a random long in decimal; the same is done here so the names
    // look the same.
    private static String tempName(String prefix, String suffix, boolean isDirectory) {
        String pre = (prefix == null) ? "" : prefix;
        String suf = suffix;
        if (suf == null) {
            suf = isDirectory ? "" : ".tmp";
        }
        long n = RANDOM.nextLong();
        // `Long.MIN_VALUE` has no absolute value: it is handled separately so as not to print the
        // sign.
        String middle = (n == Long.MIN_VALUE) ? "0" : Long.toString(Math.abs(n));
        return pre + middle + suf;
    }

    /**
     * A fresh file with a unique name inside `dir`.
     *
     * <p>**It is not atomic** for the same reason as `createFile`; it retries until a name is not
     * taken, and after several attempts it gives up rather than spin forever.
     */
    public static Path createTempFile(Path dir, String prefix, String suffix,
            FileAttribute<?>... attrs) throws IOException {
        noAttributes(attrs);
        int attempts = 0;
        while (attempts < 100) {
            Path p = dir.resolve(tempName(prefix, suffix, false));
            if ((Fs.stat(p.toString()) & Fs.EXISTS) == 0) {
                return createFile(p);
            }
            attempts = attempts + 1;
        }
        throw new IOException("cannot create a unique temporary file in " + dir);
    }

    /** Like the other, in `java.io.tmpdir`'s directory. */
    public static Path createTempFile(String prefix, String suffix, FileAttribute<?>... attrs)
            throws IOException {
        return createTempFile(tempDirectory(), prefix, suffix, attrs);
    }

    /** A fresh directory with a unique name inside `dir`. It is not atomic. */
    public static Path createTempDirectory(Path dir, String prefix, FileAttribute<?>... attrs)
            throws IOException {
        noAttributes(attrs);
        int attempts = 0;
        while (attempts < 100) {
            Path p = dir.resolve(tempName(prefix, null, true));
            if ((Fs.stat(p.toString()) & Fs.EXISTS) == 0) {
                return createDirectory(p);
            }
            attempts = attempts + 1;
        }
        throw new IOException("cannot create a unique temporary directory in " + dir);
    }

    /** Like the other, in `java.io.tmpdir`'s directory. */
    public static Path createTempDirectory(String prefix, FileAttribute<?>... attrs)
            throws IOException {
        return createTempDirectory(tempDirectory(), prefix, attrs);
    }

    private static Path tempDirectory() {
        String t = System.getProperty("java.io.tmpdir");
        if (t == null || t.length() == 0) {
            t = ".";
        }
        return Path.of(t);
    }

    // ------------------------------------------------------------------------------------------
    // Deleting, copying, moving
    // ------------------------------------------------------------------------------------------

    /**
     * It deletes the file, or the directory **if it is empty**.
     *
     * <p>That a directory with content is not deleted is the native's doing and it is deliberate: a
     * recursive delete hidden behind a `delete()` turns a path mistake into data loss.
     *
     * @throws NoSuchFileException if it does not exist
     * @throws DirectoryNotEmptyException if it is a directory with things inside
     */
    public static void delete(Path path) throws IOException {
        String p = pathOf(path);
        int st = Fs.stat(p);
        if ((st & Fs.EXISTS) == 0) {
            throw new NoSuchFileException(p);
        }
        if (Fs.delete(p)) {
            return;
        }
        if ((st & Fs.IS_DIRECTORY) != 0) {
            throw new DirectoryNotEmptyException(p);
        }
        throw new AccessDeniedException(p);
    }

    /**
     * It deletes if it is there; it returns whether it deleted anything.
     *
     * <p>**It is not atomic**: it checks and then deletes. It serves the common case --not wanting
     * to write the `try`/`catch`-- not arbitrating between processes.
     */
    public static boolean deleteIfExists(Path path) throws IOException {
        String p = pathOf(path);
        if ((Fs.stat(p) & Fs.EXISTS) == 0) {
            return false;
        }
        delete(path);
        return true;
    }

    // It sorts out the copy options. It returns whether the target has to be overwritten; it
    // rejects the ones that cannot be honoured, each with the exception the spec assigns it.
    private static boolean resolveCopyOptions(CopyOption[] options) throws IOException {
        boolean overwrite = false;
        int i = 0;
        while (i < options.length) {
            CopyOption o = options[i];
            if (o == StandardCopyOption.REPLACE_EXISTING) {
                overwrite = true;
            } else if (o == StandardCopyOption.COPY_ATTRIBUTES) {
                // Metadata can be neither read nor written: accepting it in silence would leave
                // the target with dates and permissions different from the source's without a
                // word.
                throw new UnsupportedOperationException("COPY_ATTRIBUTES: no attribute natives");
            } else if (o == StandardCopyOption.ATOMIC_MOVE) {
                throw new AtomicMoveNotSupportedException(null, null,
                        "move is copy+delete in KajiJDK; there is no rename native");
            } else if (o == LinkOption.NOFOLLOW_LINKS) {
                // With no links, not following them is what already happens: it changes nothing.
            } else {
                throw new UnsupportedOperationException(String.valueOf(o) + " not supported");
            }
            i = i + 1;
        }
        return overwrite;
    }

    /**
     * It copies `source` into `target`.
     *
     * <p>**Regular files only.** Copying a directory in the JDK creates the empty directory in the
     * target; that can be done here, and the spec says a directory's copy is **not** recursive --
     * so the case is supported.
     *
     * <p>Like everything else, it goes through memory: a one-gigabyte file is copied by reading it
     * whole.
     *
     * @throws FileAlreadyExistsException if the target exists and `REPLACE_EXISTING` was not passed
     * @throws UnsupportedOperationException with `COPY_ATTRIBUTES`
     */
    public static Path copy(Path source, Path target, CopyOption... options) throws IOException {
        String s = pathOf(source);
        String t = pathOf(target);
        boolean overwrite = resolveCopyOptions(options);
        int sourceStat = Fs.stat(s);
        if ((sourceStat & Fs.EXISTS) == 0) {
            throw new NoSuchFileException(s);
        }
        int targetStat = Fs.stat(t);
        if ((targetStat & Fs.EXISTS) != 0) {
            if (!overwrite) {
                throw new FileAlreadyExistsException(t);
            }
            if (!Fs.delete(t)) {
                throw new DirectoryNotEmptyException(t);
            }
        }
        if ((sourceStat & Fs.IS_DIRECTORY) != 0) {
            // A directory's copy creates an empty one in the target; it does not descend. It is
            // what the spec says, not a limitation of this implementation.
            if (!Fs.mkdir(t, false)) {
                throw new IOException("cannot create directory " + t);
            }
            return target;
        }
        byte[] b = Fs.readAllBytes(s);
        if (b == null) {
            throw whyItWasNotRead(s);
        }
        if (!Fs.writeAllBytes(t, b, false)) {
            throw new IOException("cannot write " + t);
        }
        return target;
    }

    /**
     * It moves `source` to `target`.
     *
     * <p>**It is copy and delete, not a rename.** There is no rename native, so there is an instant
     * when the file is on both sides. The consequences, said plainly:
     *
     * <ul>
     *   <li>`ATOMIC_MOVE` raises `AtomicMoveNotSupportedException`, always. It is the correct
     *       answer: whoever asks for it asks because it matters to them.
     *   <li>A cut in the middle can leave the file duplicated. It never loses it: the target is
     *       written first and only then is the source deleted.
     *   <li>Moving a directory with content **fails**, because deleting the source cannot delete a
     *       non-empty directory. In the JDK a rename within the same volume moves it.
     * </ul>
     */
    public static Path move(Path source, Path target, CopyOption... options) throws IOException {
        String s = pathOf(source);
        copy(source, target, options);
        if (!Fs.delete(s)) {
            // The target is already written. It is reported rather than kept quiet: the tree has
            // been left with one copy too many and the caller has to know.
            throw new IOException("copied to " + pathOf(target) + " but cannot delete " + s);
        }
        return target;
    }

    // ------------------------------------------------------------------------------------------
    // Queries
    // ------------------------------------------------------------------------------------------

    /**
     * Whether the path exists.
     *
     * <p>`options` is accepted and changes nothing: with no symbolic links in the model, following
     * them or not comes to the same.
     */
    public static boolean exists(Path path, LinkOption... options) {
        return (Fs.stat(pathOf(path)) & Fs.EXISTS) != 0;
    }

    /**
     * Whether the path does **not** exist.
     *
     * <p>It is not `exists`'s negation in the JDK --there both can give `false` when it cannot be
     * determined-- but here it is: `stat` does not tell "does not exist" from "cannot look", and
     * faking a third state the native does not report would be inventing it.
     */
    public static boolean notExists(Path path, LinkOption... options) {
        return (Fs.stat(pathOf(path)) & Fs.EXISTS) == 0;
    }

    /** Whether it exists and is a regular file. */
    public static boolean isRegularFile(Path path, LinkOption... options) {
        return (Fs.stat(pathOf(path)) & Fs.IS_FILE) != 0;
    }

    /** Whether it exists and is a directory. */
    public static boolean isDirectory(Path path, LinkOption... options) {
        return (Fs.stat(pathOf(path)) & Fs.IS_DIRECTORY) != 0;
    }

    /** Whether this VM can read it. */
    public static boolean isReadable(Path path) {
        return (Fs.stat(pathOf(path)) & Fs.CAN_READ) != 0;
    }

    /** Whether this VM can write it. */
    public static boolean isWritable(Path path) {
        return (Fs.stat(pathOf(path)) & Fs.CAN_WRITE) != 0;
    }

    /**
     * Whether this VM can execute it -- **always `false` in KajiJDK**.
     *
     * <p>`stat` brings read and write, not execute, so it cannot be determined here. And `false`
     * **is** the answer the spec requires for that case: the contract says `false` "if the file
     * does not exist, if execute permission would be denied, **or if the access cannot be
     * determined**". All three are the same `false`, which is why nothing has to be invented.
     *
     * <p>It is the only one of the three `is*able` that differs from its equivalent in the
     * provider: `KajiFileSystemProvider.checkAccess(p, EXECUTE)` throws
     * `UnsupportedOperationException`, because there the signature **does** allow saying "cannot",
     * and saying it beats a `false`.
     */
    public static boolean isExecutable(Path path) {
        pathOf(path);
        return false;
    }

    /**
     * Whether it is a symbolic link -- **always `false` in KajiJDK**.
     *
     * <p>As in `isExecutable`, `false` is what the spec asks for when it cannot be determined. This
     * VM's `stat` follows links and returns the target's data, so a link to a file looks like the
     * file: the model underneath is transparent to links and has nothing to tell them apart
     * with.
     */
    public static boolean isSymbolicLink(Path path) {
        try {
            return readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS)
                    .isSymbolicLink();
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Whether the file is considered hidden.
     *
     * <p>**The definition is the provider's, and this is KajiJDK's: the name starts with a dot.**
     * It is not a half answer --the spec says in as many words that "the exact definition of hidden
     * is platform or provider dependent"-- so a provider that picks a rule and publishes it is
     * answering the contract, not dodging it. The rule picked is the one `java.io.File.isHidden()`
     * already uses in this library: two different answers to the same question about the same file
     * would be the real inconsistency.
     *
     * <p>**How it differs from the JDK on Windows.** There the answer comes from the DOS bit, which
     * `stat` does not bring: `.gitignore` gives `false` on a Windows JDK and `true` here, and a
     * file marked hidden without a leading dot gives `true` there and `false` here. It is a
     * difference of definition, not a reading error, and that is why it is written down.
     *
     * <p>It does not look at the disk --it does not need to in order to answer by the name-- so it
     * does not fail if the file does not exist either. It is the same thing the JDK's Unix provider
     * does.
     */
    public static boolean isHidden(Path path) throws IOException {
        pathOf(path);
        Path name = path.getFileName();
        if (name == null) {
            return false;
        }
        String s = name.toString();
        return s.length() > 0 && s.charAt(0) == '.';
    }

    /**
     * The size in bytes.
     *
     * @throws NoSuchFileException if it does not exist
     */
    public static long size(Path path) throws IOException {
        String p = pathOf(path);
        if ((Fs.stat(p) & Fs.EXISTS) == 0) {
            throw new NoSuchFileException(p);
        }
        return Fs.size(p);
    }

    /**
     * The position of the first byte where the two files differ, or -1 if they are identical.
     *
     * <p>If one is a prefix of the other, the answer is the shorter one's size.
     *
     * <p>Unlike the JDK there is **no** shortcut by file identity: there, two paths to the same
     * inode give -1 without reading anything. Here the bytes are read and compared, which gives the
     * same answer for the same content -- only doing more work.
     */
    public static long mismatch(Path path, Path path2) throws IOException {
        byte[] a = readAllBytes(path);
        byte[] b = readAllBytes(path2);
        int n = Math.min(a.length, b.length);
        int i = 0;
        while (i < n) {
            if (a[i] != b[i]) {
                return i;
            }
            i = i + 1;
        }
        return (a.length == b.length) ? -1L : ((long) n);
    }

    /**
     * A view of the file's attributes -- **always `null` in KajiJDK**.
     *
     * <p>`null` is not a hole papered over: the spec says it is what has to be returned when the
     * view asked for is not available, and here **none** is available.
     *
     * <p>And it is not because the attributes cannot be read --`readAttributes` reads them-- but
     * because a view is an object that **reads and writes**: `BasicFileAttributeView`, the only one
     * a JDK is obliged to offer, has `setTimes`, which sets all three timestamps at once, and there
     * is no native that writes the other two nor an exception declared there with which to say so.
     * A view whose `setTimes` lied would be worse than having no view. That is why
     * `FileSystem.supportedFileAttributeViews()` does not name `"basic"` either: the two answers
     * say the same thing. See `java.nio.file.attribute.BasicFileAttributes`.
     */
    public static <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type,
            LinkOption... options) {
        pathOf(path);
        if (type == null) {
            throw new NullPointerException();
        }
        return null;
    }

    // ------------------------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------------------------

    // The two timestamps with no native. The epoch is **not** a filler: the specs of
    // `BasicFileAttributes.lastAccessTime` and `creationTime` say in as many words that if the
    // filesystem does not support the stamp, the method returns "an implementation specific default
    // value, typically a `FileTime` representing the epoch". There is no native for either of the
    // two, so this is the value **the contract answers with**, not a zero dressed up as a date.
    //
    // `lastModifiedTime` does **not** use this any more: `Fs.mtime` reads it for real. Keeping it
    // at the epoch while `setLastModifiedTime` wrote it would have been worse than both absences
    // together -- one could set a date and read another.
    //
    // A single shared instance: `FileTime` is immutable.
    private static final FileTime EPOCH = FileTime.fromMillis(0L);

    /**
     * A file's nine basic attributes, taken from a `stat`, a `size` and an `mtime`.
     *
     * <p>Attribute by attribute, where each comes from:
     *
     * <ul>
     *   <li>`isRegularFile`, `isDirectory` -- `stat` flags, directly.
     *   <li>`isOther` -- it exists and is neither of the two. It is the spec's definition.
     *   <li>`size` -- the `size` native.
     *   <li>`lastModifiedTime` -- the `mtime` native.
     *   <li>`lastAccessTime`, `creationTime` -- the epoch; see `EPOCH`.
     *   <li>`fileKey` -- `null`, which the spec declares valid when there is nothing like an inode.
     *   <li>`isSymbolicLink` -- `false`. It is the only one where the answer is not backed by this
     *       type's contract but by `Files.isSymbolicLink`'s, which makes `false` the answer for
     *       "cannot be determined". `stat` follows links, so the model underneath does not see
     *       them.
     * </ul>
     *
     * <p>**One** snapshot is taken in the constructor and afterwards only fields are consulted:
     * that is what makes the nine questions consistent with each other. Asking `stat` in each
     * accessor could mix two moments --a file in one, deleted in the other-- which is exactly what
     * a `BasicFileAttributes` exists to avoid.
     */
    private static final class BasicAttrs implements BasicFileAttributes {

        private final int flags;
        private final long bytes;

        // The modification date, read in the **same snapshot** as the flags and the size. It is
        // what keeps the nine questions consistent with each other, which is what
        // `BasicFileAttributes` exists for.
        private final FileTime modified;

        BasicAttrs(int flags, long bytes, long millis) {
            this.flags = flags;
            this.bytes = bytes;
            this.modified = millis == Long.MIN_VALUE ? EPOCH : FileTime.fromMillis(millis);
        }

        public FileTime lastModifiedTime() {
            return this.modified;
        }

        public FileTime lastAccessTime() {
            return EPOCH;
        }

        public FileTime creationTime() {
            return EPOCH;
        }

        public boolean isRegularFile() {
            return (this.flags & Fs.IS_FILE) != 0;
        }

        public boolean isDirectory() {
            return (this.flags & Fs.IS_DIRECTORY) != 0;
        }

        public boolean isSymbolicLink() {
            return false;
        }

        public boolean isOther() {
            return (this.flags & Fs.EXISTS) != 0
                    && (this.flags & (Fs.IS_FILE | Fs.IS_DIRECTORY)) == 0;
        }

        public long size() {
            return this.bytes;
        }

        public Object fileKey() {
            return null;
        }
    }

    // `p`'s snapshot, or the exception that fits if it could not be looked at.
    private static BasicFileAttributes readAttrs(String p) throws IOException {
        int st = Fs.stat(p);
        if ((st & Fs.EXISTS) == 0) {
            throw new NoSuchFileException(p);
        }
        return new BasicAttrs(st, Fs.size(p), Fs.mtime(p));
    }

    // The `basic` view's nine names, in the order the JDK returns them. A `Map`'s order is part of
    // no contract, but agreeing comes free and makes the two outputs comparable when one is tested
    // against the other.
    private static final String[] BASIC_NAMES = {
        "lastModifiedTime", "lastAccessTime", "creationTime", "size",
        "isRegularFile", "isDirectory", "isSymbolicLink", "isOther", "fileKey"
    };

    // The half before the colon in "view:attributes", with `basic` as the default.
    private static String viewOf(String attribute) {
        int i = attribute.indexOf(':');
        return (i < 0) ? "basic" : attribute.substring(0, i);
    }

    // The half after it.
    private static String namesOf(String attribute) {
        int i = attribute.indexOf(':');
        return (i < 0) ? attribute : attribute.substring(i + 1);
    }

    // A `basic` attribute's value by name, or `null` if the name is not one of the nine.
    //
    // It returns `null` rather than throw so the caller chooses the exception: `readAttributes`
    // puts the full name with the view in the message, and `setAttribute` tells "does not exist"
    // from "exists but is read-only".
    private static Object basicAttribute(BasicFileAttributes a, String name) {
        if (name.equals("lastModifiedTime")) {
            return a.lastModifiedTime();
        }
        if (name.equals("lastAccessTime")) {
            return a.lastAccessTime();
        }
        if (name.equals("creationTime")) {
            return a.creationTime();
        }
        if (name.equals("size")) {
            return Long.valueOf(a.size());
        }
        if (name.equals("isRegularFile")) {
            return Boolean.valueOf(a.isRegularFile());
        }
        if (name.equals("isDirectory")) {
            return Boolean.valueOf(a.isDirectory());
        }
        if (name.equals("isSymbolicLink")) {
            return Boolean.valueOf(a.isSymbolicLink());
        }
        if (name.equals("isOther")) {
            return Boolean.valueOf(a.isOther());
        }
        if (name.equals("fileKey")) {
            return a.fileKey();
        }
        return null;
    }

    /**
     * The file's attributes, of the type asked for.
     *
     * <p>Only `BasicFileAttributes.class`: it is the only view this VM can answer. For
     * `DosFileAttributes` or `PosixFileAttributes` it throws `UnsupportedOperationException`, which
     * is what the spec declares for an unsupported attribute type.
     *
     * <p>`options` is accepted and changes nothing: with no links in the model, following them or
     * not comes to the same. See `BasicAttrs` for the detail of what comes from where.
     *
     * @throws UnsupportedOperationException if `type` is not `BasicFileAttributes.class`
     * @throws NoSuchFileException if the file is not there
     */
    public static <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type,
            LinkOption... options) throws IOException {
        String p = pathOf(path);
        if (type == null) {
            throw new NullPointerException();
        }
        if (type != BasicFileAttributes.class) {
            throw new UnsupportedOperationException(type.getName() + " is not supported");
        }
        return type.cast(readAttrs(p));
    }

    /**
     * The named attributes, as a map from name to value.
     *
     * <p>`attributes` has the shape `[view:]list`, with the list comma-separated and `*` to ask for
     * all of them. The only view is `basic`, which is also the default.
     *
     * <p>The map's keys go **without** the view's prefix, as in the JDK.
     *
     * @throws UnsupportedOperationException if a view other than `basic` is named
     * @throws IllegalArgumentException if no attribute is named, or if one does not exist
     * @throws NoSuchFileException if the file is not there
     */
    public static Map<String, Object> readAttributes(Path path, String attributes,
            LinkOption... options) throws IOException {
        String p = pathOf(path);
        if (attributes == null) {
            throw new NullPointerException();
        }
        String view = viewOf(attributes);
        if (!view.equals("basic")) {
            throw new UnsupportedOperationException("View '" + view + "' not available");
        }
        String list = namesOf(attributes);
        if (list.length() == 0) {
            throw new IllegalArgumentException("No attributes specified");
        }
        BasicFileAttributes a = readAttrs(p);
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        int startOfRun = 0;
        while (startOfRun <= list.length()) {
            int comma = list.indexOf(',', startOfRun);
            String name = (comma < 0) ? list.substring(startOfRun) : list.substring(startOfRun, comma);
            if (name.equals("*")) {
                // `*` wins over the rest: asking for "size,*" gives the nine, as in the JDK.
                int i = 0;
                while (i < BASIC_NAMES.length) {
                    out.put(BASIC_NAMES[i], basicAttribute(a, BASIC_NAMES[i]));
                    i = i + 1;
                }
                return out;
            }
            if (name.length() > 0) {
                Object v = basicAttribute(a, name);
                if (v == null && !name.equals("fileKey")) {
                    throw new IllegalArgumentException("'basic:" + name + "' not recognized");
                }
                out.put(name, v);
            }
            if (comma < 0) {
                break;
            }
            startOfRun = comma + 1;
        }
        return out;
    }

    /**
     * **One** attribute's value.
     *
     * <p>Like `readAttributes(path, attributes, options)` but for a single name: no `*` and no
     * commas.
     *
     * @throws IllegalArgumentException if the name carries `*` or `,`, or if it does not exist
     * @throws UnsupportedOperationException if a view other than `basic` is named
     * @throws NoSuchFileException if the file is not there
     */
    public static Object getAttribute(Path path, String attribute, LinkOption... options)
            throws IOException {
        if (attribute == null) {
            throw new NullPointerException();
        }
        if (attribute.indexOf('*') >= 0 || attribute.indexOf(',') >= 0) {
            throw new IllegalArgumentException(attribute);
        }
        Map<String, Object> one = readAttributes(path, attribute, options);
        return one.get(namesOf(attribute));
    }

    /**
     * It sets an attribute -- **it never works in KajiJDK**.
     *
     * <p>No view of this VM is writable through here. The exception depends on **why** it cannot
     * be, so the error says something:
     *
     * <ul>
     *   <li>a view other than `basic`: `UnsupportedOperationException`, which is what the spec
     *       declares for "the attribute view is not available".
     *   <li>a name that is not one of the nine: `IllegalArgumentException`, as in the JDK.
     *   <li>one of the six read-only attributes (`size`, `isDirectory`, ...):
     *       `IllegalArgumentException`, also as in the JDK -- they are writable on **no**
     *       filesystem.
     *   <li>one of the three timestamps, which in the JDK **can** be set:
     *       `UnsupportedOperationException`. It is the only one of the four where the difference is
     *       this VM's and not the spec's, and that is why the exception is the one that says "it
     *       cannot be done here". (`Fs.setMtime` exists and `setLastModifiedTime` uses it; what has
     *       no native is setting a timestamp through a named attribute, which the JDK routes
     *       through `BasicFileAttributeView.setTimes` -- all three at once.)
     * </ul>
     */
    public static Path setAttribute(Path path, String attribute, Object value,
            LinkOption... options) throws IOException {
        pathOf(path);
        if (attribute == null) {
            throw new NullPointerException();
        }
        String view = viewOf(attribute);
        if (!view.equals("basic")) {
            throw new UnsupportedOperationException("View '" + view + "' not available");
        }
        String name = namesOf(attribute);
        boolean isTimeAttribute = name.equals("lastModifiedTime") || name.equals("lastAccessTime")
                || name.equals("creationTime");
        if (isTimeAttribute) {
            throw new UnsupportedOperationException(
                    "KajiJDK cannot write 'basic:" + name + "': no native writes timestamps");
        }
        throw new IllegalArgumentException("'basic:" + name + "' not recognized");
    }

    /**
     * The last modification date.
     *
     * <p>This javadoc used to say the answer is always the epoch; it is read for real, out of
     * `Fs.mtime`, in the same snapshot as the rest of the attributes. The epoch is still the answer
     * for `lastAccessTime` and `creationTime` -- see `EPOCH`.
     *
     * @throws NoSuchFileException if the file is not there
     */
    public static FileTime getLastModifiedTime(Path path, LinkOption... options)
            throws IOException {
        return readAttrs(pathOf(path)).lastModifiedTime();
    }

    /**
     * The POSIX permissions -- **it always fails**.
     *
     * <p>`UnsupportedOperationException` is what the spec declares for when the filesystem does not
     * support `PosixFileAttributeView`, and this one does not: `stat` gives five flags and none of
     * them is a nine-bit mode. See `FileSystem.supportedFileAttributeViews`, which returns the
     * empty set and is where this answer comes from.
     */
    public static Set<PosixFilePermission> getPosixFilePermissions(Path path,
            LinkOption... options) throws IOException {
        pathOf(path);
        throw new UnsupportedOperationException("PosixFileAttributeView is not supported");
    }

    /** It sets the POSIX permissions -- **it always fails**, for the same reason as the other. */
    public static Path setPosixFilePermissions(Path path, Set<PosixFilePermission> perms)
            throws IOException {
        pathOf(path);
        if (perms == null) {
            throw new NullPointerException();
        }
        throw new UnsupportedOperationException("PosixFileAttributeView is not supported");
    }

    /**
     * The file's owner -- **it always fails**.
     *
     * <p>`UnsupportedOperationException` is what the spec declares for when
     * `FileOwnerAttributeView` is not supported. There is no native that returns a uid, and nothing
     * with which to turn a uid into a `UserPrincipal` either: see
     * `FileSystem.getUserPrincipalLookupService`, which fails for the same reason.
     */
    public static UserPrincipal getOwner(Path path, LinkOption... options) throws IOException {
        pathOf(path);
        throw new UnsupportedOperationException("FileOwnerAttributeView is not supported");
    }

    /** It sets the owner -- **it always fails**, for the same reason as the other. */
    public static Path setOwner(Path path, UserPrincipal owner) throws IOException {
        pathOf(path);
        if (owner == null) {
            throw new NullPointerException();
        }
        throw new UnsupportedOperationException("FileOwnerAttributeView is not supported");
    }

    /**
     * The file's MIME type, or `null` if it cannot be determined.
     *
     * <p>**Today it returns `null` for everything, and that is the correct answer, not a stub.**
     * The spec says the result comes from the **installed** `FileTypeDetector`s --which are
     * discovered with `ServiceLoader`-- plus a system default detector. KajiJDK brings no default
     * detector, and the discovery finds nothing because the built-in loaders serve no resources
     * (see `java.util.ServiceLoader`'s header). With the chain empty, `null` --"could not be
     * determined"-- is the only thing that can be answered.
     *
     * <p>The walk is written out for real and not short-circuited to `return null`: the day the
     * discovery works, a detector put on the class path by the application is used without touching
     * this class. Guessing by extension in here would be answering on behalf of a detector nobody
     * registered.
     */
    public static String probeContentType(Path path) throws IOException {
        pathOf(path);
        // The `ServiceLoader` goes into a variable with the type written out rather than chaining
        // `.iterator()` onto the call: our `javac`'s inference does not propagate the type
        // parameter through the chain. See that session's report.
        ServiceLoader<FileTypeDetector> detectors = ServiceLoader.load(FileTypeDetector.class);
        Iterator<FileTypeDetector> it = detectors.iterator();
        while (it.hasNext()) {
            String kind = it.next().probeContentType(path);
            if (kind != null) {
                return kind;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------------------------
    // Links
    // ------------------------------------------------------------------------------------------

    /**
     * It creates a symbolic link -- **it always fails**.
     *
     * <p>`UnsupportedOperationException` is exactly what the spec declares for "the implementation
     * does not support the creation of symbolic links", and `jdk.internal.io.Fs`'s natives include
     * none that makes one. A program that calls this gets the same exception it would get from a
     * JDK over a filesystem with no links.
     */
    public static Path createSymbolicLink(Path link, Path target, FileAttribute<?>... attrs)
            throws IOException {
        pathOf(link);
        pathOf(target);
        throw new UnsupportedOperationException("KajiJDK does not support symbolic links");
    }

    /**
     * It creates a hard link -- **it always fails**.
     *
     * <p>The spec declares `UnsupportedOperationException` for "the implementation does not support
     * adding an existing file to a directory", which is what happens here.
     */
    public static Path createLink(Path link, Path existing) throws IOException {
        pathOf(link);
        pathOf(existing);
        throw new UnsupportedOperationException("KajiJDK does not support hard links");
    }

    /**
     * A symbolic link's target -- **it always fails**.
     *
     * <p>The same reason as `createSymbolicLink`, and the same exception declared by the spec. Note
     * it is **not** `NotLinkException`: that would say "this path is not a link", which is an
     * assertion about the file, and here what does not exist is the operation.
     */
    public static Path readSymbolicLink(Path link) throws IOException {
        pathOf(link);
        throw new UnsupportedOperationException("KajiJDK does not support symbolic links");
    }

    // ------------------------------------------------------------------------------------------
    // Reading and writing the content
    // ------------------------------------------------------------------------------------------

    /**
     * All of the file's bytes.
     *
     * <p>It is the native operation as it stands: in this VM reading a file is reading it whole, so
     * this method is the cheap one and the rest are built on top of it.
     */
    public static byte[] readAllBytes(Path path) throws IOException {
        String p = pathOf(path);
        byte[] b = Fs.readAllBytes(p);
        if (b == null) {
            throw whyItWasNotRead(p);
        }
        return b;
    }

    /** The file as text, decoded with `cs`. */
    public static String readString(Path path, Charset cs) throws IOException {
        if (cs == null) {
            throw new NullPointerException();
        }
        return new String(readAllBytes(path), cs);
    }

    /** The file as text, in UTF-8. */
    public static String readString(Path path) throws IOException {
        return readString(path, StandardCharsets.UTF_8);
    }

    // It cuts into lines at `\n`, `\r` or `\r\n`, without leaving an empty line at the end if the
    // file ends with a break. It is `BufferedReader.readLine`'s rule, written here over the already
    // decoded string so as not to build the whole stream plumbing for what is a walk.
    private static List<String> splitIntoLines(String text) {
        List<String> out = new ArrayList<String>();
        int i = 0;
        int startOfRun = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '\n') {
                out.add(text.substring(startOfRun, i));
                i = i + 1;
                startOfRun = i;
            } else if (c == '\r') {
                out.add(text.substring(startOfRun, i));
                i = i + 1;
                if (i < text.length() && text.charAt(i) == '\n') {
                    i = i + 1;
                }
                startOfRun = i;
            } else {
                i = i + 1;
            }
        }
        if (startOfRun < text.length()) {
            out.add(text.substring(startOfRun));
        }
        return out;
    }

    /** The file's lines, decoded with `cs`. */
    public static List<String> readAllLines(Path path, Charset cs) throws IOException {
        return splitIntoLines(readString(path, cs));
    }

    /** The file's lines, in UTF-8. */
    public static List<String> readAllLines(Path path) throws IOException {
        return readAllLines(path, StandardCharsets.UTF_8);
    }

    /**
     * The file's lines as a `Stream`.
     *
     * <p>**A difference from the JDK, and not a small one: here it is not lazy.** There the stream
     * reads as it is consumed and has to be closed; here the file is read whole first --there is no
     * other way of reading it-- and the stream comes out of a list in memory. The result is the
     * same lines in the same order; what changes is when the work is done and how much memory it
     * takes. `close()` on the stream is still valid and is not needed.
     */
    public static Stream<String> lines(Path path, Charset cs) throws IOException {
        return readAllLines(path, cs).stream();
    }

    /** Like the other, in UTF-8. */
    public static Stream<String> lines(Path path) throws IOException {
        return lines(path, StandardCharsets.UTF_8);
    }

    // It writes `data` honouring the options. It is the only place that calls `writeAllBytes`, so
    // the existence check and the choice to append are written once.
    private static Path writeTo(Path path, byte[] data, OpenOption[] options)
            throws IOException {
        String p = pathOf(path);
        Opening a = resolveOptions(options, true);
        if (a.readFrom) {
            throw new UnsupportedOperationException("READ on write");
        }
        boolean exists = (Fs.stat(p) & Fs.EXISTS) != 0;
        if (a.createNew && exists) {
            throw new FileAlreadyExistsException(p);
        }
        if (!exists && !a.creating && !a.createNew && options.length > 0) {
            throw new NoSuchFileException(p);
        }
        if (!Fs.writeAllBytes(p, data, a.appending)) {
            throw new IOException("cannot write " + p);
        }
        return path;
    }

    /**
     * It writes the bytes into the file.
     *
     * <p>With no options: it creates, truncates and writes. With `APPEND`: it adds at the end.
     */
    public static Path write(Path path, byte[] bytes, OpenOption... options) throws IOException {
        if (bytes == null) {
            throw new NullPointerException();
        }
        return writeTo(path, bytes, options);
    }

    /** It writes the text encoded with `cs`. */
    public static Path writeString(Path path, CharSequence csq, Charset cs, OpenOption... options)
            throws IOException {
        if (csq == null || cs == null) {
            throw new NullPointerException();
        }
        return writeTo(path, csq.toString().getBytes(cs), options);
    }

    /** It writes the text in UTF-8. */
    public static Path writeString(Path path, CharSequence csq, OpenOption... options)
            throws IOException {
        return writeString(path, csq, StandardCharsets.UTF_8, options);
    }

    /**
     * It writes the lines, each followed by the platform's line separator.
     *
     * <p>The whole string is built and encoded in one go, rather than line by line: with a native
     * that writes the complete file, writing one at a time would mean re-reading and rewriting
     * everything for each line.
     */
    public static Path write(Path path, Iterable<? extends CharSequence> lines, Charset cs,
            OpenOption... options) throws IOException {
        if (lines == null || cs == null) {
            throw new NullPointerException();
        }
        String lineBreak = System.lineSeparator();
        StringBuilder sb = new StringBuilder();
        Iterator<? extends CharSequence> it = lines.iterator();
        while (it.hasNext()) {
            CharSequence line = it.next();
            sb.append(line == null ? "null" : line.toString());
            sb.append(lineBreak);
        }
        return writeTo(path, sb.toString().getBytes(cs), options);
    }

    /** Like the other, in UTF-8. */
    public static Path write(Path path, Iterable<? extends CharSequence> lines,
            OpenOption... options) throws IOException {
        return write(path, lines, StandardCharsets.UTF_8, options);
    }

    /**
     * It dumps `in` into `target` and returns how many bytes it copied.
     *
     * <p>The stream is read whole into memory before writing, because the native writes the
     * complete file in one go. **`in` is not closed**: whoever called opened it, and closing
     * something one did not open is the kind of surprise that breaks an outer try-with-resources.
     *
     * @throws FileAlreadyExistsException if the target exists and `REPLACE_EXISTING` was not
     *         passed
     */
    public static long copy(InputStream in, Path target, CopyOption... options) throws IOException {
        if (in == null) {
            throw new NullPointerException();
        }
        String t = pathOf(target);
        boolean overwrite = resolveCopyOptions(options);
        if ((Fs.stat(t) & Fs.EXISTS) != 0 && !overwrite) {
            throw new FileAlreadyExistsException(t);
        }
        byte[] data = readAll(in);
        if (!Fs.writeAllBytes(t, data, false)) {
            throw new IOException("cannot write " + t);
        }
        return (long) data.length;
    }

    /**
     * It dumps `source` into `out` and returns how many bytes it copied.
     *
     * <p>**`out` is not closed**, for the same reason as `in` in the other overload.
     */
    public static long copy(Path source, OutputStream out) throws IOException {
        if (out == null) {
            throw new NullPointerException();
        }
        byte[] b = readAllBytes(source);
        out.write(b, 0, b.length);
        return (long) b.length;
    }

    // It gathers everything left in the stream. It doubles the buffer when it fills: growing by a
    // fixed size would be quadratic in the number of copies for a large stream.
    private static byte[] readAll(InputStream in) throws IOException {
        byte[] buf = new byte[8192];
        int used = 0;
        while (true) {
            if (used == buf.length) {
                byte[] more = new byte[buf.length * 2];
                System.arraycopy(buf, 0, more, 0, used);
                buf = more;
            }
            int n = in.read(buf, used, buf.length - used);
            if (n < 0) {
                break;
            }
            used = used + n;
        }
        byte[] out = new byte[used];
        System.arraycopy(buf, 0, out, 0, used);
        return out;
    }

    // ---- enumerating a directory --------------------------------------------------------------------
    //
    // All nine rest on `Fs.list`, and all nine rest on **one**: `walkFileTree`. The `walk`s, `find`
    // and `list` are stream views over the same traversal, and writing them separately would have
    // given four traversals that can drift apart.

    /** That directory's **direct** entries. Unfiltered, without descending. */
    public static DirectoryStream<Path> newDirectoryStream(Path dir) throws IOException {
        return new KajiDirectoryStream(dir, null);
    }

    /**
     * The one above filtered by a **glob** against each entry's name.
     *
     * <p>The pattern is compared against the path's last element and not against the whole path,
     * which is what makes `"*.java"` work as one expects.
     */
    public static DirectoryStream<Path> newDirectoryStream(Path dir, String glob)
            throws IOException {
        if (glob == null) {
            throw new NullPointerException("glob");
        }
        // `"*"` is the common case and means "everything": no matcher has to be built for that.
        if (glob.equals("*")) {
            return new KajiDirectoryStream(dir, null);
        }
        final PathMatcher matcher = dir.getFileSystem().getPathMatcher("glob:" + glob);
        return new KajiDirectoryStream(dir, new DirectoryStream.Filter<Path>() {
            public boolean accept(Path entry) {
                Path name = entry.getFileName();
                return name != null && matcher.matches(name);
            }
        });
    }

    /** The one above with a filter of one's own. */
    public static DirectoryStream<Path> newDirectoryStream(Path dir,
            DirectoryStream.Filter<? super Path> filter) throws IOException {
        if (filter == null) {
            throw new NullPointerException("filter");
        }
        return new KajiDirectoryStream(dir, filter);
    }

    /**
     * It walks the tree hanging off `start`, telling the visitor at each step.
     *
     * <p>It is the method the other four come out of. The order is the one the contract fixes: for
     * a directory, `preVisitDirectory`, then its children, then `postVisitDirectory`; for a file,
     * `visitFile`. And `visitFileFailed` when an entry could not be looked at -- which is **not**
     * an error of the traversal: the visitor decides whether to carry on.
     *
     * @param maxDepth how many levels to descend; `0` visits only `start`
     */
    public static Path walkFileTree(Path start, java.util.Set<FileVisitOption> options,
            int maxDepth, FileVisitor<? super Path> visitor) throws IOException {
        if (start == null || options == null || visitor == null) {
            throw new NullPointerException();
        }
        if (maxDepth < 0) {
            throw new IllegalArgumentException("maxDepth cannot be negative");
        }
        walkTree(start, 0, maxDepth, visitor);
        return start;
    }

    /** The one above with no options and no depth limit. */
    public static Path walkFileTree(Path start, FileVisitor<? super Path> visitor)
            throws IOException {
        return walkFileTree(start, java.util.Collections.<FileVisitOption>emptySet(),
                Integer.MAX_VALUE, visitor);
    }

    // The traversal. It returns what the visitor answered, so a `TERMINATE` cuts the whole tree and
    // not only the branch -- which is the difference between `TERMINATE` and `SKIP_SUBTREE`.
    private static FileVisitResult walkTree(Path p, int depth, int maxDepth,
            FileVisitor<? super Path> visitor) throws IOException {
        BasicFileAttributes attrs = null;
        try {
            attrs = readAttributes(p, BasicFileAttributes.class);
        } catch (IOException e) {
            return visitor.visitFileFailed(p, e);
        }
        if (!attrs.isDirectory() || depth >= maxDepth) {
            return visitor.visitFile(p, attrs);
        }
        FileVisitResult r = visitor.preVisitDirectory(p, attrs);
        if (r == FileVisitResult.TERMINATE) {
            return r;
        }
        if (r == FileVisitResult.SKIP_SUBTREE || r == FileVisitResult.SKIP_SIBLINGS) {
            // `SKIP_SIBLINGS` on a directory means "neither go in nor carry on with its
            // siblings": it does not descend, and what cuts the siblings is the loop above.
            return r == FileVisitResult.SKIP_SIBLINGS ? r : FileVisitResult.CONTINUE;
        }
        IOException failed = null;
        try {
            DirectoryStream<Path> children = newDirectoryStream(p);
            try {
                java.util.Iterator<Path> it = children.iterator();
                while (it.hasNext()) {
                    FileVisitResult hr = walkTree(it.next(), depth + 1, maxDepth, visitor);
                    if (hr == FileVisitResult.TERMINATE) {
                        return hr;
                    }
                    if (hr == FileVisitResult.SKIP_SIBLINGS) {
                        break;
                    }
                }
            } finally {
                children.close();
            }
        } catch (IOException e) {
            // The failure to list is handed to `postVisitDirectory`, which is where the contract
            // says it arrives. It is not thrown: the visitor may want to carry on with the rest of
            // the tree.
            failed = e;
        }
        return visitor.postVisitDirectory(p, failed);
    }

    /** A directory's direct entries, as a stream. */
    public static java.util.stream.Stream<Path> list(Path dir) throws IOException {
        List<Path> out = new ArrayList<Path>();
        DirectoryStream<Path> s = newDirectoryStream(dir);
        try {
            java.util.Iterator<Path> it = s.iterator();
            while (it.hasNext()) {
                out.add(it.next());
            }
        } finally {
            s.close();
        }
        return out.stream();
    }

    /**
     * The whole tree hanging off `start`, as a stream, down to `maxDepth` levels.
     *
     * <p>The first element is `start`. An entry that cannot be looked at **cuts** the stream with
     * an `IOException`, which is what the JDK does: `walk` has no way of reporting a partial
     * failure.
     */
    public static java.util.stream.Stream<Path> walk(Path start, int maxDepth,
            FileVisitOption... options) throws IOException {
        final List<Path> out = new ArrayList<Path>();
        walkFileTree(start, optionsOf(options), maxDepth, new SimpleFileVisitor<Path>() {
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                out.add(dir);
                return FileVisitResult.CONTINUE;
            }

            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                out.add(file);
                return FileVisitResult.CONTINUE;
            }

            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                throw exc;
            }
        });
        return out.stream();
    }

    /** The one above with no depth limit. */
    public static java.util.stream.Stream<Path> walk(Path start, FileVisitOption... options)
            throws IOException {
        return walk(start, Integer.MAX_VALUE, options);
    }

    /**
     * The tree's entries that satisfy the predicate.
     *
     * <p>The predicate receives the path **and its attributes**, which were already read in order
     * to walk: it is what stops whoever filters by size or by date from having to look at the disk
     * again.
     */
    public static java.util.stream.Stream<Path> find(Path start, int maxDepth,
            java.util.function.BiPredicate<Path, BasicFileAttributes> matcher,
            FileVisitOption... options) throws IOException {
        if (matcher == null) {
            throw new NullPointerException("matcher");
        }
        final List<Path> out = new ArrayList<Path>();
        final java.util.function.BiPredicate<Path, BasicFileAttributes> pred = matcher;
        walkFileTree(start, optionsOf(options), maxDepth, new SimpleFileVisitor<Path>() {
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (pred.test(dir, attrs)) {
                    out.add(dir);
                }
                return FileVisitResult.CONTINUE;
            }

            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (pred.test(file, attrs)) {
                    out.add(file);
                }
                return FileVisitResult.CONTINUE;
            }

            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                throw exc;
            }
        });
        return out.stream();
    }

    // The options as a set. Today the only one that exists is `FOLLOW_LINKS`, and this VM has no
    // symbolic links, so the traversal comes out the same with or without it. It is accepted all
    // the same because the signature declares it and rejecting it would be inventing an error.
    private static java.util.Set<FileVisitOption> optionsOf(FileVisitOption[] options) {
        java.util.Set<FileVisitOption> out = new java.util.HashSet<FileVisitOption>();
        int i = 0;
        while (options != null && i < options.length) {
            if (options[i] == null) {
                throw new NullPointerException("an option is null");
            }
            out.add(options[i]);
            i = i + 1;
        }
        return out;
    }

    // ---- identity and modification date ---------------------------------------------------------
    //
    // Both were out until their natives turned up, and both are of the same family: questions about
    // the file that the name cannot answer.

    /**
     * Whether the two paths name **the same file**.
     *
     * <p>The string cannot answer the question. On Windows `C:\A.TXT` and `C:.txt` are the same
     * file and are not the same text; a relative path and an absolute one are not either; and
     * `a/../b` and `b` less still. That is why the **canonical** form is compared, which resolves
     * all three. (The second path used to carry a literal BEL where its `` had been eaten by an
     * earlier edit.)
     *
     * <p>The equality shortcut goes first and is not only an optimisation: the spec says two equal
     * paths are the same file **without looking at the disk**, so two equal paths that do not exist
     * give `true` all the same.
     *
     * @throws NoSuchFileException if either of the two does not exist
     */
    public static boolean isSameFile(Path path, Path path2) throws IOException {
        if (path == null || path2 == null) {
            throw new NullPointerException();
        }
        if (path.equals(path2)) {
            return true;
        }
        String a = Fs.canonical(path.toString());
        if (a == null) {
            throw new NoSuchFileException(path.toString());
        }
        String b = Fs.canonical(path2.toString());
        if (b == null) {
            throw new NoSuchFileException(path2.toString());
        }
        return a.equals(b);
    }

    /**
     * It sets the last modification date.
     *
     * <p>It returns the same `path`, which is what allows chaining it after a `write`.
     *
     * @throws NoSuchFileException if the file does not exist
     */
    public static Path setLastModifiedTime(Path path, java.nio.file.attribute.FileTime time)
            throws IOException {
        if (path == null || time == null) {
            throw new NullPointerException();
        }
        if (!Fs.setMtime(path.toString(), time.toMillis())) {
            if ((Fs.stat(path.toString()) & Fs.EXISTS) == 0) {
                throw new NoSuchFileException(path.toString());
            }
            throw new IOException("could not set the date of " + path);
        }
        return path;
    }

    /**
     * The volume that file lives on.
     *
     * <p>The {@link FileStore} that comes out answers the three spaces for real --total, usable and
     * unallocated-- and gives `"unknown"` as the type, which is what the JDK itself answers when it
     * cannot determine one. See {@link KajiFileStore} on what is known and what is not.
     *
     * @throws NullPointerException if `path` is `null`
     * @throws IOException if the file does not exist or if the volume could not be read
     */
    public static FileStore getFileStore(Path path) throws IOException {
        if (path == null) {
            throw new NullPointerException("path");
        }
        String pathOf = path.toAbsolutePath().toString();
        // That the file exists is checked **before** asking about the volume, and not after: the
        // space API answers all the same for a path that does not exist --the volume is enough for
        // it-- and `getFileStore` of a non-existent file has to fail, not return its directory's
        // volume.
        if (jdk.internal.io.Fs.stat(pathOf) == 0) {
            throw new NoSuchFileException(path.toString());
        }
        return new KajiFileStore(pathOf, KajiFileStore.volumeName(pathOf));
    }
}
