package java.lang;

// Thrown when a referenced method doesn't exist on the (loadable) target class —
// the classic "recompiled one class but not its callers" binary-incompatibility.
public class NoSuchMethodError extends LinkageError {
    public NoSuchMethodError() {
    }
}
