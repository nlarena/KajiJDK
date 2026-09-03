package java.security.cert;

import java.security.InvalidAlgorithmParameterException;

// Lo que un proveedor tiene que escribir para ofrecer un validador de caminos.
//
// El contrato de `engineValidate` es el mismo que el de la clase de arriba: si el camino no vale,
// **lanza**; si vuelve, vale. Devolver un resultado nunca significa "fallo".
public abstract class CertPathValidatorSpi {

    public CertPathValidatorSpi() {
    }

    public abstract CertPathValidatorResult engineValidate(CertPath certPath,
                                                           CertPathParameters params)
        throws CertPathValidatorException, InvalidAlgorithmParameterException;

    // Ver `CertPathBuilderSpi.engineGetRevocationChecker()`: mismo default y mismo motivo.
    public CertPathChecker engineGetRevocationChecker() {
        throw new UnsupportedOperationException();
    }
}
