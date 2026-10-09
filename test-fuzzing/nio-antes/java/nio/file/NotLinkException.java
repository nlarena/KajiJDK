package java.nio.file;

// A symbolic link was expected and the path points at something else.
//
// KajiJDK never throws it. This note used to say `Files.readSymbolicLink` --the only method that
// would-- does not exist; it is declared, and it throws `UnsupportedOperationException` instead,
// because there is no native that reads links. The distinction is the one that method's javadoc
// makes: this exception would say "this path is not a link", and what does not exist is the
// operation.
public class NotLinkException extends FileSystemException {

    private static final long serialVersionUID = -388655596416518021L;

    /** @param file the path that was not a link, or `null` */
    public NotLinkException(String file) {
        super(file);
    }

    /** @param file the file; `other` the other; `reason` the reason. Any may be `null`. */
    public NotLinkException(String file, String other, String reason) {
        super(file, other, reason);
    }
}
