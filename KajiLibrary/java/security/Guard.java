package java.security;

// An object that can watch over the access to another.
//
// It is the smallest interface of the package and the one that explains its shape: `Permission`
// implements it, so **a permission is its own guard**. A `GuardedObject` does not need to know what
// kind of control is asked of it — it passes the object to the guard and the guard decides.
public interface Guard {

    // Determines whether access to `object` is allowed. Throws SecurityException if not.
    void checkGuard(Object object) throws SecurityException;
}
