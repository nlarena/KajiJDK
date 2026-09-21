package javax.xml.stream.events;

/**
 * KajiLibrary's javax.xml.stream.events.NotationDeclaration -- a DTD {@code <!NOTATION ...>}.
 *
 * <p>A notation gives a name to a format XML is not going to interpret --{@code <!NOTATION jpeg
 * SYSTEM "image/jpeg">}-- so that an external unparsed entity can say what type its data is. It is
 * the mechanism with which a DTD says "there is something in here that is not text".
 *
 * <p>It has fallen practically into disuse: the same problem is solved today with a MIME type in an
 * attribute, or with base64 data inside the document itself. It stays in the API because it stays
 * in the XML 1.0 specification, and because an old document that uses it has to be readable without
 * losing information.
 *
 * <p>Either {@link #getPublicId()} or {@link #getSystemId()} can be missing --a notation declared
 * only with {@code PUBLIC} has no system identifier-- but not both at once.
 */
public interface NotationDeclaration extends XMLEvent {

    /**
     * The name of the notation, which is what an entity references it with.
     *
     * @return the name; never null
     */
    String getName();

    /**
     * The declared public identifier.
     *
     * @return the public identifier, or null if the notation was declared only with {@code SYSTEM}
     */
    String getPublicId();

    /**
     * The declared system identifier.
     *
     * @return the system identifier, or null if the notation was declared only with {@code PUBLIC}
     */
    String getSystemId();
}
