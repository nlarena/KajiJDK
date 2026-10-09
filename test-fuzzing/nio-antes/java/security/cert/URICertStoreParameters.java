package java.security.cert;

import java.net.URI;

// The parameters of a `CertStore` that reads from a URI.
//
// It is the modern version of `LDAPCertStoreParameters`: the same idea without being tied to a
// protocol. In practice it is what is used for following the AIA extension of a certificate, which
// says over HTTP where to download the certificate of the issuer from.
//
// Unlike the other two parameter classes, this one is **immutable** and has `equals` and
// `hashCode`: a URI is a value, not a mutable configuration, so it behaves as such. The return type
// of `clone()` is covariant so that the caller does not have to cast.
//
// **It opens no connection.** This class only keeps the URI; this library brings no `CertStore`
// provider that knows how to resolve it.
public final class URICertStoreParameters implements CertStoreParameters {

    private final URI uri;

    public URICertStoreParameters(URI uri) {
        if (uri == null) {
            throw new NullPointerException();
        }
        this.uri = uri;
    }

    public URI getURI() {
        return this.uri;
    }

    // It returns a **new** instance even though the class is immutable and `this` would be enough.
    // It is done this way because it is what the JDK does, and the identity of the result is
    // observable: there is code that compares with `==` to know whether it has a copy of its own.
    @Override
    public URICertStoreParameters clone() {
        return new URICertStoreParameters(this.uri);
    }

    @Override
    public int hashCode() {
        return this.uri.hashCode() * 7;
    }

    @Override
    public boolean equals(Object p) {
        if (p == this) {
            return true;
        }
        if (!(p instanceof URICertStoreParameters)) {
            return false;
        }
        return this.uri.equals(((URICertStoreParameters) p).uri);
    }

    @Override
    public String toString() {
        return "URICertStoreParameters: " + this.uri.toString();
    }
}
