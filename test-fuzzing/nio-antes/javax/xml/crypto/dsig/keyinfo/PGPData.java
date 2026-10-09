package javax.xml.crypto.dsig.keyinfo;

import java.util.List;
import javax.xml.crypto.XMLStructure;

/**
 * KajiLibrary's javax.xml.crypto.dsig.keyinfo.PGPData -- key information from the PGP world.
 *
 * <p>It carries a PGP key identifier, a key packet, or both, plus whatever the application wants to
 * add in {@link #getExternalElements}.
 *
 * <p>It is the least used corner of the package: XML-DSig came out when PGP and X.509 were
 * competing, and this element exists from that time. Almost no implementation really handles it,
 * and one that finds it usually ignores it -- which is fine, because a {@code KeyInfo} with content
 * that is not understood does not invalidate the signature.
 *
 * <p>The bytes are in OpenPGP formats, not XML: the element carries them in base 64.
 */
public interface PGPData extends XMLStructure {

    /** The type URI of this element. */
    static final String TYPE = "http://www.w3.org/2000/09/xmldsig#PGPData";

    /** The PGP key identifier, or null. */
    byte[] getKeyId();

    /** The PGP key packet, or null. */
    byte[] getKeyPacket();

    /** Whatever the application added. Unmodifiable. */
    List<XMLStructure> getExternalElements();
}
