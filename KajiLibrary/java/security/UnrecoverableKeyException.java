package java.security;

// The particular case of `UnrecoverableEntryException` in which the entry that could not be
// recovered is a key.
public class UnrecoverableKeyException extends UnrecoverableEntryException {

    public UnrecoverableKeyException() {
        super();
    }

    public UnrecoverableKeyException(String message) {
        super(message);
    }
}
