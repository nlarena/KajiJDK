package java.nio.file;

// A directory that still has things inside was to be deleted.
//
// KajiJDK really can throw it: the `Fs.delete` native deletes directories **only if they are
// empty** --on purpose, so a `delete()` on the wrong directory is not data loss-- and returns
// `false` when it cannot. `Files.delete` turns that `false` over an existing directory into this
// exception.
public class DirectoryNotEmptyException extends FileSystemException {

    private static final long serialVersionUID = 3056667871802779003L;

    /** @param dir the directory that was not empty, or `null` */
    public DirectoryNotEmptyException(String dir) {
        super(dir);
    }
}
