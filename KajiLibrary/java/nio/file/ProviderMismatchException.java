package java.nio.file;

// An object of one provider was mixed with another that is not its own -- a `Path` from a ZIP
// handed to a method of the default filesystem, say.
//
// **It is not an `IOException`.** It inherits from `IllegalArgumentException` because the error is
// in the argument and is detected without touching the disk. In KajiJDK it turns up when a method
// that expects a `Path` of this library is handed a foreign implementation.
public class ProviderMismatchException extends IllegalArgumentException {

    private static final long serialVersionUID = 4990847485741612530L;

    /** With no message. */
    public ProviderMismatchException() {
    }

    /** @param msg the detail */
    public ProviderMismatchException(String msg) {
        super(msg);
    }
}
