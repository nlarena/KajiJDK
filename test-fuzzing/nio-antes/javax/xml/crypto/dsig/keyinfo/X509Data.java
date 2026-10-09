package javax.xml.crypto.dsig.keyinfo;

import java.util.List;
import javax.xml.crypto.XMLStructure;

/**
 * KajiLibrary's javax.xml.crypto.dsig.keyinfo.X509Data -- key information by certificate.
 *
 * <p>The most used element of {@link KeyInfo}. Its content is a <b>heterogeneous</b> list: it can
 * bring certificates, revocation lists, subject names, issuer-serial pairs, or the bytes of a raw
 * certificate. That is why {@link #getContent} returns an untyped {@code List<?>} -- it is what the
 * standard allows.
 *
 * <p>It is the one that comes closest to being a legitimate source of trust, and it still is not
 * one on its own: a certificate inside the signature is worth what the <b>chain</b> that leads it
 * to an anchor one trusts is worth. Validating the signature and not validating the chain is having
 * a valid signature from anybody.
 *
 * <p>{@link #RAW_X509_CERTIFICATE_TYPE} names the raw form --the DER bytes without wrapping-- used
 * when the certificate goes as the content of another element.
 */
public interface X509Data extends XMLStructure {

    /** The type URI of this element. */
    static final String TYPE = "http://www.w3.org/2000/09/xmldsig#X509Data";

    /** The one of a raw certificate. */
    static final String RAW_X509_CERTIFICATE_TYPE =
        "http://www.w3.org/2000/09/xmldsig#rawX509Certificate";

    /** What it carries inside; heterogeneous. Unmodifiable. See the class note. */
    List<?> getContent();
}
