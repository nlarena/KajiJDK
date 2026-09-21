package javax.xml.stream;

/**
 * KajiLibrary's javax.xml.stream.XMLResolver -- who decides where an external entity comes from.
 *
 * <p>A document can refer to another file --a DTD, an external entity-- by a URI, and the parser
 * has to fetch it. This is the point where the application steps into that search, and the two
 * reasons for doing so are different:
 *
 * <ul>
 *   <li><b>serving from a local catalog</b>, so as not to go out to the network to download a DTD
 *       one already has, and
 *   <li><b>serving nothing</b>, which is the defence against XXE: a hostile document that declares
 *       an entity pointing to a server file makes the parser read it and carries it off in the
 *       response. A resolver that returns empty for everything it does not recognize cuts that off
 *       at the root.
 * </ul>
 *
 * <p>The return value is {@code Object} and not a useful type because implementations accept
 * several forms of the same thing --an {@link java.io.InputStream}, a {@link java.io.Reader}, a
 * {@link javax.xml.stream.XMLStreamReader}, a {@link javax.xml.transform.Source}-- and the spec did
 * not want to choose. Which types each parser accepts is its own business.
 */
public interface XMLResolver {

    /**
     * Resolves an external entity.
     *
     * @param publicID the declared public identifier, or null if there is none
     * @param systemID the declared system identifier
     * @param baseURI the URI of the document that references it, to resolve the relative one
     * @param namespace the namespace of the entity, if it applies
     * @return the content, in one of the forms the parser accepts, or null for it to resolve the
     *     default one
     * @throws XMLStreamException if the entity cannot be resolved and that has to cut
     */
    Object resolveEntity(String publicID, String systemID, String baseURI, String namespace)
            throws XMLStreamException;
}
