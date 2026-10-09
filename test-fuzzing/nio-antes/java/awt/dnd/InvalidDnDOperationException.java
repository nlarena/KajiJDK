package java.awt.dnd;

/**
 * Something of drag and drop was done at a moment when it did not fit.
 *
 * <p>It inherits from {@code IllegalStateException} and that says it all: it is not a problem of
 * the arguments but of the **state**. Asking for the data before accepting the drop, or accepting
 * twice, or dropping when there is no drag under way.
 *
 * <p>That it is not checked is deliberate: they are sequence errors of the program, not situations
 * that have to be caught.
 */
public class InvalidDnDOperationException extends IllegalStateException {

    private static final long serialVersionUID = -6062568741193956678L;

    /** With the default message. */
    public InvalidDnDOperationException() {
        super("The operation requested cannot be performed by the DnD system since it is not in "
                + "the appropriate state");
    }

    /** With the given explanation. */
    public InvalidDnDOperationException(String msg) {
        super(msg);
    }
}
