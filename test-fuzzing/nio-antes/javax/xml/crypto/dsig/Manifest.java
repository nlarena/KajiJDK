package javax.xml.crypto.dsig;

import java.util.List;
import javax.xml.crypto.XMLStructure;

/**
 * KajiLibrary's javax.xml.crypto.dsig.Manifest -- a list of references with its own validation.
 *
 * <p>A group of {@link Reference}s the signature covers <b>as a set</b>: the {@code SignedInfo} has
 * a single reference to the manifest, and the manifest has the rest.
 *
 * <p>The difference that justifies it is one of <b>failure policy</b>. A reference of the
 * {@code SignedInfo} that does not check out invalidates the whole signature. A reference of a
 * manifest does not: the library validates it and reports, and whoever uses the API decides what to
 * do.
 *
 * <p>That serves when many files are signed and some being missing is acceptable -- a package of
 * documents where each one is verified separately. And it is a trap if nobody looks at the results:
 * the signature validates and the manifest's references can all be broken.
 */
public interface Manifest extends XMLStructure {

    /** The type URI of this element. */
    static final String TYPE = "http://www.w3.org/2000/09/xmldsig#Manifest";

    /** The identifier; it is what the {@code SignedInfo}'s reference points to. */
    String getId();

    /** The manifest's references. Unmodifiable and never empty. */
    List<Reference> getReferences();
}
