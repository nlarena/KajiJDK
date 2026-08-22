package java.lang.reflect;

/**
 * Thrown when the {@code MethodParameters} attribute of a class file is malformed.
 *
 * <p>That attribute is optional — it is what {@code javac -parameters} writes so that reflection can
 * report a parameter's real name instead of {@code arg0}. Being optional and unverified, it is the
 * kind of metadata that can be wrong without anything else failing first.
 */
public class MalformedParametersException extends RuntimeException {

    /**
     * Creates an exception with no detail message.
     */
    public MalformedParametersException() {
        super();
    }

    /**
     * Creates an exception with the given detail message.
     *
     * @param reason the detail message
     */
    public MalformedParametersException(String reason) {
        super(reason);
    }
}
