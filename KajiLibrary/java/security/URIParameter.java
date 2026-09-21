package java.security;

import java.net.URI;

// The parameters of a `Policy` that live in a URI: "the policy is in this file".
//
// A KajiLibrary subset: in the JDK it also implements
// `javax.security.auth.login.Configuration.Parameters`, because the same object serves to configure
// a policy of authorisation and one of authentication. That package does not exist in this library,
// so only the `java.security` half is declared.
public class URIParameter implements Policy.Parameters {

    private final URI uri;

    public URIParameter(URI uri) {
        if (uri == null) {
            throw new NullPointerException("invalid null URI");
        }
        this.uri = uri;
    }

    public URI getURI() {
        return this.uri;
    }
}
