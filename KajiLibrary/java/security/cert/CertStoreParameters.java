package java.security.cert;

// Los parametros de inicializacion de un `CertStore`: de donde saca los certificados y las CRLs.
//
// Igual que `CertPathParameters`, es marcadora con `clone()`, y la copia es por lo mismo: el store
// se queda con los parametros y no puede permitir que cambien por atras.
public interface CertStoreParameters extends Cloneable {

    Object clone();
}
