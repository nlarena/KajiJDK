package java.lang;

// KajiLibrary's java.lang.NoClassDefFoundError — the loader could not find the .class
// for a type the running code refers to.
public class NoClassDefFoundError extends LinkageError {

    public NoClassDefFoundError() {
    }

    public NoClassDefFoundError(String message) {
        super(message);
    }
}
