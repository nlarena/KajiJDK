package java.security.cert;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;

// It validates an already assembled certification path against a set of anchors.
//
// It is the operation all the trust of TLS and of code signatures rests on, and its contract is the
// one to be clear about: **`validate` does not return a boolean**. If the path is not valid, it
// throws `CertPathValidatorException`, which also says in which link and why. If it returns, it
// returns useful data. Writing `try { v.validate(p, ps); } catch (Exception e) {}` is not handling
// an error: it is accepting any chain.
//
// A KajiLibrary subset: **there is no registered provider**, so the three overloads of
// `getInstance` always throw `NoSuchAlgorithmException`. The reason is the same as in
// `CertPathBuilder`: validating PKIX asks for verifying signatures and comparing X.500 names, and
// neither of the two things is implemented. A validator that said yes without verifying is the
// worst possible hole in this library, so none is registered.
public class CertPathValidator {

    private final CertPathValidatorSpi validatorSpi;
    private final Provider provider;
    private final String algorithm;

    protected CertPathValidator(CertPathValidatorSpi validatorSpi, Provider provider,
                                String algorithm) {
        this.validatorSpi = validatorSpi;
        this.provider = provider;
        this.algorithm = algorithm;
    }

    public static CertPathValidator getInstance(String algorithm) throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        Provider[] provs = Security.getProviders();
        int i = 0;
        while (i < provs.length) {
            Provider.Service s = provs[i].getService("CertPathValidator", algorithm);
            if (s != null) {
                return build(s, algorithm);
            }
            i = i + 1;
        }
        throw new NoSuchAlgorithmException(algorithm + " CertPathValidator not available");
    }

    public static CertPathValidator getInstance(String algorithm, String provider)
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

    public static CertPathValidator getInstance(String algorithm, Provider provider)
            throws NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        Provider.Service s = provider.getService("CertPathValidator", algorithm);
        if (s == null) {
            throw new NoSuchAlgorithmException(
                "no such algorithm: " + algorithm + " for provider " + provider.getName());
        }
        return build(s, algorithm);
    }

    private static CertPathValidator build(Provider.Service s, String algorithm)
            throws NoSuchAlgorithmException {
        Object o = s.newInstance(null);
        if (!(o instanceof CertPathValidatorSpi)) {
            throw new NoSuchAlgorithmException(
                "class configured for CertPathValidator is not a CertPathValidatorSpi: "
                + s.getClassName());
        }
        return new CertPathValidator((CertPathValidatorSpi) o, s.getProvider(), algorithm);
    }

    public final Provider getProvider() {
        return this.provider;
    }

    public final String getAlgorithm() {
        return this.algorithm;
    }

    // It validates the path. **If it returns, it was valid; if not, it throws.** See the note of
    // the class.
    public final CertPathValidatorResult validate(CertPath certPath, CertPathParameters params)
            throws CertPathValidatorException, InvalidAlgorithmParameterException {
        return this.validatorSpi.engineValidate(certPath, params);
    }

    // The default algorithm, from the property `certpathvalidator.type`. "PKIX" if it is not set.
    public static final String getDefaultType() {
        String t = Security.getProperty("certpathvalidator.type");
        if (t == null) {
            return "PKIX";
        }
        return t;
    }

    public final CertPathChecker getRevocationChecker() {
        return this.validatorSpi.engineGetRevocationChecker();
    }
}
