package javax.xml.crypto;

/**
 * KajiLibrary's javax.xml.crypto.URIReference -- something that points to something else by URI.
 *
 * <p>Two methods: where it points and of what type the target is. It is implemented by the two
 * XML-DSig structures that reference data: {@code Reference} --what is signed-- and {@code
 * RetrievalMethod} --where a key is taken from--.
 *
 * <p>The URI has three forms and it is worth telling them apart: empty means <b>the whole
 * document</b>, one that starts with a hash points inside the same document, and any other is
 * external. The external ones are the dangerous ones: resolving them is fetching something whoever
 * signed chose.
 */
public interface URIReference {

    /** Where it points. See the class note on the three forms. */
    String getURI();

    /** The type of the target, or null. */
    String getType();
}
