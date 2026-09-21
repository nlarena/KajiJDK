package java.security.cert;

import java.util.Collection;
import java.util.Collections;

// The parameters of the simplest `CertStore`: a collection in memory.
//
// The collection **is not copied**, and it is not an oversight —it is what the JDK documents and it
// has to be respected—. It is the only way of having a store that grows: whoever builds it can go
// on adding certificates to the collection after creating the store, and they appear in the
// following queries. The counterpart is that the synchronisation is left on the caller's side, and
// that is why `clone()` does not copy either: the two objects point at the same collection on
// purpose.
public class CollectionCertStoreParameters implements CertStoreParameters {

    private final Collection<?> coll;

    public CollectionCertStoreParameters(Collection<?> collection) {
        if (collection == null) {
            throw new NullPointerException();
        }
        this.coll = collection;
    }

    // An empty and immutable store: it serves as a starting point when the real source is defined
    // afterwards.
    public CollectionCertStoreParameters() {
        this.coll = Collections.emptySet();
    }

    public Collection<?> getCollection() {
        return this.coll;
    }

    // Shallow copy: the collection is shared, see above.
    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            // It cannot happen: the class implements Cloneable by way of CertStoreParameters.
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
