package java.security.cert;

import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

// What a provider has to write in order to know how to read certificates and CRLs from a stream.
//
// The four methods of `CertPath` are not abstract and the other four are. The asymmetry is
// historical: certification paths arrived in Java 1.4, after there were already factories written,
// and making them abstract would have broken all of them. The ones that are not overridden throw
// `UnsupportedOperationException`, which is the honest thing: a factory that knows nothing about
// paths says so instead of returning something empty.
public abstract class CertificateFactorySpi {

    public CertificateFactorySpi() {
    }

    // Reads a certificate from the stream.
    public abstract Certificate engineGenerateCertificate(InputStream inStream)
        throws CertificateException;

    public CertPath engineGenerateCertPath(InputStream inStream) throws CertificateException {
        throw new UnsupportedOperationException();
    }

    public CertPath engineGenerateCertPath(InputStream inStream, String encoding)
            throws CertificateException {
        throw new UnsupportedOperationException();
    }

    public CertPath engineGenerateCertPath(List<? extends Certificate> certificates)
            throws CertificateException {
        throw new UnsupportedOperationException();
    }

    // The supported path encodings, with the preferred one first.
    public Iterator<String> engineGetCertPathEncodings() {
        throw new UnsupportedOperationException();
    }

    // Reads every certificate of the stream. It may return an empty collection.
    public abstract Collection<? extends Certificate> engineGenerateCertificates(
        InputStream inStream) throws CertificateException;

    public abstract CRL engineGenerateCRL(InputStream inStream) throws CRLException;

    public abstract Collection<? extends CRL> engineGenerateCRLs(InputStream inStream)
        throws CRLException;
}
