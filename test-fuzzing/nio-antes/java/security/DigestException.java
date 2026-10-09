package java.security;

// A failure in a digest operation.
//
// In practice a single method throws it, `MessageDigest.digest(byte[], int, int)`, when the buffer
// it is given has no room for the result. The other roads of the digest cannot fail: feeding bytes
// to a hash function has no failure mode.
public class DigestException extends GeneralSecurityException {

    public DigestException() {
        super();
    }

    public DigestException(String message) {
        super(message);
    }

    public DigestException(String message, Throwable cause) {
        super(message, cause);
    }

    public DigestException(Throwable cause) {
        super(cause);
    }
}
