package java.lang;

// KajiLibrary's java.lang.IncompatibleClassChangeError — a class was compiled against a
// version of another class that no longer matches: the field became static, the method
// became abstract, the class became an interface. This is the whole reason separate
// compilation needs link-time checks — javac was right when it ran, and the world moved.
public class IncompatibleClassChangeError extends LinkageError {

    public IncompatibleClassChangeError() {
    }

    public IncompatibleClassChangeError(String message) {
        super(message);
    }
}
