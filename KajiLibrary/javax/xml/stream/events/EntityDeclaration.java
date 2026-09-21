package javax.xml.stream.events;

/**
 * KajiLibrary's javax.xml.stream.events.EntityDeclaration -- a DTD {@code <!ENTITY ...>}.
 *
 * <h2>The three forms of entity, and how they are told apart here</h2>
 *
 * <p>The six accessors look like a lot for something so small, but they cover three different
 * things that the DTD syntax groups under the same word:
 *
 * <ul>
 *   <li>an <b>internal</b> entity --{@code <!ENTITY greeting "hello">}-- has
 *       {@link #getReplacementText()} and nothing else; the identifiers are null;
 *   <li>an <b>external parsed</b> entity --{@code <!ENTITY ch SYSTEM "ch1.xml">}-- has {@link
 *       #getSystemId()} and perhaps {@link #getPublicId()}, and its replacement text has to be
 *       fetched;
 *   <li>an <b>external unparsed</b> entity --the one ending in {@code NDATA jpeg}-- also has
 *       {@link #getNotationName()}, which is what marks it as data XML is not going to look at.
 * </ul>
 *
 * <p>That is, {@link #getNotationName()} being non-null is the hard discriminant: that entity is
 * never expanded, it is only referenced from an {@code ENTITY} attribute.
 *
 * <p>{@link #getBaseURI()} exists because a system identifier can be relative, and relative <b>to
 * where the declaration was written</b>, not to the document: an external DTD included from another
 * directory changes the base. Without this, resolving the URI gives the wrong file.
 */
public interface EntityDeclaration extends XMLEvent {

    /**
     * The declared public identifier.
     *
     * @return the public identifier, or null if the entity has none or is internal
     */
    String getPublicId();

    /**
     * The declared system identifier, perhaps relative to {@link #getBaseURI()}.
     *
     * @return the system identifier, or null if the entity is internal
     */
    String getSystemId();

    /**
     * The name the entity is referenced with, without the {@code &} or the {@code ;}.
     *
     * @return the name; never null
     */
    String getName();

    /**
     * The notation of an external unparsed entity.
     *
     * @return the name of the notation, or null if the entity is parsed
     */
    String getNotationName();

    /**
     * The text that replaces the entity, for internal ones.
     *
     * @return the replacement text, or null if the entity is external
     */
    String getReplacementText();

    /**
     * The base against which to resolve {@link #getSystemId()} if it is relative.
     *
     * @return the base URI, or null if not known
     */
    String getBaseURI();
}
