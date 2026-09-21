package javax.security.auth;

/**
 * KajiLibrary's javax.security.auth.DestroyFailedException -- a credential could not be erased.
 *
 * <p>It is worth saying when it is right to throw it, because the common case is the other way
 * round. An object that keeps its secret in its own memory erases it and that is it. This exception
 * is for the one that **cannot**: a key living inside a hardware module, or one the system copied
 * to a place the library does not control. There the only honest answer is to warn that the secret
 * still exists, and not to return silently as if it had been erased.
 */
public class DestroyFailedException extends Exception {

    private static final long serialVersionUID = -7790152857890440085L;

    public DestroyFailedException() {
        super();
    }

    public DestroyFailedException(String msg) {
        super(msg);
    }
}
