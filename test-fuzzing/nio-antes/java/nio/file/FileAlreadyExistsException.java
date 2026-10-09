package java.nio.file;

// The file already exists and the operation asked for it to be created.
//
// `Files.createFile`, `Files.createDirectory` and the copies without `REPLACE_EXISTING` throw it.
// In KajiJDK the check is a prior `stat`, not an atomic creation -- see `Files.createFile`'s note.
public class FileAlreadyExistsException extends FileSystemException {

    private static final long serialVersionUID = 7579540934498831181L;

    /** @param file the file that was already there, or `null` */
    public FileAlreadyExistsException(String file) {
        super(file);
    }

    /** @param file the file; `other` the other; `reason` the reason. Any may be `null`. */
    public FileAlreadyExistsException(String file, String other, String reason) {
        super(file, other, reason);
    }
}
