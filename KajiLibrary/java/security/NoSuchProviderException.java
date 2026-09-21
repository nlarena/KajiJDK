package java.security;

// There is no provider registered with that name.
//
// It is different from `NoSuchAlgorithmException`: here the problem is that the provider **does not
// exist**, not that it exists and does not know the algorithm. That is why it carries two
// constructors and not four — it is never built wrapping another cause, because there is no
// operation that failed underneath: it is a search in a table that found nothing.
public class NoSuchProviderException extends GeneralSecurityException {

    public NoSuchProviderException() {
        super();
    }

    public NoSuchProviderException(String message) {
        super(message);
    }
}
