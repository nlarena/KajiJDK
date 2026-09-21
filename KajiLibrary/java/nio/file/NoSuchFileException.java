package java.nio.file;

// The file does not exist.
//
// It is the one that fits when `stat` says the path is not there. Note that the native does **not
// tell** "does not exist" from "I have no permission" --it returns zero flags in both cases-- so
// whoever throws it has to have checked existence separately before choosing between this and
// `AccessDeniedException`.
public class NoSuchFileException extends FileSystemException {

    private static final long serialVersionUID = -1390291775875351931L;

    /** @param file the file that is not there, or `null` */
    public NoSuchFileException(String file) {
        super(file);
    }

    /** @param file the file; `other` the other; `reason` the reason. Any may be `null`. */
    public NoSuchFileException(String file, String other, String reason) {
        super(file, other, reason);
    }
}
