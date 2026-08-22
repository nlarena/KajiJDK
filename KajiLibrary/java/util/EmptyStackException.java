package java.util;

// Thrown when a Stack operation is asked for an element and the stack has none.
public class EmptyStackException extends RuntimeException {

    public EmptyStackException() {
        super();
    }
}
