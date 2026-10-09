package java.security.cert;

import java.io.InputStream;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

// The factory that turns bytes into certificates, CRLs and paths.
//
// It is the only official road for building a `Certificate` from its encoding, and that is why it
// is the piece missing for the rest of the package to be of any use: without it, `X509Certificate`
// is a contract nobody can instantiate.
//
// It is worth being explicit about what it does **not** do, because the name invites confusion:
// reading a certificate does not validate it. `generateCertificate` returns the object and nothing
// else —it does not check the date, it does not verify the signature, it does not look at who
// issued it—. The certificate that comes out of here is a datum, not an assertion. Whoever believes
// that "it parsed fine, so it serves" has a hole.
//
// ===============================================================================================
// A KajiLibrary subset
// ===============================================================================================
//
// **There is no registered factory**, so the three overloads of `getInstance` always throw
// `CertificateException`. An X.509 factory is not registered because reading a certificate is
// parsing the whole structure of ASN.1 —X.500 names, GeneralName, every extension— and this library
// only has the minimal DER reader `DerReader` explains. A factory that parses **halfway** is worse
// than not having one: it would return an object that looks like a certificate and lies about what
// it says.
//
// The whole structure is there, and `CertificateFactorySpi` is the complete interface: the day
// there is a parser, it is registered and everything else works.
public class CertificateFactory {

    private final CertificateFactorySpi certFacSpi;
    private final Provider provider;
    private final String type;

    protected CertificateFactory(CertificateFactorySpi certFacSpi, Provider provider, String type) {
        this.certFacSpi = certFacSpi;
        this.provider = provider;
        this.type = type;
    }

    public static final CertificateFactory getInstance(String type) throws CertificateException {
        if (type == null) {
            throw new NullPointerException("null type name");
        }
        Provider[] provs = Security.getProviders();
        int i = 0;
        while (i < provs.length) {
            Provider.Service s = provs[i].getService("CertificateFactory", type);
            if (s != null) {
                return build(s, type);
            }
            i = i + 1;
        }
        throw new CertificateException(type + " not found");
    }

    public static final CertificateFactory getInstance(String type, String provider)
            throws CertificateException, NoSuchProviderException {
        if (provider == null || provider.isEmpty()) {
            throw new IllegalArgumentException("missing provider");
        }
        Provider p = Security.getProvider(provider);
        if (p == null) {
            throw new NoSuchProviderException("no such provider: " + provider);
        }
        return getInstance(type, p);
    }

    public static final CertificateFactory getInstance(String type, Provider provider)
            throws CertificateException {
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        if (type == null) {
            throw new NullPointerException("null type name");
        }
        Provider.Service s = provider.getService("CertificateFactory", type);
        if (s == null) {
            throw new CertificateException(
                "no such type: " + type + " for provider " + provider.getName());
        }
        return build(s, type);
    }

    private static CertificateFactory build(Provider.Service s, String type)
            throws CertificateException {
        Object o;
        try {
            o = s.newInstance(null);
        } catch (java.security.NoSuchAlgorithmException e) {
            // Unlike the rest of the factories of the area, this one cannot propagate
            // `NoSuchAlgorithmException`: its `getInstance` only declares `CertificateException`.
            // It is wrapped keeping the cause instead of losing it.
            throw new CertificateException(e.getMessage(), e);
        }
        if (!(o instanceof CertificateFactorySpi)) {
            throw new CertificateException(
                "class configured for CertificateFactory is not a CertificateFactorySpi: "
                + s.getClassName());
        }
        return new CertificateFactory((CertificateFactorySpi) o, s.getProvider(), type);
    }

    public final Provider getProvider() {
        return this.provider;
    }

    // The type of certificate: "X.509".
    public final String getType() {
        return this.type;
    }

    // It reads **one** certificate from the stream. It does not validate it; see the note of the
    // class.
    public final Certificate generateCertificate(InputStream inStream)
            throws CertificateException {
        return this.certFacSpi.engineGenerateCertificate(inStream);
    }

    // The path encodings it supports, with the preferred one first.
    public final Iterator<String> getCertPathEncodings() {
        return this.certFacSpi.engineGetCertPathEncodings();
    }

    public final CertPath generateCertPath(InputStream inStream) throws CertificateException {
        return this.certFacSpi.engineGenerateCertPath(inStream);
    }

    public final CertPath generateCertPath(InputStream inStream, String encoding)
            throws CertificateException {
        return this.certFacSpi.engineGenerateCertPath(inStream, encoding);
    }

    // It builds a path from a list already in memory. The order of the list **is** that of the path
    // and is not reordered: the first is the subject and each one is signed by the next.
    public final CertPath generateCertPath(List<? extends Certificate> certificates)
            throws CertificateException {
        return this.certFacSpi.engineGenerateCertPath(certificates);
    }

    // It reads every certificate of the stream.
    public final Collection<? extends Certificate> generateCertificates(InputStream inStream)
            throws CertificateException {
        return this.certFacSpi.engineGenerateCertificates(inStream);
    }

    public final CRL generateCRL(InputStream inStream) throws CRLException {
        return this.certFacSpi.engineGenerateCRL(inStream);
    }

    public final Collection<? extends CRL> generateCRLs(InputStream inStream) throws CRLException {
        return this.certFacSpi.engineGenerateCRLs(inStream);
    }
}
