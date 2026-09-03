package java.security.cert;

import java.util.Collection;
import java.util.Collections;

// Los parametros del `CertStore` mas simple: una coleccion en memoria.
//
// La coleccion **no se copia**, y no es un descuido —es lo que documenta el JDK y hay que
// respetarlo—. Es la unica forma de tener un store que crezca: quien lo arma puede seguir
// agregando certificados a la coleccion despues de crear el store, y aparecen en las consultas
// siguientes. La contrapartida es que la sincronizacion queda del lado del llamador, y por eso
// `clone()` tampoco copia: los dos objetos apuntan a la misma coleccion a proposito.
public class CollectionCertStoreParameters implements CertStoreParameters {

    private final Collection<?> coll;

    public CollectionCertStoreParameters(Collection<?> collection) {
        if (collection == null) {
            throw new NullPointerException();
        }
        this.coll = collection;
    }

    // Un store vacio e inmutable: sirve como punto de partida cuando la fuente real se define
    // despues.
    public CollectionCertStoreParameters() {
        this.coll = Collections.emptySet();
    }

    public Collection<?> getCollection() {
        return this.coll;
    }

    // Copia superficial: la coleccion se comparte, ver arriba.
    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            // No puede pasar: la clase implementa Cloneable por via de CertStoreParameters.
            throw new InternalError(e.toString(), e);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("CollectionCertStoreParameters: [\n");
        sb.append("  collection: " + this.coll + "\n");
        sb.append("]");
        return sb.toString();
    }
}
