package javax.xml.xpath;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * KajiLibrary's javax.xml.xpath.XPathFactory -- where {@link XPath} objects come from.
 *
 * <p>It is requested by <b>object model</b>: the URI of the document representation the evaluation
 * will run on. The platform ships only one, {@link #DEFAULT_OBJECT_MODEL_URI}, which is DOM. The
 * indirection exists because XPath does not depend on DOM in principle -- it can be evaluated on
 * other representations-- and this is the point where that is chosen.
 *
 * <p>The system property that configures it is not a fixed name: it is {@link
 * #DEFAULT_PROPERTY_NAME} <b>plus a colon and the model's URI</b>, so each model is configured
 * separately. That detail cannot be guessed.
 *
 * <h2>Three ways to fail, and they are different</h2>
 *
 * <ul>
 *   <li>{@link #newInstance(String)} with a model nobody supports throws {@link
 *       XPathFactoryConfigurationException}, which is <b>checked</b>: there is no support for that
 *       particular thing and the program can try something else;
 *   <li>with null it throws {@link NullPointerException} and with the empty string
 *       {@link IllegalArgumentException}: those are not models, they are malformed arguments;
 *   <li>{@link #newInstance()} --the one that takes no model-- throws a {@link RuntimeException} if
 *       it does not find DOM, because it has no way to declare a checked one.
 * </ul>
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>This library ships no XPath evaluator: that takes an expression parser, an axis engine and a
 * live DOM implementation, and none of the three is here. Without a registered factory,
 * {@link #newDefaultInstance} and {@link #newInstance()} throw, and {@link #newInstance(String)}
 * throws the checked exception it already declares. The lookup by system property and by service is
 * really implemented, so registering an implementation is enough. Unlike the JDK, the lookup does
 * not read {@code jaxp.properties} (the same gap as in {@code javax.xml.parsers}).
 */
public abstract class XPathFactory {

    /** The prefix of the system property; {@code ":"} and the model's URI are appended to it. */
    public static final String DEFAULT_PROPERTY_NAME = "javax.xml.xpath.XPathFactory";

    /** The DOM model, which is the one the platform ships. */
    public static final String DEFAULT_OBJECT_MODEL_URI = "http://java.sun.com/jaxp/xpath/dom";

    /** For the subclasses. */
    protected XPathFactory() {
    }

    /**
     * The implementation built into the platform.
     *
     * @throws RuntimeException always in KajiLibrary; see the class note
     */
    public static XPathFactory newDefaultInstance() {
        throw new RuntimeException(
            "KajiLibrary does not include a built-in XPath implementation; set the system property "
                + DEFAULT_PROPERTY_NAME + ":" + DEFAULT_OBJECT_MODEL_URI
                + " or register an XPathFactory service");
    }

    /**
     * The factory for DOM.
     *
     * @throws RuntimeException if there is none
     */
    public static XPathFactory newInstance() {
        try {
            return newInstance(DEFAULT_OBJECT_MODEL_URI);
        } catch (XPathFactoryConfigurationException e) {
            // The method declares no checked exceptions: wrap it, which is what the JDK does.
            throw new RuntimeException(
                "XPathFactory#newInstance() failed to create an XPathFactory for the default "
                    + "object model: " + DEFAULT_OBJECT_MODEL_URI, e);
        }
    }

    /**
     * The factory for that object model.
     *
     * <p>It looks, in order, at: that model's system property, the providers registered as a
     * service --keeping the first one that <b>says it supports</b> the model-- and the platform
     * implementation. It does not read {@code jaxp.properties}; see the class note.
     *
     * @throws NullPointerException if the URI is null
     * @throws IllegalArgumentException if it is the empty string
     * @throws XPathFactoryConfigurationException if nobody supports that model
     */
    public static XPathFactory newInstance(String uri) throws XPathFactoryConfigurationException {
        if (uri == null) {
            throw new NullPointerException(
                "XPathFactory#newInstance(String uri) cannot be called with uri == null");
        }
        if (uri.length() == 0) {
            throw new IllegalArgumentException(
                "XPathFactory#newInstance(String uri) cannot be called with uri == \"\"");
        }
        String configured = null;
        try {
            configured = System.getProperty(DEFAULT_PROPERTY_NAME + ":" + uri);
        } catch (SecurityException e) {
            // No permission to read it: carry on with the services.
        }
        if (configured != null && configured.length() > 0) {
            return newInstance(uri, configured, null);
        }
        ServiceLoader<XPathFactory> loader = ServiceLoader.load(XPathFactory.class);
        Iterator<XPathFactory> it = loader.iterator();
        while (it.hasNext()) {
            XPathFactory candidate = it.next();
            if (candidate.isObjectModelSupported(uri)) {
                return candidate;
            }
        }
        throw new XPathFactoryConfigurationException(
            "No XPathFactory implementation found for the object model: " + uri);
    }

    /**
     * That class and no other, for that model.
     *
     * @param classLoader the one to load it with; null means the context one or this class's
     * @throws XPathFactoryConfigurationException if it cannot be built, or if the built one does
     *     not support that model
     */
    public static XPathFactory newInstance(String uri, String factoryClassName,
                                           ClassLoader classLoader)
        throws XPathFactoryConfigurationException {
        if (uri == null) {
            throw new NullPointerException(
                "XPathFactory#newInstance(String uri) cannot be called with uri == null");
        }
        if (uri.length() == 0) {
            throw new IllegalArgumentException(
                "XPathFactory#newInstance(String uri) cannot be called with uri == \"\"");
        }
        if (factoryClassName == null) {
            throw new XPathFactoryConfigurationException("factoryClassName cannot be null");
        }
        XPathFactory made;
        try {
            ClassLoader loader = classLoader;
            if (loader == null) {
                loader = Thread.currentThread().getContextClassLoader();
            }
            if (loader == null) {
                loader = XPathFactory.class.getClassLoader();
            }
            Class<?> found = Class.forName(factoryClassName, false, loader);
            made = (XPathFactory) found.getConstructor(new Class<?>[0]).newInstance(new Object[0]);
        } catch (Exception e) {
            throw new XPathFactoryConfigurationException(
                "Provider " + factoryClassName + " could not be instantiated: " + e);
        }
        if (!made.isObjectModelSupported(uri)) {
            throw new XPathFactoryConfigurationException(
                factoryClassName + " does not support the object model " + uri);
        }
        return made;
    }

    /** Whether this factory works on that object model. */
    public abstract boolean isObjectModelSupported(String objectModel);

    /**
     * Sets a feature.
     *
     * <p>The one every implementation must recognize is
     * {@code javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING}, which among other things turns off
     * extension functions.
     */
    public abstract void setFeature(String name, boolean value)
        throws XPathFactoryConfigurationException;

    /** The value of a feature. */
    public abstract boolean getFeature(String name) throws XPathFactoryConfigurationException;

    /** The variable resolver the {@link XPath} objects made here will carry. */
    public abstract void setXPathVariableResolver(XPathVariableResolver resolver);

    /** Same for the functions. */
    public abstract void setXPathFunctionResolver(XPathFunctionResolver resolver);

    /** An evaluator with the factory's current configuration. */
    public abstract XPath newXPath();

    /**
     * An implementation property.
     *
     * @throws UnsupportedOperationException by default: it arrived after the class, and an old
     *     implementation does not know it
     */
    public void setProperty(String name, String value) {
        throw new UnsupportedOperationException(
            "This XPathFactory does not support the setProperty method.");
    }

    /**
     * The value of a property.
     *
     * @throws UnsupportedOperationException by default
     */
    public String getProperty(String name) {
        throw new UnsupportedOperationException(
            "This XPathFactory does not support the getProperty method.");
    }
}
