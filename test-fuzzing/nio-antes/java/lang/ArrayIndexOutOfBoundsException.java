package java.lang;

/**
 * KajiLibrary's java.lang.ArrayIndexOutOfBoundsException -- thrown when an array is indexed
 * outside {@code [0, length)}.
 *
 * <p>It extends {@link IndexOutOfBoundsException} and that is the load-bearing part of this file.
 * It used to extend {@code RuntimeException} directly, which compiles, passes every signature
 * check, and quietly breaks the most common way anyone handles a bounds error:
 * {@code catch (IndexOutOfBoundsException e)} did not fire for an array. The gate could not see
 * it -- the members were all correct -- and only a probe that CAUGHT one by its supertype did.
 */
public class ArrayIndexOutOfBoundsException extends IndexOutOfBoundsException {

    /** With no detail message. */
    public ArrayIndexOutOfBoundsException() {
    }

    /**
     * With a detail message.
     *
     * @param message what went wrong
     */
    public ArrayIndexOutOfBoundsException(String message) {
        super(message);
    }

    /**
     * For the offending index, which becomes the message.
     *
     * @param index the index that was out of range
     */
    public ArrayIndexOutOfBoundsException(int index) {
        super("Array index out of range: " + index);
    }
}
