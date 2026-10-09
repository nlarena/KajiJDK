package java.lang;

// KajiLibrary's java.lang.WrongThreadException — an object confined to one thread was used from
// another. Thread confinement is a discipline the type system cannot express, so it is enforced
// at run time by the object checking Thread.currentThread() against its owner.
public class WrongThreadException extends RuntimeException {

    public WrongThreadException() {
    }

    public WrongThreadException(String message) {
        super(message);
    }

    public WrongThreadException(String message, Throwable cause) {
        super(message, cause);
    }

    public WrongThreadException(Throwable cause) {
        super(cause);
    }
}
