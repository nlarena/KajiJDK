package javax.xml.transform;

/**
 * KajiLibrary's javax.xml.transform.Source -- where an XML document comes from.
 *
 * <p>It is a **marker interface with an identity**: it does not say how to read the document, only
 * that there is one and where it came from. Whoever implements it chooses the form --a stream, a
 * DOM tree, a sequence of SAX events-- and the processor accepts the one it knows how to handle.
 * Without this abstraction, every API that receives XML would have to offer one overload per
 * representation.
 *
 * <p>The system identifier is the only thing common to all of them: the base URI against which to
 * resolve the document's relative references. Without it, an `&lt;xsl:include
 * href="common.xsl"/&gt;` cannot be followed.
 */
public interface Source {

    /** The base URI of the document. */
    void setSystemId(String systemId);

    String getSystemId();

    /**
     * Whether this source has nothing.
     *
     * <p>It exists to tell "empty" from "null": passing an empty source is valid and means "no
     * document", which is not the same as a programming error. By default it says no, because an
     * implementation that cannot answer had better not make it up.
     */
    default boolean isEmpty() {
        return false;
    }
}
