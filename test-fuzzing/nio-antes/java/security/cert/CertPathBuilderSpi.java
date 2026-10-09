package java.security.cert;

import java.security.InvalidAlgorithmParameterException;

// What a provider has to write in order to offer a path builder.
//
// `engineGetRevocationChecker()` throws `UnsupportedOperationException` by default and **it is like
// that in the JDK**: it was added in Java 8 and giving it a base implementation that does nothing
// would have broken the providers that already existed. A provider that knows how to check
// revocation overrides it.
public abstract class CertPathBuilderSpi {

    public CertPathBuilderSpi() {
    }

    // Builds a path with these parameters.
    public abstract CertPathBuilderResult engineBuild(CertPathParameters params)
        throws CertPathBuilderException, InvalidAlgorithmParameterException;

    public CertPathChecker engineGetRevocationChecker() {
        throw new UnsupportedOperationException();
    }
}
