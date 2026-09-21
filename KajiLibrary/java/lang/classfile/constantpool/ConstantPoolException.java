package java.lang.classfile.constantpool;

// What is thrown when a pool entry does not do what was asked of it: an index out of range, a tag
// that does not match the requested type, or a broken internal reference. It inherits from
// `IllegalArgumentException`, just as in the JDK.
public class ConstantPoolException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    /** With neither message nor cause. */
    public ConstantPoolException() {
        super();
    }

    /** With a message. */
    public ConstantPoolException(String message) {
        super(message);
    }

    /** With a message and a cause. */
    public ConstantPoolException(String message, Throwable cause) {
        super(message, cause);
    }

    /** With a cause. */
    public ConstantPoolException(Throwable cause) {
        super(cause);
    }
}
