package javax.xml.stream;

import java.io.OutputStream;
import java.io.Writer;

import javax.xml.transform.Result;

/**
 * KajiLibrary's javax.xml.stream.XMLOutputFactory -- the way into writing with StAX.
 *
 * <h2>Writing is easier than reading, and that is why this works whole</h2>
 *
 * <p>An XML writer parses nothing: it receives already structured calls --open this element, put
 * this attribute, write this text-- and turns them into characters, escaping what is needed. There
 * is no grammar to recognize. So nothing is left out here: both models, the cursor one and the
 * event one, really write.
 *
 * <h2>The only property, and what it decides</h2>
 *
 * <p>{@link #IS_REPAIRING_NAMESPACES} is the one that separates two very different ways of using
 * the API.
 *
 * <p>Off --the default value-- the writer does what it is told and nothing more: if an element is
 * written with prefix {@code p} and nobody declared {@code p}, a document with an undeclared prefix
 * comes out, which is not valid XML. The responsibility for calling {@code writeNamespace} in the
 * right place is the caller's.
 *
 * <p>On, the writer takes charge: when it sees a qualified name whose namespace is not declared in
 * the current scope, it emits the declaration itself, inventing a prefix if needed. In exchange, it
 * stops respecting exactly what it is asked for --it may change one prefix for another-- which is
 * correct as far as meaning goes but changes the text.
 *
 * <p>The choice is not one of style: in repairing mode the caller can ignore namespaces completely,
 * and in the other one they have to keep track. Both are implemented here.
 */
public abstract class XMLOutputFactory {

    /**
     * {@code javax.xml.stream.isRepairingNamespaces}: whether the writer declares by itself the
     * namespaces that are needed.
     *
     * <p>False by default; see the class header.
     */
    public static final String IS_REPAIRING_NAMESPACES = "javax.xml.stream.isRepairingNamespaces";

    /** The system property another implementation is plugged in with. */
    static final String PROPERTY = "javax.xml.stream.XMLOutputFactory";

    /** For the subclasses. */
    protected XMLOutputFactory() {
    }

    // ---- discovery --------------------------------------------------------------------------

    /**
     * The platform implementation, without looking at the configuration.
     *
     * @return this library's writing factory; never null
     */
    public static XMLOutputFactory newDefaultFactory() {
        return new KajiOutputFactory();
    }

    /**
     * The configured factory, or the platform's if there is none.
     *
     * @return the factory; never null
     * @throws FactoryConfigurationError if the configuration names a class that cannot be used
     */
    public static XMLOutputFactory newInstance() {
        return newFactory();
    }

    /**
     * The same as {@link #newInstance()}, with the new name.
     *
     * @return the factory; never null
     * @throws FactoryConfigurationError if the configuration names a class that cannot be used
     */
    public static XMLOutputFactory newFactory() {
        Object f = Factories.fromSystemProperty(PROPERTY, XMLOutputFactory.class);
        if (f != null) {
            return (XMLOutputFactory) f;
        }
        return newDefaultFactory();
    }

    /**
     * The <b>input</b> factory named explicitly.
     *
     * <p>Yes, it returns an {@link XMLInputFactory}, and it is not a typo of this library: the
     * signature is like that in the original API since StAX 1.0. It was a copy-and-paste mistake in
     * the specification, and by the time it was noticed there was already code compiled against it;
     * changing the return type breaks binary compatibility, so it stayed.
     *
     * <p>It is reproduced as is because the contract is the contract: a source that compiles with
     * the JDK has to compile here. To get an output factory by name there is {@link
     * #newFactory(String, ClassLoader)}, which is the one that does what one expects.
     *
     * @param factoryId the name of the class (in JDK 25, the name of a property that holds it)
     * @param classLoader the loader to look for it with; null uses the context one
     * @return the named input factory
     * @throws FactoryConfigurationError if the class cannot be loaded or is not a factory
     */
    public static XMLInputFactory newInstance(String factoryId, ClassLoader classLoader) {
        return XMLInputFactory.newFactory(factoryId, classLoader);
    }

    /**
     * The output factory named explicitly.
     *
     * <p>The one to use; see {@link #newInstance(String, ClassLoader)}.
     *
     * @param factoryId the name of the factory class; null falls back to {@link #newFactory()}. In
     *     JDK 25 it is instead the name of a property that holds the class name, and a class name
     *     there fails
     * @param classLoader the loader to look for it with; null uses the context one
     * @return the factory; never null
     * @throws FactoryConfigurationError if the class cannot be loaded or is not a factory
     */
    public static XMLOutputFactory newFactory(String factoryId, ClassLoader classLoader) {
        if (factoryId == null) {
            return newFactory();
        }
        return (XMLOutputFactory)
                Factories.instantiate(factoryId, classLoader, XMLOutputFactory.class);
    }

    // ---- writers ----------------------------------------------------------------------------

    /**
     * A cursor writer over a {@link Writer}.
     *
     * @param stream where to write
     * @return the writer
     * @throws XMLStreamException if it cannot be built
     */
    public abstract XMLStreamWriter createXMLStreamWriter(Writer stream) throws XMLStreamException;

    /**
     * A cursor writer over a byte stream, in UTF-8.
     *
     * @param stream where to write
     * @return the writer
     * @throws XMLStreamException if it cannot be built
     */
    public abstract XMLStreamWriter createXMLStreamWriter(OutputStream stream)
            throws XMLStreamException;

    /**
     * A cursor writer over a byte stream with the given encoding.
     *
     * @param stream where to write
     * @param encoding the encoding
     * @return the writer
     * @throws XMLStreamException if the encoding is not known
     */
    public abstract XMLStreamWriter createXMLStreamWriter(OutputStream stream, String encoding)
            throws XMLStreamException;

    /**
     * A cursor writer over a {@link Result}.
     *
     * @param result where to write
     * @return the writer
     * @throws XMLStreamException if the type of {@code Result} is not supported
     */
    public abstract XMLStreamWriter createXMLStreamWriter(Result result) throws XMLStreamException;

    /**
     * An event writer over a {@link Result}.
     *
     * @param result where to write
     * @return the writer
     * @throws XMLStreamException if the type of {@code Result} is not supported
     */
    public abstract XMLEventWriter createXMLEventWriter(Result result) throws XMLStreamException;

    /**
     * An event writer over a byte stream, in UTF-8.
     *
     * @param stream where to write
     * @return the writer
     * @throws XMLStreamException if it cannot be built
     */
    public abstract XMLEventWriter createXMLEventWriter(OutputStream stream)
            throws XMLStreamException;

    /**
     * An event writer over a byte stream with the given encoding.
     *
     * @param stream where to write
     * @param encoding the encoding
     * @return the writer
     * @throws XMLStreamException if the encoding is not known
     */
    public abstract XMLEventWriter createXMLEventWriter(OutputStream stream, String encoding)
            throws XMLStreamException;

    /**
     * An event writer over a {@link Writer}.
     *
     * @param stream where to write
     * @return the writer
     * @throws XMLStreamException if it cannot be built
     */
    public abstract XMLEventWriter createXMLEventWriter(Writer stream) throws XMLStreamException;

    // ---- configuration ----------------------------------------------------------------------

    /**
     * Changes a property of the factory.
     *
     * @param name the name of the property
     * @param value the value
     * @throws IllegalArgumentException if the property is not known
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
     * @param name the name of the property
     * @return true if it knows it
     */
    public abstract boolean isPropertySupported(String name);
}
