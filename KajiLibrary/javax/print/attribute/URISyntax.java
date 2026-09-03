package javax.print.attribute;

import java.io.Serializable;
import java.net.URI;

// La clase de sintaxis de los atributos cuyo valor es una URI. Delega todo -- igualdad, hash y
// texto -- en la URI, que ya es un valor inmutable bien portado.
public abstract class URISyntax implements Serializable, Cloneable {

    private static final long serialVersionUID = -7842661210486401678L;

    private URI uri;

    protected URISyntax(URI uri) {
        this.uri = verify(uri);
    }

    private static URI verify(URI uri) {
        if (uri == null) {
            throw new NullPointerException(" uri is null");
        }
        return uri;
    }

    public URI getURI() {
        return this.uri;
    }

    public int hashCode() {
        return this.uri.hashCode();
    }

    public boolean equals(Object object) {
        if (!(object instanceof URISyntax)) {
            return false;
        }
        return this.uri.equals(((URISyntax) object).uri);
    }

    public String toString() {
        return this.uri.toString();
    }
}
