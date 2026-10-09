package java.security.cert;

import java.security.InvalidAlgorithmParameterException;

// What a provider has to write in order to offer a path validator.
//
// The contract of `engineValidate` is the same as that of the class above: if the path is not
// valid, it **throws**; if it returns, it is valid. Returning a result never means "it failed".
public abstract class CertPathValidatorSpi {

    public CertPathValidatorSpi() {
    }

    public abstract CertPathValidatorResult engineValidate(CertPath certPath,
                                                           CertPathParameters params)
        throws CertPathValidatorException, InvalidAlgorithmParameterException;

    // See `CertPathBuilderSpi.engineGetRevocationChecker()`: the same default and the same reason.
    public CertPathChecker engineGetRevocationChecker() {
        throw new UnsupportedOperationException();
    }
}
