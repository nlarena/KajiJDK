package java.io;

// KajiLibrary's java.io.FileDescriptor -- an opaque handle to an open file/socket/pipe. KajiJDK
// does no real file I/O, so only the three standard streams are modelled as valid handles (fd
// 0/1/2); a FileDescriptor built with the public constructor is invalid until the platform fills
// it in, which here never happens.
public final class FileDescriptor {

    // The underlying handle: 0/1/2 for the standard streams, -1 ("not a handle") otherwise.
    private int fd;

    /** A handle to standard input. */
    public static final FileDescriptor in = new FileDescriptor(0);

    /** A handle to standard output. */
    public static final FileDescriptor out = new FileDescriptor(1);

    /** A handle to standard error. */
    public static final FileDescriptor err = new FileDescriptor(2);

    private FileDescriptor(int fd) {
        this.fd = fd;
    }

    public FileDescriptor() {
        this.fd = -1;
    }

    /** Whether this represents a valid, open handle. */
    public boolean valid() {
        return this.fd != -1;
    }

    /** Forces buffered data to the device. KajiJDK buffers nothing at this level, so it is a no-op. */
    public void sync() throws SyncFailedException {
    }
}
