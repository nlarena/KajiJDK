package javax.xml.crypto.dsig.keyinfo;

import java.util.List;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.XMLStructure;

/**
 * KajiLibrary's javax.xml.crypto.dsig.keyinfo.KeyInfo -- what the signature says about its key.
 *
 * <p>A container of heterogeneous structures: it can bring a key name, the public key in the clear,
 * a certificate chain, a pointer to where to fetch it, or nothing. That is why {@link #getContent}
 * returns {@code XMLStructure} and not something more precise.
 *
 * <p><b>It is information, not authority.</b> Whoever signed wrote it, so a forged signature brings
 * its own key and validates perfectly against it. It serves to <b>choose</b> among keys one already
 * knows, never as the source of the key. See the note of {@code KeySelector}, which is where that
 * decision is made.
 *
 * <p>It is optional: a signature without {@code KeyInfo} is perfectly valid and means that whoever
 * validates already knows which key it is. It is, in fact, the safest way to sign.
 */
public interface KeyInfo extends XMLStructure {

    /** What it carries inside. Unmodifiable. */
    List<XMLStructure> getContent();

    /** The element's identifier, or null. */
    String getId();

    /**
     * Writes it inside that structure.
     *
     * @throws MarshalException if it cannot be written there
     */
    void marshal(XMLStructure parent, XMLCryptoContext context) throws MarshalException;
}
