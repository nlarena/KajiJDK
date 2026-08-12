package java.lang;

// KajiLibrary's java.lang.Error — the Throwable branch for serious problems not meant
// to be caught in normal code (linkage failures, VM errors). Root of the linkage errors.
public class Error extends Throwable {

    public Error() {
    }

    public Error(String message) {
        super(message);
    }
}
