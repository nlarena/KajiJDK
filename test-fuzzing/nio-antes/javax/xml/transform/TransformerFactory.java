package javax.xml.transform;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Properties;
import java.util.ServiceLoader;

/**
 * KajiLibrary's javax.xml.transform.TransformerFactory -- where the transformers come from.
 *
 * <p>It is the entry point of the whole XSLT API and, more interestingly, the canonical example of
 * the JAXP pluggable factory pattern: the application's code names **this** abstract class and
 * never a concrete processor, and {@link #newInstance()} discovers at run time which one is
 * installed. Switching from Xalan to Saxon is changing the classpath, not the code.
 *
 * <h2>The search order, which is the contract</h2>
 *
 * <p>{@link #newInstance()} looks, and keeps the first it finds:
 *
 * <ol>
 *   <li>the system property {@code javax.xml.transform.TransformerFactory};
 *   <li>the file {@code $java.home/conf/jaxp.properties}, with that same key;
 *   <li>the providers declared via {@link ServiceLoader}, that is
 *       {@code META-INF/services/javax.xml.transform.TransformerFactory} on the classpath;
 *   <li>the platform's default implementation.
 * </ol>
 *
 * <p>The order is not arbitrary and it explains what each step is for: the system property beats
 * everything because it is what one can change without touching the deployment, the file is the
 * installation's configuration, and the `ServiceLoader` is what a jar brings just by being there.
 * The first three are implemented here as they are. **The fourth does not exist in this library**,
 * and everything that follows comes from that.
 *
 * <h2>What is written here and what is not, and why</h2>
 *
 * <p>The API is whole: the twelve abstract operations, the three static entry points and the
 * protected constructor. (The note said thirteen operations.) What there is **not** is an XSLT
 * processor. XSLT is a complete transformation language --its own syntax, its tree model, and XPath
 * inside-- and writing it is a separate project, not a member of this class.
 *
 * <p>So {@link #newInstance()} walks the three steps that are there and, when none gives anything,
 * throws {@link TransformerFactoryConfigurationError} -- which is **exactly** what the JDK does
 * when it finds no implementation, and that is why it is not a stub but the path the contract
 * foresees. That the real JDK almost never takes it is an accident of it bringing Xalan inside, not
 * another rule.
 *
 * <p>The tempting alternative --returning a factory whose `Transformer` copies the input to the
 * output-- was discarded on purpose, and the reason is worth leaving written because it seems more
 * useful: a transformer that does not transform **fails silently**. The caller receives a document,
 * takes it as transformed, and the stylesheet was never applied. An error when building the factory
 * is seen on the first run; an untransformed document is seen when it is already in production. Of
 * the two, the only honest one is the one that breaks early.
 */
public abstract class TransformerFactory {

    /** The key, which is both the name of the service and that of the system property. */
    private static final String KEY = "javax.xml.transform.TransformerFactory";

    /** For the subclasses; there is no state to initialize. */
    protected TransformerFactory() {
    }

    // ---- discovery ---------------------------------------------------------------------------

    /**
     * The implementation **of the platform**, without looking at the configuration.
     *
     * <p>It skips the four steps of {@link #newInstance()} on purpose: it exists so that a piece
     * that needs the reference processor --and not the one the application may have plugged in--
     * can ask for it. It is the escape hatch from pluggability, not a shortcut.
     *
     * <p>Here there is none, so it always fails. See the class header.
     *
     * @return never returns
     * @throws TransformerFactoryConfigurationError always: this library brings no XSLT
     */
    public static TransformerFactory newDefaultInstance() {
        throw new TransformerFactoryConfigurationError(
                "No system-default TransformerFactory: this runtime ships no XSLT processor");
    }

    /**
     * The configured factory, looked for in the four steps of the header.
     *
     * @return the factory found
     * @throws TransformerFactoryConfigurationError if none of the steps gives one
     */
    public static TransformerFactory newInstance() throws TransformerFactoryConfigurationError {
        // 1. The system property.
        String className = null;
        try {
            className = System.getProperty(KEY);
        } catch (SecurityException ignored) {
            // Without permission to read it, it is the same as not being set: carry on to the next
            // step.
        }
        if (className != null && className.length() > 0) {
            return instantiate(className, null);
        }

        // 2. $java.home/conf/jaxp.properties.
        className = fromJaxpProperties();
        if (className != null && className.length() > 0) {
            return instantiate(className, null);
        }

        // 3. The providers declared on the classpath.
        TransformerFactory fromService = fromServiceLoader();
        if (fromService != null) {
            return fromService;
        }

        // 4. The default implementation, which does not exist here.
        throw new TransformerFactoryConfigurationError("Provider for " + KEY + " cannot be found");
    }

    /**
     * A factory of a named class, without any discovery.
     *
     * <p>For when the application needs **two** processors at once and one chosen globally does not
     * do.
     *
     * @param factoryClassName the fully qualified name of the class
     * @param classLoader what to load it with; null uses the one that corresponds by default
     * @return the factory
     * @throws TransformerFactoryConfigurationError if the class is not there or cannot be
     *     instantiated
     */
    public static TransformerFactory newInstance(String factoryClassName, ClassLoader classLoader)
            throws TransformerFactoryConfigurationError {
        if (factoryClassName == null) {
            // The JDK gets here with an inner NullPointerException and reports it wrapped; the same
            // text is reproduced because there is code that reads it.
            NullPointerException e = new NullPointerException();
            throw new TransformerFactoryConfigurationError(
                    e, "Provider " + factoryClassName + " could not be instantiated: " + e);
        }
        return instantiate(factoryClassName, classLoader);
    }

    // ---- the nuts and bolts of discovery --------------------------------------------------------

    /**
     * Loads and instantiates the named class, with the error messages the contract defines.
     *
     * <p>The two cases are told apart because they are fixed differently: **not found** is a
     * missing jar, **could not be instantiated** is a class that is there but is no good --without
     * a no-argument constructor, or not a `TransformerFactory`--.
     */
    private static TransformerFactory instantiate(String className, ClassLoader loader) {
        Class<?> cls;
        try {
            if (loader == null) {
                cls = Class.forName(className);
            } else {
                cls = Class.forName(className, false, loader);
            }
        } catch (ClassNotFoundException e) {
            throw new TransformerFactoryConfigurationError(
                    e, "Provider " + className + " not found");
        }
        Object obj;
        try {
            obj = cls.newInstance();
        } catch (Exception e) {
            throw new TransformerFactoryConfigurationError(
                    e, "Provider " + className + " could not be instantiated: " + e);
        }
        if (!(obj instanceof TransformerFactory)) {
            ClassCastException e = new ClassCastException(className + " cannot be cast to " + KEY);
            throw new TransformerFactoryConfigurationError(
                    e, "Provider " + className + " could not be instantiated: " + e);
        }
        return (TransformerFactory) obj;
    }

    /**
     * The class name {@code $java.home/conf/jaxp.properties} declares, or null.
     *
     * <p>Without a cache on purpose: the JDK reads the file only once per VM, and that is a
     * performance decision that buys nothing here --this path is walked when someone asks for a
     * factory, not in a loop-- and that in exchange makes it impossible to test.
     *
     * <p>Any read failure returns null instead of propagating: the file is **optional**, and not
     * being able to read it is not a configuration error but the absence of configuration. In this
     * VM {@code java.home} is not defined, so this step contributes nothing yet; the code is
     * written for the day it is.
     */
    private static String fromJaxpProperties() {
        try {
            String home = System.getProperty("java.home");
            if (home == null) {
                return null;
            }
            File f = new File(new File(new File(home), "conf"), "jaxp.properties");
            if (!f.exists()) {
                return null;
            }
            Properties props = new Properties();
            InputStream in = new FileInputStream(f);
            try {
                props.load(in);
            } finally {
                in.close();
            }
            return props.getProperty(KEY);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * The first factory a classpath provider declares, or null if there is none.
     *
     * <p>Today it always gives null, and not because of a shortcut here: this library's {@link
     * ServiceLoader} cannot enumerate {@code META-INF/services} because our {@code ClassLoader} has
     * no resources. The machinery is plugged in where it goes, so the day the resources exist this
     * step starts finding providers without touching a line.
     */
    private static TransformerFactory fromServiceLoader() {
        try {
            ServiceLoader<TransformerFactory> sl = ServiceLoader.load(TransformerFactory.class);
            Iterator<TransformerFactory> it = sl.iterator();
            if (it.hasNext()) {
                return it.next();
            }
        } catch (Throwable ignored) {
            // A broken provider cannot stop the next step from being tried.
        }
        return null;
    }

    // ---- the factory's contract -----------------------------------------------------------------

    /**
     * A transformer that applies the stylesheet of {@code source}.
     *
     * @param source the stylesheet
     * @return the transformer
     * @throws TransformerConfigurationException if the stylesheet cannot be compiled
     */
    public abstract Transformer newTransformer(Source source) throws TransformerConfigurationException;

    /**
     * A **copying** transformer: without a stylesheet, it moves the input to the output.
     *
     * <p>It is the only identity transformation the API defines, and it is right that it exists
     * because the caller asks for it explicitly and knows what they get. It serves for serializing:
     * it is given a tree and a stream, and the {@link OutputKeys} properties are used without
     * writing a serializer.
     *
     * @return the copying transformer
     * @throws TransformerConfigurationException if it cannot be built
     */
    public abstract Transformer newTransformer() throws TransformerConfigurationException;

    /**
     * Compiles the stylesheet once to reuse it many times.
     *
     * @param source the stylesheet
     * @return the compiled stylesheet
     * @throws TransformerConfigurationException if it cannot be compiled
     */
    public abstract Templates newTemplates(Source source) throws TransformerConfigurationException;

    /**
     * The stylesheet the document itself associates with {@code &lt;?xml-stylesheet?&gt;}.
     *
     * <p>The three criteria --media, title, charset-- filter among several instructions; null in
     * any of them means "I do not care about that one". It returns null if none matches, which is
     * not an error: a document need not bring a stylesheet.
     *
     * @param source the document
     * @param media the media looked for, or null
     * @param title the title looked for, or null
     * @param charset the charset looked for, or null
     * @return the source of the stylesheet, or null
     * @throws TransformerConfigurationException if the document cannot be read
     */
    public abstract Source getAssociatedStylesheet(Source source, String media, String title, String charset)
            throws TransformerConfigurationException;

    /**
     * Who resolves the `href`s of the transformers that come out of here.
     *
     * @param resolver the resolver, or null to go back to the default one
     */
    public abstract void setURIResolver(URIResolver resolver);

    /** The resolver in use, or null. */
    public abstract URIResolver getURIResolver();

    /**
     * Turns a feature on or off.
     *
     * <p>The only one the spec requires to be supported is {@code
     * javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING}, and that one **may refuse a no**: an
     * implementation that has it on is not obliged to let it be turned off, because secure mode can
     * be imposed by the environment.
     *
     * @param name the name of the feature
     * @param value whether it is wanted on
     * @throws TransformerConfigurationException if it is not recognized or cannot be set that way
     */
    public abstract void setFeature(String name, boolean value) throws TransformerConfigurationException;

    /**
     * Whether a feature is on.
     *
     * <p>A {@code false} is ambiguous on purpose: it can be "it is off" or "I do not know it". The
     * API does not tell them apart.
     *
     * @param name the name of the feature
     * @return whether it is supported and on
     */
    public abstract boolean getFeature(String name);

    /**
     * Sets an implementation-specific attribute.
     *
     * @param name the name of the attribute
     * @param value the value
     * @throws IllegalArgumentException if it is not recognized
     */
    public abstract void setAttribute(String name, Object value);

    /**
     * The value of an implementation-specific attribute.
     *
     * @param name the name of the attribute
     * @return the value
     * @throws IllegalArgumentException if it is not recognized
     */
    public abstract Object getAttribute(String name);

    /**
     * Who receives the errors **of compiling stylesheets**.
     *
     * <p>It is not the same listener as the {@link Transformer}'s: here the problems of setting up
     * the transformation are reported, there those of running it.
     *
     * @param listener the listener; cannot be null
     * @throws IllegalArgumentException if it is null
     */
    public abstract void setErrorListener(ErrorListener listener);

    /** The listener in use; never null. */
    public abstract ErrorListener getErrorListener();
}
