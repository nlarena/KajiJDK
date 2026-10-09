package java.lang;

// KajiLibrary's java.lang.CloneNotSupportedException — Object.clone() was called on an object whose
// class does not implement Cloneable. The design is famously odd: Cloneable is a marker interface
// with no clone() method, so "can I be cloned?" is answered at run time by this checked exception
// rather than by the type system.
public class CloneNotSupportedException extends Exception {

    public CloneNotSupportedException() {
    }

    public CloneNotSupportedException(String message) {
        super(message);
    }
}
