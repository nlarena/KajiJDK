package java.lang.reflect;

/**
 * Thrown when a reflective method encounters a parameterized type whose signature is semantically
 * impossible — for instance one whose argument count does not match the raw type's parameter count.
 *
 * <p>It signals a corrupt or hand-edited class file rather than a programming error at the call
 * site: the generic type information lives in a {@code Signature} attribute that the JVM never
 * validates, so nothing before this point would have noticed.
 */
public class MalformedParameterizedTypeException extends RuntimeException {

    /**
     * Creates an exception with no detail message.
     */
    public MalformedParameterizedTypeException() {
        super();
    }

    /**
     * Creates an exception with the given detail message.
     *
     * @param message the detail message
     */
    public MalformedParameterizedTypeException(String message) {
        super(message);
    }
}
