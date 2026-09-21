package javax.security.cert;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.math.BigInteger;
import java.security.Principal;
import java.security.Security;
import java.util.Date;

/**
 * KajiLibrary's javax.security.cert.X509Certificate -- an X.509, in the old API.
 *
 * <p>It is a deliberate subset of the real X.509: only the fields of <b>version 1</b>, without
 * extensions. That makes it useless for validating a modern chain --without extensions there is no
 * {@code basicConstraints}, and without {@code basicConstraints} there is no knowing whether a
 * certificate has the right to sign others-- and that is exactly why
 * {@link java.security.cert.X509Certificate} replaced it.
 *
 * <h2>Where the implementation comes from</h2>
 *
 * <p>This class is abstract and the {@code getInstance}s have to make something concrete. The name
 * of that class is not written here: it is read from the security property {@code
 * cert.provider.x509v1} and instantiated by reflection. It is indirection on purpose --it lets the
 * parser be changed without recompiling-- and it is the same route the JDK uses.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>KajiLibrary comes with no concrete X.509 parser today, so that property has no value and both
 * {@code getInstance}s throw {@link CertificateException}. It is a <b>declared</b> way out of the
 * method, not a lie: the caller already has to handle it, and the message says which property is
 * missing. Returning a half-built certificate would be worse, because the callers of this API are
 * precisely the ones who decide whether to trust a remote peer.
 *
 * <p>Deprecated <b>and marked for removal</b> since Java 9. The replacement is {@code
 * java.security.cert}, which is not an improved version of this but something else: it supports
 * version 3 of the format, with extensions, which is the only thing that serves to validate a chain
 * today.
 */
@Deprecated(since = "9", forRemoval = true)
public abstract class X509Certificate extends Certificate {

    /** The security property that names the concrete class. */
    private static final String X509_PROVIDER = "cert.provider.x509v1";

    /** For the subclasses. */
    public X509Certificate() {
    }

    /**
     * Reads a certificate from a stream.
     *
     * <p>It consumes the whole stream and delegates to {@link #getInstance(byte[])}.
     *
     * @throws CertificateException if there is no parser configured or the bytes do not check out
     */
    public static final X509Certificate getInstance(InputStream inStream)
        throws CertificateException {
        if (inStream == null) {
            throw new CertificateException("Missing input stream");
        }
        byte[] encoded;
        try {
            encoded = inStream.readAllBytes();
        } catch (IOException e) {
            throw new CertificateException("Failed to read the certificate: " + e.getMessage());
        }
        return getInstance(encoded);
    }

    /**
     * Reads a certificate from its bytes.
     *
     * @throws CertificateException if there is no parser configured or the bytes do not check out
     */
    public static final X509Certificate getInstance(byte[] certData) throws CertificateException {
        if (certData == null) {
            throw new CertificateException("Missing certificate data");
        }
        String className = Security.getProperty(X509_PROVIDER);
        if (className == null || className.length() == 0) {
            throw new CertificateException(
                "No X.509 v1 certificate implementation is configured; the security property "
                    + X509_PROVIDER + " is not set");
        }
        try {
            Class<?> implementation = Class.forName(className);
            Constructor<?> ctor = implementation.getConstructor(new Class<?>[] {InputStream.class});
            Object made = ctor.newInstance(new Object[] {new ByteArrayInputStream(certData)});
            return (X509Certificate) made;
        } catch (ClassCastException e) {
            throw new CertificateException(
                className + " is not a javax.security.cert.X509Certificate");
        } catch (Exception e) {
            throw new CertificateException(
                "Could not build a certificate with " + className + ": " + e);
        }
    }

    /**
     * Checks that the certificate is valid <b>now</b>.
     *
     * @throws CertificateExpiredException if it already expired
     * @throws CertificateNotYetValidException if it has not started yet
     */
    public abstract void checkValidity()
        throws CertificateExpiredException, CertificateNotYetValidException;

    /** Likewise, against a given date. */
    public abstract void checkValidity(Date date)
        throws CertificateExpiredException, CertificateNotYetValidException;

    /**
     * The format version as the ASN.1 encodes it: 0 for v1, 1 for v2, 2 for v3 -- unlike
     * {@code java.security.cert}, which counts from 1. (The note said 1 for everything this API can
     * describe; the JDK's specification gives 0, 1 or 2, so a v1 certificate is 0.)
     */
    public abstract int getVersion();

    /** The serial number, unique <b>per issuer</b> and not in general. */
    public abstract BigInteger getSerialNumber();

    /** Who signed it. */
    public abstract Principal getIssuerDN();

    /** Whom it is about. */
    public abstract Principal getSubjectDN();

    /** From when it is valid. */
    public abstract Date getNotBefore();

    /** Until when it is valid. */
    public abstract Date getNotAfter();

    /** The name of the signature algorithm, if known; otherwise the OID. */
    public abstract String getSigAlgName();

    /** The OID of the signature algorithm, which is the datum really in the certificate. */
    public abstract String getSigAlgOID();

    /** The algorithm's parameters, in DER, or null if it has none. */
    public abstract byte[] getSigAlgParams();
}
