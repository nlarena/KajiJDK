package javax.xml.crypto.dsig;

import java.io.InputStream;
import java.util.List;
import javax.xml.crypto.Data;
import javax.xml.crypto.URIReference;
import javax.xml.crypto.XMLStructure;

/**
 * KajiLibrary's javax.xml.crypto.dsig.Reference -- a datum covered by the signature.
 *
 * <p>It points to something by URI, says which transforms to apply to it, and keeps the digest of
 * the result. Validating a reference is redoing that path and comparing the digest.
 *
 * <h2>The two digests</h2>
 *
 * <p>{@link #getDigestValue} is the one <b>the document says</b> and {@link
 * #getCalculatedDigestValue} the one obtained when validating. That they are two different methods
 * is what allows diagnosing: if they do not match, the datum changed after being signed.
 *
 * <p>{@link #getDigestInputStream} and {@link #getDereferencedData} serve the same purpose one
 * level down: they show which data were resolved and which bytes went into the digest. Without
 * them, a reference that does not validate is a wall.
 *
 * <p>The three return something useful only <b>after</b> validating, and null before. The last two
 * also need reference caching turned on in the context; without it they stay null even after
 * validating.
 */
public interface Reference extends URIReference, XMLStructure {

    /** The transforms, in order of application. Unmodifiable. */
    List<Transform> getTransforms();

    /** What algorithm it is digested with. */
    DigestMethod getDigestMethod();

    /** The element's identifier, or null. */
    String getId();

    /** The digest the document says. */
    byte[] getDigestValue();

    /**
     * The digest computed when validating.
     *
     * @return null if it has not been validated yet
     */
    byte[] getCalculatedDigestValue();

    /**
     * Whether this reference checks out.
     *
     * @throws XMLSignatureException if it could not be resolved or transformed
     */
    boolean validate(XMLValidateContext validateContext) throws XMLSignatureException;

    /**
     * The data the URI resolved to.
     *
     * @return null if it has not been resolved yet or caching is off
     */
    Data getDereferencedData();

    /**
     * The bytes that went into the digest.
     *
     * @return null if it has not been computed yet or caching is off
     */
    InputStream getDigestInputStream();
}
