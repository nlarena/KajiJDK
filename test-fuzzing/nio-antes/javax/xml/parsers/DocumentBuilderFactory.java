package javax.xml.parsers;

import java.util.Iterator;
import java.util.ServiceLoader;
import javax.xml.validation.Schema;

/**
 * KajiLibrary's javax.xml.parsers.DocumentBuilderFactory -- where the {@link DocumentBuilder}s come
 * from.
 *
 * <p>It is a factory and not a constructor because the XML implementation is replaceable: whoever
 * writes the program asks for "a DOM parser" and whoever puts the deployment together decides
 * which. The six {@code newInstance}s are the variants of that choice.
 *
 * <h2>The three ways of choosing</h2>
 *
 * <ul>
 *   <li>{@link #newInstance()} <b>searches</b>: first the system property {@code
 *       javax.xml.parsers.DocumentBuilderFactory}, then the providers registered as a service, and
 *       if there is nothing, the implementation included in the platform. (The JDK also reads
 *       {@code $java.home/conf/jaxp.properties} between the property and the services; this does
 *       not.)
 *   <li>{@link #newInstance(String, ClassLoader)} does not search: it uses that class or fails. It
 *       serves when a program needs <b>one</b> concrete implementation and does not want a system
 *       property to swap it behind its back;
 *   <li>{@link #newDefaultInstance()} skips the search the other way: it goes straight to the
 *       platform's, ignoring properties and services.
 * </ul>
 *
 * <p>The {@code NS} variants are the same but return the factory already set to {@code
 * namespaceAware}. They exist because that default value is <b>false</b> for historical reasons, it
 * is almost always the wrong one, and forgetting to change it gives a confusing symptom: the
 * elements appear with the prefix stuck to the name and the searches by namespace find nothing.
 *
 * <h2>The flags and their defaults</h2>
 *
 * <p>They all start at false except {@link #isExpandEntityReferences}, which starts at true. That
 * is the one worth looking at: with entities expanded, a document that declares an external entity
 * makes the parser go and fetch it, and from there come both the reading of local files and the
 * network requests the program never asked for.
 *
 * <h2>Validating by schema or by DTD</h2>
 *
 * <p>{@link #setSchema} and {@link #setValidating} are two <b>different</b> mechanisms and they
 * should not be mixed: the second validates against the DTD the document declares, the first
 * against a schema the application chooses. Setting both is a configuration error, and the
 * underlying difference is who is in charge: with DTD, the document; with a schema, whoever reads
 * it.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>{@link #newDefaultInstance} throws {@link FactoryConfigurationError}, because KajiLibrary
 * comes with no included XML implementation. It is the way out that method already declares for the
 * "no factory" case, and that is why {@link #newInstance()} works just as in the JDK as long as
 * somebody registers one: it only fails when there is none, which is the truth.
 */
public abstract class DocumentBuilderFactory {

    /** The system property that names the factory. */
    private static final String PROPERTY = "javax.xml.parsers.DocumentBuilderFactory";

    private boolean namespaceAware = false;
    private boolean validating = false;
    private boolean whitespace = false;
    private boolean expandEntityRef = true;
    private boolean ignoreComments = false;
    private boolean coalescing = false;

    /** For the subclasses. */
    protected DocumentBuilderFactory() {
    }

    /**
     * The factory included in the platform, already set to {@code namespaceAware}.
     *
     * @throws FactoryConfigurationError always in KajiLibrary; see the class note
     */
    public static DocumentBuilderFactory newDefaultNSInstance() {
        DocumentBuilderFactory factory = newDefaultInstance();
        factory.setNamespaceAware(true);
        return factory;
    }

    /** Like {@link #newInstance()}, already set to {@code namespaceAware}. */
    public static DocumentBuilderFactory newNSInstance() {
        DocumentBuilderFactory factory = newInstance();
        factory.setNamespaceAware(true);
        return factory;
    }

    /** Like {@link #newInstance(String, ClassLoader)}, already set to {@code namespaceAware}. */
    public static DocumentBuilderFactory newNSInstance(String factoryClassName,
                                                       ClassLoader classLoader) {
        DocumentBuilderFactory factory = newInstance(factoryClassName, classLoader);
        factory.setNamespaceAware(true);
        return factory;
    }

    /**
     * The factory included in the platform, without looking at properties or services.
     *
     * @throws FactoryConfigurationError always in KajiLibrary; see the class note
     */
    public static DocumentBuilderFactory newDefaultInstance() {
        throw new FactoryConfigurationError(
            "KajiLibrary does not include a built-in XML implementation; set the system property "
                + PROPERTY + " or register a DocumentBuilderFactory service");
    }

    /**
     * The configured factory, searched for in order.
     *
     * <p>See the three steps in the class note.
     *
     * @throws FactoryConfigurationError if there is none
     */
    public static DocumentBuilderFactory newInstance() {
        String configured = null;
        try {
            configured = System.getProperty(PROPERTY);
        } catch (SecurityException e) {
            // Without permission to read it: carry on with the services, which is the same as doing
            // nothing.
        }
        if (configured != null && configured.length() > 0) {
            return newInstance(configured, null);
        }
        ServiceLoader<DocumentBuilderFactory> loader =
            ServiceLoader.load(DocumentBuilderFactory.class);
        Iterator<DocumentBuilderFactory> it = loader.iterator();
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
    public static DocumentBuilderFactory newInstance(String factoryClassName,
                                                     ClassLoader classLoader) {
        if (factoryClassName == null) {
            throw new FactoryConfigurationError("factoryClassName cannot be null");
        }
        try {
            ClassLoader loader = classLoader;
            if (loader == null) {
                loader = Thread.currentThread().getContextClassLoader();
            }
            if (loader == null) {
                loader = DocumentBuilderFactory.class.getClassLoader();
            }
            Class<?> found = Class.forName(factoryClassName, false, loader);
            return (DocumentBuilderFactory) found.getConstructor(new Class<?>[0])
                .newInstance(new Object[0]);
        } catch (ClassCastException e) {
            throw new FactoryConfigurationError(
                e, factoryClassName + " is not a DocumentBuilderFactory");
        } catch (Exception e) {
            throw new FactoryConfigurationError(e, "Provider " + factoryClassName + " not found");
        }
    }

    /**
     * A parser with the configuration the factory has now.
     *
     * <p>Later changes to the factory do not affect it: what is read when building it stays fixed.
     *
     * @throws ParserConfigurationException if this implementation cannot provide what was asked
     */
    public abstract DocumentBuilder newDocumentBuilder() throws ParserConfigurationException;

    /** See the class note on why it almost always has to be set to true. */
    public void setNamespaceAware(boolean awareness) {
        this.namespaceAware = awareness;
    }

    /** Whether the parsers validate against the document's DTD. */
    public void setValidating(boolean validating) {
        this.validating = validating;
    }

    /**
     * Whether the whitespace the DTD declares as padding is discarded.
     *
     * <p>It only does something with validation on: without a DTD there is no way of knowing which
     * space is significant and which is indentation.
     */
    public void setIgnoringElementContentWhitespace(boolean whitespace) {
        this.whitespace = whitespace;
    }

    /** See the class note on external entities. */
    public void setExpandEntityReferences(boolean expandEntityRef) {
        this.expandEntityRef = expandEntityRef;
    }

    /** Whether comments do not reach the tree. */
    public void setIgnoringComments(boolean ignoreComments) {
        this.ignoreComments = ignoreComments;
    }

    /**
     * Whether CDATA sections are merged with the text around them.
     *
     * <p>It is advisable: without this, the same text can arrive split into several nodes depending
     * on where the author opened a CDATA, and it has to be joined by hand on every read.
     */
    public void setCoalescing(boolean coalescing) {
        this.coalescing = coalescing;
    }

    /** Ver {@link #setNamespaceAware}. */
    public boolean isNamespaceAware() {
        return this.namespaceAware;
    }

    /** Ver {@link #setValidating}. */
    public boolean isValidating() {
        return this.validating;
    }

    /** Ver {@link #setIgnoringElementContentWhitespace}. */
    public boolean isIgnoringElementContentWhitespace() {
        return this.whitespace;
    }

    /** See {@link #setExpandEntityReferences}. It starts at <b>true</b>. */
    public boolean isExpandEntityReferences() {
        return this.expandEntityRef;
    }

    /** Ver {@link #setIgnoringComments}. */
    public boolean isIgnoringComments() {
        return this.ignoreComments;
    }

    /** Ver {@link #setCoalescing}. */
    public boolean isCoalescing() {
        return this.coalescing;
    }

    /**
     * An implementation-specific attribute.
     *
     * @throws IllegalArgumentException if it does not recognize it
     */
    public abstract void setAttribute(String name, Object value) throws IllegalArgumentException;

    /** The value of an implementation-specific attribute. */
    public abstract Object getAttribute(String name) throws IllegalArgumentException;

    /**
     * An implementation-specific flag.
     *
     * <p>The only one every implementation has to recognize is
     * {@code javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING}.
     *
     * @throws ParserConfigurationException if it does not recognize it or cannot provide it
     */
    public abstract void setFeature(String name, boolean value)
        throws ParserConfigurationException;

    /** The value of a flag. */
    public abstract boolean getFeature(String name) throws ParserConfigurationException;

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
     * Sets the schema. See the class note on not mixing it with {@link #setValidating}.
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
     * <p>Asking for false does nothing, because not asking is the default state. Asking for true
     * throws if this implementation cannot do it -- which is right: going on silently would leave a
     * half-built document without anybody finding out.
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
     * @throws UnsupportedOperationException by default; see {@link DocumentBuilder#isXIncludeAware}
     */
    public boolean isXIncludeAware() {
        throw new UnsupportedOperationException(
            "This parser does not support specification \"XInclude\".");
    }
}
