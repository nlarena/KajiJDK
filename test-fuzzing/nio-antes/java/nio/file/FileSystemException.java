package java.nio.file;

import java.io.IOException;

// The base of this package's exceptions: a failure over one or two files.
//
// **Why two files and not one.** `copy` and `move` fail over a pair, and knowing which of the two
// was the problem is half the diagnosis. `getFile()` is the source and `getOtherFile()` the target;
// in single-file operations the second is left `null`.
//
// **Why the reason is kept apart from the message.** `getMessage()` builds the text by joining the
// three parts, but whoever catches the exception usually wants the raw path --to retry, or to show
// it in another language-- and taking it out of the formatted message would be fragile.
public class FileSystemException extends IOException {

    private static final long serialVersionUID = -3055425747967319812L;

    private final String file;
    private final String other;
    private final String reason;

    /**
     * A failure over a single file, with no explanation.
     *
     * @param file the file, or `null` if it is not known
     */
    public FileSystemException(String file) {
        super((String) null);
        this.file = file;
        this.other = null;
        this.reason = null;
    }

    /**
     * A failure with the full detail.
     *
     * @param file the file, or `null`
     * @param other the other file, or `null`
     * @param reason why it failed, or `null`
     */
    public FileSystemException(String file, String other, String reason) {
        super((String) null);
        this.file = file;
        this.other = other;
        this.reason = reason;
    }

    /** The file, or `null`. */
    public String getFile() {
        return this.file;
    }

    /** The other file, or `null`. */
    public String getOtherFile() {
        return this.other;
    }

    /** The reason, or `null`. */
    public String getReason() {
        return this.reason;
    }

    /**
     * The assembled message: `file -> other: reason`, skipping the parts that are missing.
     *
     * <p>It is computed here and not in the constructor because the three parts are final: the
     * result is always the same and there is no state to keep.
     */
    public String getMessage() {
        if (this.file == null && this.other == null) {
            return this.reason;
        }
        StringBuilder sb = new StringBuilder();
        if (this.file != null) {
            sb.append(this.file);
        }
        if (this.other != null) {
            sb.append(" -> ");
            sb.append(this.other);
        }
        if (this.reason != null) {
            sb.append(": ");
            sb.append(this.reason);
        }
        return sb.toString();
    }
}
