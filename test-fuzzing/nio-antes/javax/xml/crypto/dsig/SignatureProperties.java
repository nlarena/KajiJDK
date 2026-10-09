package javax.xml.crypto.dsig;

import java.util.List;
import javax.xml.crypto.XMLStructure;

/**
 * KajiLibrary's javax.xml.crypto.dsig.SignatureProperties -- a group of {@link SignatureProperty}.
 *
 * <p>It only groups. It exists because a {@link Reference} points to <b>one</b> element, and
 * without it one reference per property would be needed; with it, a single reference covers them
 * all.
 *
 * <p>The same warning as in {@link SignatureProperty} holds: grouping does not protect. If nobody
 * points to the group, the properties are left outside the signature.
 */
public interface SignatureProperties extends XMLStructure {

    /** The type URI of this element. */
    static final String TYPE = "http://www.w3.org/2000/09/xmldsig#SignatureProperties";

    /** The identifier; it is what a {@link Reference} points to. */
    String getId();

    /** The properties. Unmodifiable and never empty. */
    List<SignatureProperty> getProperties();
}
