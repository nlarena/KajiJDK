package java.security.cert;

// Un criterio para elegir certificados de un `CertStore`.
//
// La contraparte de `CRLSelector`, y redeclara `clone()` por lo mismo: el selector es estado
// mutable del que el store se apropia, y tiene que poder copiarlo.
public interface CertSelector extends Cloneable {

    boolean match(Certificate cert);

    Object clone();
}
