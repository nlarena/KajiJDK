package java.lang;

// Thrown when resolution needs a class that can't be loaded (not on the classpath).
public class NoClassDefFoundError extends LinkageError {
    public NoClassDefFoundError() {
    }
}
