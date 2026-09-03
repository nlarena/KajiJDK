package java.security.cert;

// Un criterio para elegir CRLs de un `CertStore`.
//
// Extiende `Cloneable` y **redeclara** `clone()` como publico, que es lo que `Cloneable` sola no
// hace. La razon es del contrato: un `CertStore` se queda con el selector que le pasan, y si no
// pudiera copiarlo, quien se lo dio podria cambiarlo despues y alterar por atras que devuelve una
// consulta.
public interface CRLSelector extends Cloneable {

    // Si esta CRL cumple el criterio.
    boolean match(CRL crl);

    Object clone();
}
