package javax.security.auth;

/**
 * KajiLibrary's javax.security.auth.RefreshFailedException -- a credential could not be renewed.
 *
 * <p>It is the companion of {@link DestroyFailedException} at the other end of a credential's life:
 * one could not be erased, the other could not be renewed. The renewal that matters is that of
 * tickets with an expiry -- a Kerberos ticket, say -- and failing there is not the same as failing
 * to use it: the old credential may keep serving a while longer.
 */
public class RefreshFailedException extends Exception {

    private static final long serialVersionUID = 5058444488565265840L;

    public RefreshFailedException() {
        super();
    }

    public RefreshFailedException(String msg) {
        super(msg);
    }
}
