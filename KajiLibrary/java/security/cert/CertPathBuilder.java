package java.security.cert;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;

// It builds a certification path: given a certificate and a set of anchors, it looks for the chain
// that joins them.
//
// It is the piece that is needed when the chain does **not** come complete, which is the normal
// thing: a TLS server usually sends its certificate and some intermediates, but not all of them,
// and the rest has to be fetched from the configured `CertStore`s. Building is a search with
// backtracking, and that is why `PKIXBuilderParameters` has a maximum length.
//
// The path it returns is validated already. It is not a shortcut of the API: looking for a chain
// implies verifying each link in order to know whether it serves, so separating it into two steps
// would duplicate the work.
//
// A KajiLibrary subset: **there is no registered provider**, so the three overloads of
// `getInstance` always throw `NoSuchAlgorithmException`. Implementing PKIX honestly asks for
// verifying signatures —RSA, ECDSA— and comparing X.500 names, and neither of the two things is
// written in this library. A builder that returned unverified chains would be exactly the hole this
// package has to avoid.
public class CertPathBuilder {

    private final CertPathBuilderSpi builderSpi;
    private final Provider provider;
    private final String algorithm;

    protected CertPathBuilder(CertPathBuilderSpi builderSpi, Provider provider, String algorithm) {
        this.builderSpi = builderSpi;
        this.provider = provider;
        this.algorithm = algorithm;
    }

    public static CertPathBuilder getInstance(String algorithm) throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        Provider[] provs = Security.getProviders();
        int i = 0;
        while (i < provs.length) {
            Provider.Service s = provs[i].getService("CertPathBuilder", algorithm);
            if (s != null) {
                return build(s, algorithm);
            }
            i = i + 1;
        }
        throw new NoSuchAlgorithmException(algorithm + " CertPathBuilder not available");
    }

    public static CertPathBuilder getInstance(String algorithm, String provider)
            throws NoSuchAlgorithmException, NoSuchProviderException {
        if (provider == null || provider.isEmpty()) {
            throw new IllegalArgumentException("missing provider");
        }
        Provider p = Security.getProvider(provider);
        if (p == null) {
            throw new NoSuchProviderException("no such provider: " + provider);
        }
        return getInstance(algorithm, p);
    }

    public static CertPathBuilder getInstance(String algorithm, Provider provider)
            throws NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        Provider.Service s = provider.getService("CertPathBuilder", algorithm);
        if (s == null) {
            throw new NoSuchAlgorithmException(
                "no such algorithm: " + algorithm + " for provider " + provider.getName());
        }
        return build(s, algorithm);
    }

    private static CertPathBuilder build(Provider.Service s, String algorithm)
            throws NoSuchAlgorithmException {
        Object o = s.newInstance(null);
        if (!(o instanceof CertPathBuilderSpi)) {
            throw new NoSuchAlgorithmException(
                "class configured for CertPathBuilder is not a CertPathBuilderSpi: "
                + s.getClassName());
        }
        return new CertPathBuilder((CertPathBuilderSpi) o, s.getProvider(), algorithm);
    }

    public final Provider getProvider() {
        return this.provider;
    }

    public final String getAlgorithm() {
        return this.algorithm;
    }

    // It looks for and validates a path. If there is none, it throws: the absence of a path is not
    // returned as null.
    public final CertPathBuilderResult build(CertPathParameters params)
            throws CertPathBuilderException, InvalidAlgorithmParameterException {
        return this.builderSpi.engineBuild(params);
    }

    // The default algorithm, from the property `certpathbuilder.type`. "PKIX" if it is not set.
    public static final String getDefaultType() {
        String t = Security.getProperty("certpathbuilder.type");
        if (t == null) {
            return "PKIX";
        }
        return t;
    }

    // The revocation checker of this provider, to configure it before building. It throws
    // `UnsupportedOperationException` if the provider does not offer it.
    public final CertPathChecker getRevocationChecker() {
        return this.builderSpi.engineGetRevocationChecker();
    }
}
