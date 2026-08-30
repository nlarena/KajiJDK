package java.security;

// KajiLibrary's java.security.Guard -- an object that guards access to another. The classic
// implementor is Permission (its checkGuard consults the security manager). One method.
public interface Guard {

    /** Determines whether to allow access to the guarded object {@code object}. */
    void checkGuard(Object object) throws SecurityException;
}
