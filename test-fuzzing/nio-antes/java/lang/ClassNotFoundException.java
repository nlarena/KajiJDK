package java.lang;

// KajiLibrary's java.lang.ClassNotFoundException — a class was requested by name at run time and
// no loader could produce it. Checked, unlike NoClassDefFoundError: asking for a class by a
// string is a fallible operation, and the compiler makes you say what happens when it fails.
public class ClassNotFoundException extends ReflectiveOperationException {

    public ClassNotFoundException() {
    }

    public ClassNotFoundException(String message) {
        super(message);
    }

    public ClassNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    // The loading failure that caused this. It predates Throwable's generic cause chain
    // (Java 1.4), which is why the JDK still carries this name; here it is simply the cause.
    public Throwable getException() {
        return getCause();
    }
}
