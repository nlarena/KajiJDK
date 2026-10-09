package java.nio.file;

// There is no provider installed for the scheme asked for.
//
// KajiJDK has exactly one --`file`'s-- and there is no service mechanism that installs others, so
// any other scheme ends up here.
public class ProviderNotFoundException extends RuntimeException {

    private static final long serialVersionUID = -1880012509822920354L;

    /** With no message. */
    public ProviderNotFoundException() {
    }

    /** @param msg the detail */
    public ProviderNotFoundException(String msg) {
        super(msg);
    }
}
