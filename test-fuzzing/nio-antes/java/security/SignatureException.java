package java.security;

// A failure in a signature operation.
//
// Careful with the nuance, because it is the classic source of holes: this exception means that the
// operation **could not be carried out** —wrong state, corrupt data, broken provider— and does
// **not** mean "the signature does not validate". A signature that does not validate is a
// `verify()` that returns `false`, with no exception. Code that treats the two things alike, or
// that catches this and carries on, ends up accepting invalid signatures.
public class SignatureException extends GeneralSecurityException {

    public SignatureException() {
        super();
    }

    public SignatureException(String message) {
        super(message);
    }

    public SignatureException(String message, Throwable cause) {
        super(message, cause);
    }

    public SignatureException(Throwable cause) {
        super(cause);
    }
}
