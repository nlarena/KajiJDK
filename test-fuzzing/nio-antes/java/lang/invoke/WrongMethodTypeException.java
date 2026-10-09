package java.lang.invoke;

// A method handle was invoked with a type it cannot accept. It is a `RuntimeException` and not a
// checked one for a reason worth noting: the whole point of a method handle is that its type is
// checked at LINK time, so reaching this at run time means an adaptation went wrong, not that the
// caller forgot to handle a foreseeable case.
public class WrongMethodTypeException extends RuntimeException {

    public WrongMethodTypeException() {
        super();
    }

    public WrongMethodTypeException(String message) {
        super(message);
    }
}
