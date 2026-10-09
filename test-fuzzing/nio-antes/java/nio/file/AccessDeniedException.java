package java.nio.file;

// The file is there but the process has no permission for what was asked.
//
// It is chosen over `NoSuchFileException` when `stat` says the path **exists** but does not bring
// the read or write flag that was needed. Without that prior check the two would be
// indistinguishable from the natives, and guessing would send the reader looking for the problem in
// the wrong place.
public class AccessDeniedException extends FileSystemException {

    private static final long serialVersionUID = 4943049599949219617L;

    /** @param file the file that could not be accessed, or `null` */
    public AccessDeniedException(String file) {
        super(file);
    }

    /** @param file the file; `other` the other; `reason` the reason. Any may be `null`. */
    public AccessDeniedException(String file, String other, String reason) {
        super(file, other, reason);
    }
}
