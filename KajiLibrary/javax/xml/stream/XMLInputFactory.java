package javax.xml.stream;

import java.io.InputStream;
import java.io.Reader;

import javax.xml.stream.util.XMLEventAllocator;
import javax.xml.transform.Source;

/**
 * KajiLibrary's javax.xml.stream.XMLInputFactory -- the way into reading with StAX.
 *
 * <h2>Both models come out of here</h2>
 *
 * <p>{@code createXMLStreamReader} returns the cursor reader and {@code createXMLEventReader} the
 * event one; they are the same reading with two ways of delivering it, and they are in the same
 * factory because the configuration --the properties below-- holds for both.
 *
 * <h2>Which parser is behind it</h2>
 *
 * <p>This library comes with its own XML 1.0 parser, non-validating and namespace-aware, and it is
 * the one these methods return. It reads the XML declaration, elements, attributes, text, CDATA
 * sections, comments, processing instructions and the five predefined entities plus numeric
 * references.
 *
 * <p>What it does <b>not</b> do, and it has to be known before trusting it with a document:
 *
 * <ul>
 *   <li>it does not interpret the DTD. The {@code <!DOCTYPE ...>} declaration is delivered whole as
 *       a {@link XMLStreamConstants#DTD} event and not looked at: there are no user-declared
 *       entities, nor default attribute values, nor attribute types, nor ignorable space;
 *   <li>it does not resolve external entities. That is why {@link #SUPPORT_DTD} and {@link
 *       #IS_SUPPORTING_EXTERNAL_ENTITIES} are read-only and false: accepting that they be set to
 *       true would be promising something that does not happen;
 *   <li>it does not validate. {@link #IS_VALIDATING} is also read-only at false, which is moreover
 *       the default value the specification dictates.
 * </ul>
 *
 * <p>That list is exactly the reason {@link #isPropertySupported} answers true for the properties
 * that do have an effect and {@link #setProperty} rejects the others with {@link
 * IllegalArgumentException}. A {@code setProperty} that accepts and does nothing is the kind of lie
 * that makes a document with external entities read differently from how the caller asked, without
 * anybody finding out.
 *
 * <h2>What can be configured</h2>
 *
 * <p>{@link #IS_COALESCING}, {@link #IS_NAMESPACE_AWARE}, {@link #IS_REPLACING_ENTITY_REFERENCES},
 * {@link #REPORTER}, {@link #RESOLVER} and {@link #ALLOCATOR}. The first three really change what
 * comes out of the parser.
 */
public abstract class XMLInputFactory {

    /**
     * {@code javax.xml.stream.isNamespaceAware}: whether the parser separates prefix and namespace.
     *
     * <p>True by default, and in this library changing it to false does what it says: the names are
     * left unqualified and the {@code xmlns} declarations become ordinary attributes.
     */
    public static final String IS_NAMESPACE_AWARE = "javax.xml.stream.isNamespaceAware";

    /**
     * {@code javax.xml.stream.isValidating}: whether the parser validates against the DTD.
     *
     * <p>Read-only at false; see the class header.
     */
    public static final String IS_VALIDATING = "javax.xml.stream.isValidating";

    /**
     * {@code javax.xml.stream.isCoalescing}: whether adjacent text is joined into a single event.
     *
     * <p>False by default. With true, the text and CDATA sections that touch arrive as a single
     * {@link XMLStreamConstants#CHARACTERS}, which is almost always what one wants: without this an
     * {@code &amp;} in the middle of a sentence splits it into three events.
     */
    public static final String IS_COALESCING = "javax.xml.stream.isCoalescing";

    /**
     * {@code javax.xml.stream.isReplacingEntityReferences}: whether entities are expanded.
     *
     * <p>True by default. With false, a reference to an entity that is not one of the five
     * predefined ones arrives as {@link XMLStreamConstants#ENTITY_REFERENCE} instead of being
     * expanded.
     */
    public static final String IS_REPLACING_ENTITY_REFERENCES =
            "javax.xml.stream.isReplacingEntityReferences";

    /**
     * {@code javax.xml.stream.isSupportingExternalEntities}: whether external entities are fetched.
     *
     * <p>Read-only at false; see the class header.
     */
    public static final String IS_SUPPORTING_EXTERNAL_ENTITIES =
            "javax.xml.stream.isSupportingExternalEntities";

    /**
     * {@code javax.xml.stream.supportDTD}: whether the document type declaration is processed.
     *
     * <p>Read-only at false; see the class header.
     */
    public static final String SUPPORT_DTD = "javax.xml.stream.supportDTD";

    /** {@code javax.xml.stream.reporter}: the {@link XMLReporter} to notify of warnings. */
    public static final String REPORTER = "javax.xml.stream.reporter";

    /** {@code javax.xml.stream.resolver}: the {@link XMLResolver} to resolve entities with. */
    public static final String RESOLVER = "javax.xml.stream.resolver";

    /** {@code javax.xml.stream.allocator}: the {@link XMLEventAllocator} that builds the events. */
    public static final String ALLOCATOR = "javax.xml.stream.allocator";

    /** The system property another implementation is plugged in with. */
    static final String PROPERTY = "javax.xml.stream.XMLInputFactory";

    /** For the subclasses. */
    protected XMLInputFactory() {
    }

    // ---- discovery --------------------------------------------------------------------------

    /**
     * The platform implementation, without looking at the configuration.
     *
     * @return this library's reading factory; never null
     */
    public static XMLInputFactory newDefaultFactory() {
        return new KajiInputFactory();
    }

    /**
     * The configured factory, or the platform's if there is none.
     *
     * @return the factory; never null
     * @throws FactoryConfigurationError if the configuration names a class that cannot be used
     */
    public static XMLInputFactory newInstance() {
        return newFactory();
    }

    /**
     * The same as {@link #newInstance()}, with the new name.
     *
     * @return the factory; never null
     * @throws FactoryConfigurationError if the configuration names a class that cannot be used
     */
    public static XMLInputFactory newFactory() {
        Object f = Factories.fromSystemProperty(PROPERTY, XMLInputFactory.class);
        if (f != null) {
            return (XMLInputFactory) f;
        }
        return newDefaultFactory();
    }

    /**
     * The factory named explicitly.
     *
     * @param factoryId the name of the factory class; null falls back to {@link #newFactory()}. In
     *     JDK 25 it is instead the name of a property that holds the class name, and a class name
     *     there fails
     * @param classLoader the loader to look for it with; null uses the context one
     * @return the factory; never null
     * @throws FactoryConfigurationError if the class cannot be loaded or is not a factory
     */
    public static XMLInputFactory newInstance(String factoryId, ClassLoader classLoader) {
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
    public static XMLInputFactory newFactory(String factoryId, ClassLoader classLoader) {
        if (factoryId == null) {
            return newFactory();
        }
        return (XMLInputFactory) Factories.instantiate(factoryId, classLoader, XMLInputFactory.class);
    }

    // ---- cursor readers -------------------------------------------------------------------------

    /**
     * A cursor reader over a {@link Reader}.
     *
     * @param reader where to read from
     * @return the reader, standing before the first event
     * @throws XMLStreamException if reading cannot start
     */
    public abstract XMLStreamReader createXMLStreamReader(Reader reader) throws XMLStreamException;

    /**
     * A cursor reader over a {@link Source}.
     *
     * @param source where to read from
     * @return the reader
     * @throws XMLStreamException if the type of {@code Source} is not supported or it cannot be
     *     read
     */
    public abstract XMLStreamReader createXMLStreamReader(Source source) throws XMLStreamException;

    /**
     * A cursor reader over a byte stream.
     *
     * @param stream where to read from
     * @return the reader
     * @throws XMLStreamException if reading cannot start
     */
    public abstract XMLStreamReader createXMLStreamReader(InputStream stream)
            throws XMLStreamException;

    /**
     * A cursor reader over a byte stream with the given encoding.
     *
     * @param stream where to read from
     * @param encoding the encoding, which wins over the one the document declares
     * @return the reader
     * @throws XMLStreamException if reading cannot start
     */
    public abstract XMLStreamReader createXMLStreamReader(InputStream stream, String encoding)
            throws XMLStreamException;

    /**
     * A cursor reader over a byte stream, remembering where it came from.
     *
     * @param systemId the system identifier, for messages and locations
     * @param stream where to read from
     * @return the reader
     * @throws XMLStreamException if reading cannot start
     */
    public abstract XMLStreamReader createXMLStreamReader(String systemId, InputStream stream)
            throws XMLStreamException;

    /**
     * A cursor reader over a {@link Reader}, remembering where it came from.
     *
     * @param systemId the system identifier, for messages and locations
     * @param reader where to read from
     * @return the reader
     * @throws XMLStreamException if reading cannot start
     */
    public abstract XMLStreamReader createXMLStreamReader(String systemId, Reader reader)
            throws XMLStreamException;

    // ---- event readers --------------------------------------------------------------------------

    /**
     * An event reader over a {@link Reader}.
     *
     * @param reader where to read from
     * @return the reader
     * @throws XMLStreamException if reading cannot start
     */
    public abstract XMLEventReader createXMLEventReader(Reader reader) throws XMLStreamException;

    /**
     * An event reader over a {@link Reader}, remembering where it came from.
     *
     * @param systemId the system identifier
     * @param reader where to read from
     * @return the reader
     * @throws XMLStreamException if reading cannot start
     */
    public abstract XMLEventReader createXMLEventReader(String systemId, Reader reader)
            throws XMLStreamException;

    /**
     * An event reader mounted on an existing cursor reader.
     *
     * <p>It is the bridge between the two models: the cursor keeps doing the work and this wraps
     * each position in an event of its own.
     *
     * @param reader the cursor reader
     * @return the event reader
     * @throws XMLStreamException if it cannot be wrapped
     */
    public abstract XMLEventReader createXMLEventReader(XMLStreamReader reader)
            throws XMLStreamException;

    /**
     * An event reader over a {@link Source}.
     *
     * @param source where to read from
     * @return the reader
     * @throws XMLStreamException if the type of {@code Source} is not supported or it cannot be
     *     read
     */
    public abstract XMLEventReader createXMLEventReader(Source source) throws XMLStreamException;

    /**
     * An event reader over a byte stream.
     *
     * @param stream where to read from
     * @return the reader
     * @throws XMLStreamException if reading cannot start
     */
    public abstract XMLEventReader createXMLEventReader(InputStream stream)
            throws XMLStreamException;

    /**
     * An event reader over a byte stream with the given encoding.
     *
     * @param stream where to read from
     * @param encoding the encoding, which wins over the one the document declares
     * @return the reader
     * @throws XMLStreamException if reading cannot start
     */
    public abstract XMLEventReader createXMLEventReader(InputStream stream, String encoding)
            throws XMLStreamException;

    /**
     * An event reader over a byte stream, remembering where it came from.
     *
     * @param systemId the system identifier
     * @param stream where to read from
     * @return the reader
     * @throws XMLStreamException if reading cannot start
     */
    public abstract XMLEventReader createXMLEventReader(String systemId, InputStream stream)
            throws XMLStreamException;

    // ---- filters ----------------------------------------------------------------------------

    /**
     * A cursor reader that only stops at the events the filter accepts.
     *
     * @param reader the underlying reader
     * @param filter which events to let through
     * @return the filtered reader
     * @throws XMLStreamException if it cannot be built
     */
    public abstract XMLStreamReader createFilteredReader(XMLStreamReader reader, StreamFilter filter)
            throws XMLStreamException;

    /**
     * An event reader that only delivers the events the filter accepts.
     *
     * @param reader the underlying reader
     * @param filter which events to let through
     * @return the filtered reader
     * @throws XMLStreamException if it cannot be built
     */
    public abstract XMLEventReader createFilteredReader(XMLEventReader reader, EventFilter filter)
            throws XMLStreamException;

    // ---- configuration ----------------------------------------------------------------------

    /**
     * The configured entity resolver.
     *
     * @return the resolver, or null if there is none
     */
    public abstract XMLResolver getXMLResolver();

    /**
     * Sets the entity resolver.
     *
     * @param resolver the resolver
     */
    public abstract void setXMLResolver(XMLResolver resolver);

    /**
     * The configured warning reporter.
     *
     * @return the reporter, or null if there is none
     */
    public abstract XMLReporter getXMLReporter();

    /**
     * Sets the warning reporter.
     *
     * @param reporter the reporter
     */
    public abstract void setXMLReporter(XMLReporter reporter);

    /**
     * Changes a property of the factory.
     *
     * @param name the name of the property
     * @param value the value
     * @throws IllegalArgumentException if the property is not known, or it is known but read-only
     *     in this implementation and the value asked for is not the one it has
     */
    public abstract void setProperty(String name, Object value) throws IllegalArgumentException;

    /**
     * The value of a property.
     *
     * @param name the name of the property
     * @return the value
     * @throws IllegalArgumentException if the property is not known
     */
    public abstract Object getProperty(String name) throws IllegalArgumentException;

    /**
     * Whether the factory knows a property.
     *
     * <p>Knowing it is not the same as letting it be changed: see {@link #setProperty}.
     *
     * @param name the name of the property
     * @return true if it knows it
     */
    public abstract boolean isPropertySupported(String name);

    /**
     * Sets the event builder the event reader is going to use.
     *
     * @param allocator the builder
     */
    public abstract void setEventAllocator(XMLEventAllocator allocator);

    /**
     * The configured event builder.
     *
     * @return the builder; never null
     */
    public abstract XMLEventAllocator getEventAllocator();
}
