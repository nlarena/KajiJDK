package javax.xml.parsers;

import java.util.Iterator;
import java.util.ServiceLoader;
import javax.xml.validation.Schema;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

/**
 * KajiLibrary's javax.xml.parsers.SAXParserFactory -- where the {@link SAXParser}s come from.
 *
 * <p>The same idea and the same six {@code newInstance}s as {@link DocumentBuilderFactory}, with
 * which it is worth reading in parallel: the search by system property, service or platform
 * implementation is explained there and here it is identical, changing the name of the property.
 *
 * <p>What is different is how much can be configured: here there are no comment, whitespace or
 * CDATA flags. It is not an omission -- they are options about <b>what is kept in the tree</b>, and
 * SAX builds no tree. What in DOM is a flag, in SAX is simply a handler method one does not write.
 *
 * <h2>Why {@code setFeature} throws three exceptions</h2>
 *
 * <p>They are three different answers and it is worth telling them apart: {@link
 * SAXNotRecognizedException} is "I do not know what that is", {@link SAXNotSupportedException} is
 * "I know what it is but I do not do it", and {@link ParserConfigurationException} is "I do it, but
 * not with the rest of what you already asked me". The last is the only one fixed by changing
 * something else.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>{@link #newDefaultInstance} throws {@link FactoryConfigurationError}; see the equivalent note
 * in {@link DocumentBuilderFactory}.
 */
public abstract class SAXParserFactory {

    /** The system property that names the factory. */
    private static final String PROPERTY = "javax.xml.parsers.SAXParserFactory";

    private boolean namespaceAware = false;
    private boolean validating = false;

    /** For the subclasses. */
    protected SAXParserFactory() {
    }

    /**
     * The platform's factory, already set to {@code namespaceAware}.
     *
     * @throws FactoryConfigurationError always in KajiLibrary; see the class note
     */
    public static SAXParserFactory newDefaultNSInstance() {
        SAXParserFactory factory = newDefaultInstance();
        factory.setNamespaceAware(true);
        return factory;
    }

    /** Like {@link #newInstance()}, already set to {@code namespaceAware}. */
    public static SAXParserFactory newNSInstance() {
        SAXParserFactory factory = newInstance();
        factory.setNamespaceAware(true);
        return factory;
    }

    /** Like {@link #newInstance(String, ClassLoader)}, already set to {@code namespaceAware}. */
    public static SAXParserFactory newNSInstance(String factoryClassName,
                                                 ClassLoader classLoader) {
        SAXParserFactory factory = newInstance(factoryClassName, classLoader);
        factory.setNamespaceAware(true);
        return factory;
    }

    /**
     * The platform's factory, without looking at properties or services.
     *
     * @throws FactoryConfigurationError always in KajiLibrary; see the class note
     */
    public static SAXParserFactory newDefaultInstance() {
        throw new FactoryConfigurationError(
            "KajiLibrary does not include a built-in XML implementation; set the system property "
                + PROPERTY + " or register a SAXParserFactory service");
    }

    /**
     * The configured factory, searched for in order.
     *
     * @throws FactoryConfigurationError if there is none
     */
    public static SAXParserFactory newInstance() {
        String configured = null;
        try {
            configured = System.getProperty(PROPERTY);
        } catch (SecurityException e) {
            // Without permission to read it: carry on with the services.
        }
        if (configured != null && configured.length() > 0) {
            return newInstance(configured, null);
        }
        ServiceLoader<SAXParserFactory> loader = ServiceLoader.load(SAXParserFactory.class);
        Iterator<SAXParserFactory> it = loader.iterator();
        if (it.hasNext()) {
            return it.next();
        }
        return newDefaultInstance();
    }

    /**
     * That factory and no other.
     *
     * @param classLoader the one it is loaded with; null means the context one or this class's
     * @throws FactoryConfigurationError if it cannot be built
     */
    public static SAXParserFactory newInstance(String factoryClassName, ClassLoader classLoader) {
        if (factoryClassName == null) {
            throw new FactoryConfigurationError("factoryClassName cannot be null");
        }
        try {
            ClassLoader loader = classLoader;
            if (loader == null) {
                loader = Thread.currentThread().getContextClassLoader();
            }
            if (loader == null) {
                loader = SAXParserFactory.class.getClassLoader();
            }
            Class<?> found = Class.forName(factoryClassName, false, loader);
            return (SAXParserFactory) found.getConstructor(new Class<?>[0])
                .newInstance(new Object[0]);
        } catch (ClassCastException e) {
            throw new FactoryConfigurationError(
                e, factoryClassName + " is not a SAXParserFactory");
        } catch (Exception e) {
            throw new FactoryConfigurationError(e, "Provider " + factoryClassName + " not found");
        }
    }

    /**
     * A parser with the configuration the factory has now.
     *
     * @throws ParserConfigurationException if this implementation cannot provide what was asked
     * @throws SAXException if the underlying parser fails while being built
     */
    public abstract SAXParser newSAXParser() throws ParserConfigurationException, SAXException;

    /** Ver {@link DocumentBuilderFactory#setNamespaceAware}. */
    public void setNamespaceAware(boolean awareness) {
        this.namespaceAware = awareness;
    }

    /** Whether the parsers validate against the document's DTD. */
    public void setValidating(boolean validating) {
        this.validating = validating;
    }

    /** Ver {@link #setNamespaceAware}. */
    public boolean isNamespaceAware() {
        return this.namespaceAware;
    }

    /** Ver {@link #setValidating}. */
    public boolean isValidating() {
        return this.validating;
    }

    /**
     * A SAX or implementation flag.
     *
     * <p>See the class note on the three exceptions.
     */
    public abstract void setFeature(String name, boolean value)
        throws ParserConfigurationException, SAXNotRecognizedException, SAXNotSupportedException;

    /** The value of a flag. */
    public abstract boolean getFeature(String name)
        throws ParserConfigurationException, SAXNotRecognizedException, SAXNotSupportedException;

    /**
     * The schema the parsers coming out of here validate with, or null.
     *
     * @throws UnsupportedOperationException by default
     */
    public Schema getSchema() {
        throw new UnsupportedOperationException(
            "This parser does not support specification \"XML Schema\".");
    }

    /**
     * Sets the schema.
     *
     * <p>Do not mix it with {@link #setValidating}; see {@link DocumentBuilderFactory#setSchema}.
     *
     * @param schema null removes it
     * @throws UnsupportedOperationException by default
     */
    public void setSchema(Schema schema) {
        throw new UnsupportedOperationException(
            "This parser does not support specification \"XML Schema\".");
    }

    /**
     * Asks for XInclude to be resolved.
     *
     * <p>Asking for false does nothing; see {@link DocumentBuilderFactory#setXIncludeAware}.
     *
     * @throws UnsupportedOperationException when asking for true on an implementation that does not
     *     support it
     */
    public void setXIncludeAware(boolean state) {
        if (state) {
            throw new UnsupportedOperationException(
                "This parser does not support specification \"XInclude\".");
        }
    }

    /**
     * Whether it resolves XInclude.
     *
     * @throws UnsupportedOperationException by default
     */
    public boolean isXIncludeAware() {
        throw new UnsupportedOperationException(
            "This parser does not support specification \"XInclude\".");
    }
}
