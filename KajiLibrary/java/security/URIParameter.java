package java.security;

import java.net.URI;

// Los parametros de una `Policy` que viven en una URI: "la politica esta en este archivo".
//
// A KajiLibrary subset: en el JDK tambien implementa
// `javax.security.auth.login.Configuration.Parameters`, porque el mismo objeto sirve para
// configurar una politica de autorizacion y una de autenticacion. Ese paquete no existe en esta
// biblioteca, asi que solo se declara la mitad de `java.security`.
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
