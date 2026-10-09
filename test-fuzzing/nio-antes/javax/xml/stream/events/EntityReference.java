package javax.xml.stream.events;

/**
 * KajiLibrary's javax.xml.stream.events.EntityReference -- an {@code &entity;} left unexpanded.
 *
 * <h2>When it appears, which is almost never</h2>
 *
 * <p>By default a StAX reader expands entities: {@code &greeting;} becomes the text the entity
 * declared and arrives as {@link Characters}. This event exists only if that expansion was turned
 * off with {@link javax.xml.stream.XMLInputFactory#IS_REPLACING_ENTITY_REFERENCES}.
 *
 * <p>Turning it off serves two concrete purposes: rewriting a document keeping the references as
 * they were --expanding them is an irreversible loss-- and not expanding entities of untrusted
 * documents, which is where exponential expansion attacks come from.
 *
 * <p>The five predefined XML entities --{@code &lt;}, {@code &gt;}, {@code &amp;}, {@code &quot;},
 * {@code &apos;}-- are <b>always</b> resolved and never arrive as this event: they are not declared
 * entities but syntax of the language.
 *
 * <p>{@link #getDeclaration()} returns the declaration that corresponds to it, which is only
 * possible if the parser read the DTD. Without a DTD there is no declaration to return.
 */
public interface EntityReference extends XMLEvent {

    /**
     * The declaration of the referenced entity.
     *
     * @return the declaration, or null if the parser did not read the DTD that declares it
     */
    EntityDeclaration getDeclaration();

    /**
     * The name of the entity, without the {@code &} or the {@code ;}.
     *
     * @return the name; never null
     */
    String getName();
}
