package java.security.cert;

import java.net.URI;

// Los parametros de un `CertStore` que lee de una URI.
//
// Es la version moderna de `LDAPCertStoreParameters`: la misma idea sin atarse a un protocolo. En
// la practica es lo que se usa para seguir la extension AIA de un certificado, que dice por HTTP
// donde bajar el certificado del emisor.
//
// A diferencia de las otras dos clases de parametros, esta es **inmutable** y tiene `equals` y
// `hashCode`: una URI es un valor, no una configuracion mutable, asi que se comporta como tal. El
// tipo de retorno de `clone()` es covariante para que el llamador no tenga que castear.
//
// **No abre ninguna conexion.** Esta clase solo guarda la URI; esta biblioteca no trae ningun
// proveedor de `CertStore` que la sepa resolver.
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

    // Devuelve una instancia **nueva** aunque la clase sea inmutable y `this` alcanzaria. Se hace
    // asi porque es lo que hace el JDK, y la identidad del resultado es observable: hay codigo que
    // compara con `==` para saber si tiene una copia propia.
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
