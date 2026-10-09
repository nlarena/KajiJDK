package java.lang;

// Thrown when a thread is waiting, sleeping, or otherwise occupied and it is
// interrupted. A checked exception — callers of blocking library methods must
// handle or propagate it.
public class InterruptedException extends Exception {

    public InterruptedException() {
        super();
    }

    public InterruptedException(String message) {
        super(message);
    }
}
