package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.Attr -- an attribute of an element.
 *
 * <p>An {@code Attr} extends {@link Node}, and there lies the peculiarity that confuses everybody
 * who comes to the DOM from another model: **an attribute is a node but it is nobody's child**.
 * {@link Node#getParentNode} always returns {@code null}, it does not appear in {@link
 * Node#getChildNodes} of its element, and the relationship with the element goes through {@link
 * #getOwnerElement}, which is a separate road. The attributes of an element are reached through
 * {@link Node#getAttributes}, which returns a {@link NamedNodeMap} and not a {@link NodeList}.
 *
 * <p>The other surprising thing is that the value is **not** a string kept in the node but the
 * children of the attribute: one or more {@link Text} and {@link EntityReference}. {@link
 * #getValue} concatenates them resolving the entities. That is why an attribute has children even
 * though it has no parent.
 *
 * <p>{@link #getSpecified} tells the attribute that was written in the document apart from the one
 * that appeared through a default value of the DTD, a distinction that only exists if there was a
 * grammar.
 *
 * <p>The interface is declared whole.
 */
public interface Attr extends Node {

    /** The name of the attribute. */
    public String getName();

    /**
     * Whether the attribute was written in the document ({@code true}) or came from a default value
     * of the DTD ({@code false}).
     */
    public boolean getSpecified();

    /** The value, with the entities resolved and the {@link Text} children concatenated. */
    public String getValue();

    /**
     * It sets the value. The text is taken **literally**: it is not parsed, so an {@code "&amp;"}
     * stays as those five characters and not as an ampersand.
     *
     * @throws DOMException {@code NO_MODIFICATION_ALLOWED_ERR} if the node is read-only
     */
    public void setValue(String value) throws DOMException;

    /** The element it belongs to, or {@code null} if the attribute is loose. */
    public Element getOwnerElement();

    /** The type information of the schema, or {@code null} if there is no validation. */
    public TypeInfo getSchemaTypeInfo();

    /**
     * Whether this attribute is of type ID, that is whether it serves for
     * {@link Document#getElementById}.
     *
     * <p>An attribute called {@code "id"} is not an ID for being called that: the DTD, the schema,
     * or somebody with {@link Element#setIdAttribute} has to declare it.
     */
    public boolean isId();
}
