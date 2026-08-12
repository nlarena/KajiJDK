package java.lang;

// Thrown by Object.clone() when the receiver's class does not implement the
// Cloneable marker interface — cloning is opt-in, and this is the refusal. A
// *checked* exception (extends Exception, not RuntimeException): callers of
// clone() must catch or declare it, which is what makes the opt-in visible in
// source code. (In the real JDK it extends Exception; same here.)
public class CloneNotSupportedException extends Exception {
    public CloneNotSupportedException(String message) {
        super(message);
    }

    public CloneNotSupportedException() {
    }
}
