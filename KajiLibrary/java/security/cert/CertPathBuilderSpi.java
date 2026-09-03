package java.security.cert;

import java.security.InvalidAlgorithmParameterException;

// Lo que un proveedor tiene que escribir para ofrecer un constructor de caminos.
//
// `engineGetRevocationChecker()` tira `UnsupportedOperationException` por default y **asi es en el
// JDK**: se agrego en Java 8 y darle una implementacion base que no hace nada habria roto a los
// proveedores que ya existian. Un proveedor que sepa chequear revocacion lo sobreescribe.
public abstract class CertPathBuilderSpi {

    public CertPathBuilderSpi() {
    }

    // Construye un camino con estos parametros.
    public abstract CertPathBuilderResult engineBuild(CertPathParameters params)
        throws CertPathBuilderException, InvalidAlgorithmParameterException;

    public CertPathChecker engineGetRevocationChecker() {
        throw new UnsupportedOperationException();
    }
}
