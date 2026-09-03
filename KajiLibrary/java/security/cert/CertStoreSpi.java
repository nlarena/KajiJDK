package java.security.cert;

import java.security.InvalidAlgorithmParameterException;
import java.util.Collection;

// Lo que un proveedor tiene que escribir para ofrecer un repositorio de certificados.
//
// El constructor recibe los parametros y puede rechazarlos, que es distinto del resto de los SPI
// del area: aca no hay un `engineInit` aparte porque un store sin fuente no tiene ningun estado
// util al que llegar.
//
// Los dos metodos devuelven **colecciones, posiblemente vacias, nunca null**, y eso es del
// contrato: "no encontre nada" es un resultado normal en un store, no un error.
public abstract class CertStoreSpi {

    public CertStoreSpi(CertStoreParameters params) throws InvalidAlgorithmParameterException {
    }

    public abstract Collection<? extends Certificate> engineGetCertificates(CertSelector selector)
        throws CertStoreException;

    public abstract Collection<? extends CRL> engineGetCRLs(CRLSelector selector)
        throws CertStoreException;
}
