package javax.xml.stream.events;

import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;

/**
 * KajiLibrary's javax.xml.stream.events.StartElement -- the start of an element with everything the
 * tag carried.
 *
 * <h2>The event that carries everything</h2>
 *
 * <p>It is the fattest of the model and for a structural reason: the start tag is the only place in
 * XML where several things happen at once --an element is named, prefixes are declared, attributes
 * are given-- and the event model promises that each event is self-sufficient. So all of that
 * travels inside.
 *
 * <p>The practical difference from the cursor model is right here. An {@link
 * javax.xml.stream.XMLStreamReader} answers the same questions, but only while it is standing on
 * the element; the {@code StartElement} keeps answering them when the parser has already moved on,
 * or even when the file has already been closed.
 *
 * <h2>Attributes and namespaces go separately</h2>
 *
 * <p>{@link #getAttributes()} does <b>not</b> include the {@code xmlns} declarations, which come
 * out through {@link #getNamespaces()}. It is the rule of the Namespaces specification: a
 * declaration is written as an attribute but is not one, and confusing them makes a generic mapping
 * take the {@code xmlns} along as if it were a data field.
 *
 * <h2>The context is the complete one, not the local one</h2>
 *
 * <p>{@link #getNamespaceContext()} returns the scope <b>in force</b> at this element: it includes
 * what the ancestors declared, not only what this tag did. It is what is needed to resolve a prefix
 * that appears <b>inside</b> an attribute value --as in {@code xsi:type="tns:Payment"}, where
 * {@code tns} may well be declared at the root--, which is a case no other accessor covers.
 */
public interface StartElement extends XMLEvent {

    /**
     * The name of the element.
     *
     * <p>An element without a prefix <b>does</b> fall into the default namespace, the other way
     * round from attributes.
     *
     * @return the qualified name; never null
     */
    QName getName();

    /**
     * The attributes of the tag, without the {@code xmlns} declarations.
     *
     * <p>The order is not significant --XML says an element's attributes are not ordered-- so it
     * should not be relied on.
     *
     * @return an iterator of {@link Attribute}; empty if there are none, never null
     */
    Iterator<Attribute> getAttributes();

    /**
     * The {@code xmlns} declarations <b>this</b> tag makes.
     *
     * <p>Only the ones here; for what is in force including the inherited ones, see
     * {@link #getNamespaceContext()}.
     *
     * @return an iterator of {@link Namespace}; empty if there are none, never null
     */
    Iterator<Namespace> getNamespaces();

    /**
     * An attribute by its name.
     *
     * <p>Since {@link QName#equals} ignores the prefix, the name passed can be built with any
     * prefix or none: what is compared is the namespace and the local name. To look up an
     * unqualified attribute the empty namespace has to be passed, not the element's.
     *
     * @param name the name looked for
     * @return the attribute, or null if the element does not have it
     */
    Attribute getAttributeByName(QName name);

    /**
     * The namespace scope in force at this element, including the ancestors'.
     *
     * @return the context; never null
     */
    NamespaceContext getNamespaceContext();

    /**
     * The URI associated with a prefix at this element.
     *
     * <p>A shortcut for {@code getNamespaceContext().getNamespaceURI(prefix)}.
     *
     * @param prefix the prefix; the empty string for the default namespace
     * @return the URI, or null if the prefix is not declared
     */
    String getNamespaceURI(String prefix);
}
