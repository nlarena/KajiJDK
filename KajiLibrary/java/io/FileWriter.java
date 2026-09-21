package java.io;

import java.nio.charset.Charset;

// KajiLibrary's java.io.FileWriter -- characters written to a file.
//
// `FileReader`'s mirror: it is `new OutputStreamWriter(new FileOutputStream(f, append), cs)` under
// a short name.
//
// **It has to be closed.** It inherits that from `FileOutputStream`, and here it hurts more than
// there because whoever writes text usually trusts that "it is written already": a `FileWriter`
// abandoned without `close()` leaves the file empty or half done. See `FileOutputStream`'s note.
//
// The `append` parameter decides between appending at the end and **truncating**, and truncating is
// what happens by default: opening a file with `new FileWriter(f)` erases whatever was there even
// if nothing is written afterwards. It is the JDK's behaviour, and it is the commonest source of
// data loss in the whole package.
public class FileWriter extends OutputStreamWriter {

    /**
     * Opens `fileName` with the default charset, **truncating** whatever was there.
     *
     * @throws IOException if it is a directory, or cannot be written to
     */
    public FileWriter(String fileName) throws IOException {
        super(new FileOutputStream(fileName));
    }

    /**
     * Opens `fileName` with the default charset.
     *
     * @param append whether what is written is appended at the end instead of replacing the
     *     contents
     * @throws IOException if it is a directory, or cannot be written to
     */
    public FileWriter(String fileName, boolean append) throws IOException {
        super(new FileOutputStream(fileName, append));
    }

    /**
     * Opens `file` with the default charset, **truncating** whatever was there.
     *
     * @throws IOException if it is a directory, or cannot be written to
     */
    public FileWriter(File file) throws IOException {
        super(new FileOutputStream(file));
    }

    /**
     * Opens `file` with the default charset.
     *
     * @param append whether what is written is appended at the end instead of replacing the
     *     contents
     * @throws IOException if it is a directory, or cannot be written to
     */
    public FileWriter(File file, boolean append) throws IOException {
        super(new FileOutputStream(file, append));
    }

    /** Writes by descriptor. This library does not model descriptors; see
     * `FileOutputStream`. */
    public FileWriter(FileDescriptor fd) {
        super(new FileOutputStream(fd));
    }

    /**
     * Opens `fileName` with the given charset, **truncating** whatever was there.
     *
     * @throws IOException if it is a directory, or cannot be written to
     */
    public FileWriter(String fileName, Charset charset) throws IOException {
        super(new FileOutputStream(fileName), charset);
    }

    /**
     * Opens `fileName` with the given charset.
     *
     * @param append whether what is written is appended at the end instead of replacing the
     *     contents
     * @throws IOException if it is a directory, or cannot be written to
     */
    public FileWriter(String fileName, Charset charset, boolean append) throws IOException {
        super(new FileOutputStream(fileName, append), charset);
    }

    /**
     * Opens `file` with the given charset, **truncating** whatever was there.
     *
     * @throws IOException if it is a directory, or cannot be written to
     */
    public FileWriter(File file, Charset charset) throws IOException {
        super(new FileOutputStream(file), charset);
    }

    /**
     * Opens `file` with the given charset.
     *
     * @param append whether what is written is appended at the end instead of replacing the
     *     contents
     * @throws IOException if it is a directory, or cannot be written to
     */
    public FileWriter(File file, Charset charset, boolean append) throws IOException {
        super(new FileOutputStream(file, append), charset);
    }
}
