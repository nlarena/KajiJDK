package javax.xml.validation;

import java.io.File;
import java.net.URL;
import java.util.Iterator;
import java.util.ServiceLoader;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

/**
 * KajiLibrary's javax.xml.validation.SchemaFactory -- reads schemas and compiles them.
 *
 * <p>A factory <b>per schema language</b>: it is asked for with the language's URI --XML Schema,
 * RELAX NG-- and returns an implementation that understands it. It is the difference from {@code
 * DocumentBuilderFactory}, which has a single no-argument {@code newInstance}: there there is only
 * one possible XML and here there are several competing schema languages.
 *
 * <h2>How it is chosen</h2>
 *
 * <p>{@link #newInstance(String)} searches in order: the system property {@code
 * javax.xml.validation.SchemaFactory:<language>} --with the language's URI stuck to the name, which
 * is what allows configuring each one separately--, then the providers registered as a service,
 * keeping the first that <b>says it supports</b> that language, and finally the implementation
 * included in the platform. (The JDK also reads {@code $java.home/conf/jaxp.properties} after the
 * property; this does not.)
 *
 * <p>That there is none is an {@link IllegalArgumentException} and not a configuration error. It
 * makes sense: the argument was a language nobody knows how to read, and that is a problem of the
 * request.
 *
 * <h2>The four {@code newSchema}s</h2>
 *
 * <p>The three with an argument build a {@code Source} and call the one that receives an array. The
 * array one is the interesting one: it compiles <b>several documents as a single schema</b>, which
 * is what is needed when a schema is split into files that import each other. It is not the same as
 * compiling each separately -- the cross references only close if they are all together.
 *
 * <p>{@link #newSchema()} without arguments is the oddest and sometimes the most useful: it returns
 * a "special" schema that validates each document against <b>whatever the document itself
 * declares</b> with {@code xsi:schemaLocation}. It is convenient and it is exactly what should not
 * be done if the document comes from outside: it lets the document choose its own rules.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>{@link #newDefaultInstance} throws {@link SchemaFactoryConfigurationError} because this
 * library comes with no included schema implementation, and {@link #newInstance(String)} ends in
 * {@link IllegalArgumentException} as long as none is registered. Both are ways out those methods
 * already declare; with a registered provider, they work as in the JDK.
 */
public abstract class SchemaFactory {

    /** The prefix of the system property; the language's URI is appended to it. */
    private static final String PROPERTY_PREFIX = "javax.xml.validation.SchemaFactory:";

    /** For the subclasses. */
    protected SchemaFactory() {
    }

    /**
     * The implementation included in the platform.
     *
     * @throws SchemaFactoryConfigurationError always in KajiLibrary; see the class note
     */
    public static SchemaFactory newDefaultInstance() {
        throw new SchemaFactoryConfigurationError(
            "KajiLibrary does not include a built-in schema implementation; set the system property "
                + PROPERTY_PREFIX + "<schemaLanguage> or register a SchemaFactory service");
    }

    /**
     * The factory that understands that language.
     *
     * <p>See the search order in the class note.
     *
     * @throws IllegalArgumentException if none supports it
     * @throws NullPointerException if the language is null
     */
    public static SchemaFactory newInstance(String schemaLanguage) {
        if (schemaLanguage == null) {
            throw new NullPointerException("schemaLanguage cannot be null");
        }
        String configured = null;
        try {
            configured = System.getProperty(PROPERTY_PREFIX + schemaLanguage);
        } catch (SecurityException e) {
            // Without permission to read it: carry on with the services.
        }
        if (configured != null && configured.length() > 0) {
            return newInstance(schemaLanguage, configured, null);
        }
        ServiceLoader<SchemaFactory> loader = ServiceLoader.load(SchemaFactory.class);
        Iterator<SchemaFactory> it = loader.iterator();
        while (it.hasNext()) {
            SchemaFactory candidate = it.next();
            // Each one is asked: a registered provider need not know every language, and keeping
            // the first one blindly would give a factory that is no good.
            if (candidate.isSchemaLanguageSupported(schemaLanguage)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException(
            "No SchemaFactory that implements the schema language specified by: " + schemaLanguage
                + " could be loaded");
    }

    /**
     * That class and no other, for that language.
     *
     * @param classLoader the one it is loaded with; null means the context one or this class's
     * @throws IllegalArgumentException if it cannot be built, or if the one built does not support
     *     that language
     */
    public static SchemaFactory newInstance(String schemaLanguage, String factoryClassName,
                                            ClassLoader classLoader) {
        if (schemaLanguage == null) {
            throw new NullPointerException("schemaLanguage cannot be null");
        }
        if (factoryClassName == null) {
            throw new IllegalArgumentException("factoryClassName cannot be null");
        }
        SchemaFactory made;
        try {
            ClassLoader loader = classLoader;
            if (loader == null) {
                loader = Thread.currentThread().getContextClassLoader();
            }
            if (loader == null) {
                loader = SchemaFactory.class.getClassLoader();
            }
            Class<?> found = Class.forName(factoryClassName, false, loader);
            made = (SchemaFactory) found.getConstructor(new Class<?>[0]).newInstance(new Object[0]);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                "Provider " + factoryClassName + " could not be instantiated: " + e);
        }
        if (!made.isSchemaLanguageSupported(schemaLanguage)) {
            throw new IllegalArgumentException(
                factoryClassName + " does not support the schema language " + schemaLanguage);
        }
        return made;
    }

    /**
     * Whether this factory understands that language.
     *
     * @throws NullPointerException if it is null
     * @throws IllegalArgumentException if it is the empty string
     */
    public abstract boolean isSchemaLanguageSupported(String schemaLanguage);

    /**
     * The value of a flag.
     *
     * <p>By default it knows none. The one every implementation has to recognize is {@code
     * javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING}, which is the one that turns off access to
     * external resources.
     *
     * @throws SAXNotRecognizedException if it does not know that name
     */
    public boolean getFeature(String name)
        throws SAXNotRecognizedException, SAXNotSupportedException {
        if (name == null) {
            throw new NullPointerException("the name parameter is null");
        }
        throw new SAXNotRecognizedException(name);
    }

    /**
     * Changes a flag.
     *
     * @throws SAXNotRecognizedException if it does not know that name
     */
    public void setFeature(String name, boolean value)
        throws SAXNotRecognizedException, SAXNotSupportedException {
        if (name == null) {
            throw new NullPointerException("the name parameter is null");
        }
        throw new SAXNotRecognizedException(name);
    }

    /**
     * Changes a property.
     *
     * @throws SAXNotRecognizedException if it does not know that name
     */
    public void setProperty(String name, Object object)
        throws SAXNotRecognizedException, SAXNotSupportedException {
        if (name == null) {
            throw new NullPointerException("the name parameter is null");
        }
        throw new SAXNotRecognizedException(name);
    }

    /**
     * The value of a property.
     *
     * @throws SAXNotRecognizedException if it does not know that name
     */
    public Object getProperty(String name)
        throws SAXNotRecognizedException, SAXNotSupportedException {
        if (name == null) {
            throw new NullPointerException("the name parameter is null");
        }
        throw new SAXNotRecognizedException(name);
    }

    /** Who receives the errors when <b>compiling the schema</b>, not when validating documents. */
    public abstract void setErrorHandler(ErrorHandler errorHandler);

    /** Ver {@link #setErrorHandler}. */
    public abstract ErrorHandler getErrorHandler();

    /** Who resolves whatever the schema imports or includes. */
    public abstract void setResourceResolver(LSResourceResolver resourceResolver);

    /** Ver {@link #setResourceResolver}. */
    public abstract LSResourceResolver getResourceResolver();

    /**
     * Compiles a schema from a source.
     *
     * @throws SAXException if the schema is wrong
     * @throws NullPointerException if the source is null
     */
    public Schema newSchema(Source schema) throws SAXException {
        if (schema == null) {
            throw new NullPointerException("schema cannot be null");
        }
        return newSchema(new Source[] {schema});
    }

    /**
     * Likewise, from a file.
     *
     * @throws NullPointerException if the file is null
     */
    public Schema newSchema(File schema) throws SAXException {
        if (schema == null) {
            throw new NullPointerException("schema cannot be null");
        }
        return newSchema(new StreamSource(schema));
    }

    /**
     * Likewise, from a URL.
     *
     * @throws NullPointerException if the URL is null
     */
    public Schema newSchema(URL schema) throws SAXException {
        if (schema == null) {
            throw new NullPointerException("schema cannot be null");
        }
        return newSchema(new StreamSource(schema.toExternalForm()));
    }

    /**
     * Compiles <b>several</b> sources as a single schema.
     *
     * <p>See the class note on why it is not the same as compiling them separately.
     */
    public abstract Schema newSchema(Source[] schemas) throws SAXException;

    /**
     * The schema that validates each document against whatever the document declares.
     *
     * <p>Convenient and dangerous; see the class note.
     */
    public abstract Schema newSchema() throws SAXException;
}
