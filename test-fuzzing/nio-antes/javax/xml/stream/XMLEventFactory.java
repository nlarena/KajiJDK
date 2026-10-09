package javax.xml.stream;

import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.Comment;
import javax.xml.stream.events.DTD;
import javax.xml.stream.events.EndDocument;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.EntityDeclaration;
import javax.xml.stream.events.EntityReference;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.ProcessingInstruction;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.StartElement;

/**
 * KajiLibrary's javax.xml.stream.XMLEventFactory -- where the {@link
 * javax.xml.stream.events.XMLEvent}s one builds by hand come from.
 *
 * <h2>The factory that needs no parser</h2>
 *
 * <p>Of the three StAX factories this is the only one that reads nothing: each method builds an
 * event from the data the caller passes it. It is the one used to <b>generate</b> XML with the
 * event model, or to inject events into a stream being transformed.
 *
 * <p>That is why it works whole here, with real implementations behind it. See {@link
 * XMLInputFactory}, which does depend on a parser.
 *
 * <h2>The sticky {@link Location}</h2>
 *
 * <p>{@link #setLocation} is the only piece with state and it has to be looked at carefully: it
 * sets a location that is going to be put on <b>all</b> the events the factory creates from then
 * on, until it is changed. It is not a parameter of an event, it is a mode of the factory.
 *
 * <p>The design comes from an event having a location but the {@code createXxx} methods not
 * receiving it, and adding a parameter to the twenty-odd of them would have been worse. The
 * practical consequence is that an instance of this factory <b>cannot</b> be shared between threads
 * if any of them calls {@code setLocation}: it is unsynchronized mutable state. The recommendation
 * is one factory per thread, which is cheap anyway.
 *
 * <h2>The {@code Iterator}s several methods receive</h2>
 *
 * <p>{@code createStartElement} and {@code createEndElement} take iterators of {@link Attribute}
 * and of {@link Namespace}. They are consumed <b>at that moment</b>: the event that comes out is
 * immutable and has already copied what it needed, so the iterator can be discarded afterwards.
 * null is valid and means "none".
 */
public abstract class XMLEventFactory {

    /**
     * The system property another implementation is plugged in with:
     * {@code javax.xml.stream.XMLEventFactory}.
     */
    static final String PROPERTY = "javax.xml.stream.XMLEventFactory";

    /** For the subclasses. */
    protected XMLEventFactory() {
    }

    // ---- discovery --------------------------------------------------------------------------

    /**
     * The platform implementation, without looking at the configuration.
     *
     * @return this library's event factory; never null
     */
    public static XMLEventFactory newDefaultFactory() {
        return new KajiEventFactory();
    }

    /**
     * The configured factory.
     *
     * <p>It looks at the system property {@code javax.xml.stream.XMLEventFactory} and, if it is not
     * set, returns the platform's. (The note also named declared service providers; see {@code
     * Factories}: no {@code ServiceLoader} is consulted here, while the JDK also reads {@code
     * stax.properties}, {@code jaxp.properties} and the service providers.)
     *
     * @return the factory found; never null
     * @throws FactoryConfigurationError if the configuration names a class that cannot be used
     */
    public static XMLEventFactory newInstance() {
        return newFactory();
    }

    /**
     * The same as {@link #newInstance()}, with the new name.
     *
     * <p>Both exist because the JDK replaced the {@code newInstance} methods with {@code
     * newFactory} ones "to maintain API consistency", with no change in behaviour; only {@code
     * newInstance(String, ClassLoader)} is deprecated there. Here they do the same. (The note said
     * {@code newInstance()} had an overload that did not tell errors apart properly.)
     *
     * @return the factory found; never null
     * @throws FactoryConfigurationError if the configuration names a class that cannot be used
     */
    public static XMLEventFactory newFactory() {
        Object f = Factories.fromSystemProperty(PROPERTY, XMLEventFactory.class);
        if (f != null) {
            return (XMLEventFactory) f;
        }
        return newDefaultFactory();
    }

    /**
     * The factory named explicitly, loaded with the loader indicated.
     *
     * @param factoryId the name of the factory class; null falls back to {@link #newFactory()}. In
     *     JDK 25 it is instead the name of a property that holds the class name, and a class name
     *     there fails
     * @param classLoader the loader to look for it with; null uses the context one
     * @return the factory; never null
     * @throws FactoryConfigurationError if the class cannot be loaded or is not a factory
     */
    public static XMLEventFactory newInstance(String factoryId, ClassLoader classLoader) {
        return newFactory(factoryId, classLoader);
    }

    /**
     * The same as {@link #newInstance(String, ClassLoader)}, with the new name.
     *
     * @param factoryId the name of the factory class; null falls back to {@link #newFactory()}. In
     *     JDK 25 it is instead the name of a property that holds the class name, and a class name
     *     there fails
     * @param classLoader the loader to look for it with; null uses the context one
     * @return the factory; never null
     * @throws FactoryConfigurationError if the class cannot be loaded or is not a factory
     */
    public static XMLEventFactory newFactory(String factoryId, ClassLoader classLoader) {
        if (factoryId == null) {
            return newFactory();
        }
        return (XMLEventFactory) Factories.instantiate(factoryId, classLoader, XMLEventFactory.class);
    }

    // ---- the sticky location --------------------------------------------------------------------

    /**
     * Sets the location all the events created from here on are going to carry.
     *
     * <p>It is the factory's state, not a parameter; see the class header.
     *
     * @param location the location; null goes back to the default, which has no data
     */
    public abstract void setLocation(Location location);

    // ---- the events -----------------------------------------------------------------------------

    /**
     * An attribute without a namespace.
     *
     * @param localName the local name
     * @param value the value
     * @return the attribute
     */
    public abstract Attribute createAttribute(String localName, String value);

    /**
     * A qualified attribute.
     *
     * @param prefix the prefix to write it with
     * @param namespaceURI the namespace
     * @param localName the local name
     * @param value the value
     * @return the attribute
     */
    public abstract Attribute createAttribute(
            String prefix, String namespaceURI, String localName, String value);

    /**
     * An attribute from an already built name.
     *
     * @param name the qualified name
     * @param value the value
     * @return the attribute
     */
    public abstract Attribute createAttribute(QName name, String value);

    /**
     * The declaration of the default namespace, that is {@code xmlns="..."}.
     *
     * @param namespaceURI the URI to declare
     * @return the declaration
     */
    public abstract Namespace createNamespace(String namespaceURI);

    /**
     * The declaration of a prefix, that is {@code xmlns:p="..."}.
     *
     * @param prefix the prefix to declare
     * @param namespaceUri the URI to associate with it
     * @return the declaration
     */
    public abstract Namespace createNamespace(String prefix, String namespaceUri);

    /**
     * The start of an element, with its attributes and its declarations.
     *
     * @param name the element's name
     * @param attributes the attributes, or null if there are none
     * @param namespaces the {@code xmlns} declarations, or null if there are none
     * @return the event
     */
    public abstract StartElement createStartElement(
            QName name, Iterator<? extends Attribute> attributes,
            Iterator<? extends Namespace> namespaces);

    /**
     * The start of an element, without attributes or declarations.
     *
     * @param prefix the prefix to write it with
     * @param namespaceUri the namespace
     * @param localName the local name
     * @return the event
     */
    public abstract StartElement createStartElement(
            String prefix, String namespaceUri, String localName);

    /**
     * The start of an element, with its attributes and its declarations.
     *
     * @param prefix the prefix to write it with
     * @param namespaceUri the namespace
     * @param localName the local name
     * @param attributes the attributes, or null if there are none
     * @param namespaces the {@code xmlns} declarations, or null if there are none
     * @return the event
     */
    public abstract StartElement createStartElement(
            String prefix, String namespaceUri, String localName,
            Iterator<? extends Attribute> attributes, Iterator<? extends Namespace> namespaces);

    /**
     * The start of an element, with the namespace context around it.
     *
     * @param prefix the prefix to write it with
     * @param namespaceUri the namespace
     * @param localName the local name
     * @param attributes the attributes, or null if there are none
     * @param namespaces the {@code xmlns} declarations, or null if there are none
     * @param context the scope in force, or null
     * @return the event
     */
    public abstract StartElement createStartElement(
            String prefix, String namespaceUri, String localName,
            Iterator<? extends Attribute> attributes, Iterator<? extends Namespace> namespaces,
            NamespaceContext context);

    /**
     * The end of an element.
     *
     * @param name the element's name
     * @param namespaces the namespaces going out of scope, or null
     * @return the event
     */
    public abstract EndElement createEndElement(
            QName name, Iterator<? extends Namespace> namespaces);

    /**
     * The end of an element.
     *
     * @param prefix the prefix to write it with
     * @param namespaceUri the namespace
     * @param localName the local name
     * @return the event
     */
    public abstract EndElement createEndElement(
            String prefix, String namespaceUri, String localName);

    /**
     * The end of an element.
     *
     * @param prefix the prefix to write it with
     * @param namespaceUri the namespace
     * @param localName the local name
     * @param namespaces the namespaces going out of scope, or null
     * @return the event
     */
    public abstract EndElement createEndElement(
            String prefix, String namespaceUri, String localName,
            Iterator<? extends Namespace> namespaces);

    /**
     * Ordinary text.
     *
     * @param content the text
     * @return the event, with {@link XMLStreamConstants#CHARACTERS}
     */
    public abstract Characters createCharacters(String content);

    /**
     * Text in a {@code <![CDATA[...]]>} section.
     *
     * @param content the text
     * @return the event, with {@link XMLStreamConstants#CDATA}
     */
    public abstract Characters createCData(String content);

    /**
     * Whitespace.
     *
     * @param content the space
     * @return the event, with {@link XMLStreamConstants#CHARACTERS}
     */
    public abstract Characters createSpace(String content);

    /**
     * Whitespace marked as ignorable.
     *
     * @param content the space
     * @return the event, with {@link XMLStreamConstants#SPACE}
     */
    public abstract Characters createIgnorableSpace(String content);

    /**
     * The start of the document, with the default values: {@code 1.0} and {@code UTF-8}, neither of
     * them declared.
     *
     * @return the event
     */
    public abstract StartDocument createStartDocument();

    /**
     * The start of the document with a declared encoding.
     *
     * @param encoding the encoding
     * @return the event
     */
    public abstract StartDocument createStartDocument(String encoding);

    /**
     * The start of the document with declared encoding and version.
     *
     * @param encoding the encoding
     * @param version the version
     * @return the event
     */
    public abstract StartDocument createStartDocument(String encoding, String version);

    /**
     * The start of the document with the three parts of the declaration.
     *
     * @param encoding the encoding
     * @param version the version
     * @param standalone the value of {@code standalone}
     * @return the event
     */
    public abstract StartDocument createStartDocument(
            String encoding, String version, boolean standalone);

    /**
     * The end of the document.
     *
     * @return the event
     */
    public abstract EndDocument createEndDocument();

    /**
     * An unexpanded entity reference.
     *
     * @param name the name of the entity
     * @param declaration its declaration
     * @return the event
     */
    public abstract EntityReference createEntityReference(
            String name, EntityDeclaration declaration);

    /**
     * A comment.
     *
     * @param text the text, without the delimiters
     * @return the event
     */
    public abstract Comment createComment(String text);

    /**
     * A processing instruction.
     *
     * @param target whom it is addressed to
     * @param data the rest, raw
     * @return the event
     */
    public abstract ProcessingInstruction createProcessingInstruction(String target, String data);

    /**
     * A document type declaration, from its raw text.
     *
     * @param dtd the text of the declaration
     * @return the event
     */
    public abstract DTD createDTD(String dtd);
}
