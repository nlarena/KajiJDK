package javax.xml.validation;

/**
 * KajiLibrary's javax.xml.validation.SchemaFactoryLoader -- a factory of factories.
 *
 * <p>A single method, which given a schema language's URI returns the {@link SchemaFactory} that
 * understands it. It is one more level of indirection over {@link SchemaFactory#newInstance}, and
 * it exists for whoever wants to decide on their own which implementation each language uses
 * instead of accepting the search by properties and services.
 *
 * <p>The platform does not use it by itself: there is no {@code newInstance} that looks for it. It
 * is a class for whoever needs it to instantiate and call, and that is why it needs no registration
 * route.
 */
public abstract class SchemaFactoryLoader {

    /** For the subclasses. */
    protected SchemaFactoryLoader() {
    }

    /**
     * The factory for that language.
     *
     * @param schemaLanguage the language's URI, for example {@code
     *     javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI}
     * @return null if it does not know that language
     */
    public abstract SchemaFactory newFactory(String schemaLanguage);
}
