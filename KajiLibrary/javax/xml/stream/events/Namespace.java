package javax.xml.stream.events;

/**
 * KajiLibrary's javax.xml.stream.events.Namespace -- an {@code xmlns} declaration seen as an event.
 *
 * <h2>It extends {@link Attribute}, and it makes sense that it does</h2>
 *
 * <p>In the text of the document a namespace declaration <b>is</b> an attribute: {@code
 * xmlns:a="http://x"} is written like any other name-value pair. The Namespaces specification then
 * takes it out of the attribute set and gives it another meaning, but the syntax is the same, hence
 * the type inherits.
 *
 * <p>The inheritance leaves {@link Attribute}'s accessors available and with a useful meaning:
 * {@link Attribute#getValue()} returns the URI --the same as {@link #getNamespaceURI()}-- and
 * {@link Attribute#getName()} the name as it was written, that is
 * <code>{http://www.w3.org/2000/xmlns/}a</code> for a declaration with a prefix and a bare {@code
 * xmlns} for the default declaration.
 *
 * <h2>The default declaration and the undeclaring one</h2>
 *
 * <p>{@code xmlns="http://x"} sets the default namespace for the element and its descendants;
 * {@code xmlns=""} <b>undeclares</b> it, and goes back to "no namespace". Both cases answer true to
 * {@link #isDefaultNamespaceDeclaration()} and are told apart because the second has an empty
 * {@link #getNamespaceURI()}.
 *
 * <p>{@link #getPrefix()} returns the empty string in both: the prefix of the default declaration
 * is, precisely, none.
 */
public interface Namespace extends Attribute {

    /**
     * The prefix being declared, or the empty string for the default declaration.
     *
     * @return the prefix; never null
     */
    String getPrefix();

    /**
     * The URI the prefix gets associated with.
     *
     * @return the namespace; the empty string if the declaration undeclares the default one
     */
    String getNamespaceURI();

    /**
     * Whether this declaration is the one of the default namespace, that is {@code xmlns=...}.
     *
     * @return true if it declares no prefix
     */
    boolean isDefaultNamespaceDeclaration();
}
