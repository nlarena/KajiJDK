package java.security.cert;

// The initialisation parameters of a `CertStore`: where it takes the certificates and the CRLs
// from.
//
// Just like `CertPathParameters`, it is a marker with `clone()`, and the copy is for the same
// reason: the store keeps the parameters and cannot allow them to change from behind.
public interface CertStoreParameters extends Cloneable {

    Object clone();
}
