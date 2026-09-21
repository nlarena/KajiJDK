package javax.xml.transform;

import java.util.Properties;

/**
 * KajiLibrary's javax.xml.transform.Transformer -- applies a stylesheet to a document.
 *
 * <p>An abstract class and not an interface, and that is no historical accident: it defines a
 * single method with a body, {@link #reset}, which was added in Java 5 with a default
 * implementation that throws {@link UnsupportedOperationException}. In 2001 there were no `default`
 * methods, so the only way of adding a member without breaking everyone who already implemented the
 * API was for it to be a class. (The note said Java 6; the JDK marks it {@code @since 1.5}.)
 *
 * <p>A `Transformer` **has state and is not shared between threads**: the parameters and the output
 * properties are its own. What is shared is the {@link Templates} it came from. The same
 * transformer can be used for several consecutive transformations in the same thread, and that is
 * why {@link #reset} exists: cleaning is cheaper than asking for it again.
 *
 * <h2>What is written here and what is not</h2>
 *
 * <p>Everything the class declares; of implementation, only what the JDK also implements. The
 * twelve abstract methods are provided by an XSLT processor, and **this library comes with none**
 * -- see the header of {@link TransformerFactory}. It is not that a piece is missing: the abstract
 * class is exactly what the API defines, and a concrete subclass would be a whole XSLT processor.
 * (The note said thirteen abstract methods.)
 */
public abstract class Transformer {

    /** For the subclasses; there is no state to initialize. */
    protected Transformer() {
    }

    /**
     * Leaves the transformer as it came out of {@link Templates#newTransformer}.
     *
     * <p>It clears the parameters and output properties that were set on it; it does **not** touch
     * the {@link URIResolver} nor the {@link ErrorListener}, which are the caller's infrastructure
     * and not data of the job. It is a distinction that gets forgotten and later shows up as a
     * listener that stopped receiving errors halfway through a batch.
     *
     * <p>The default implementation throws {@link UnsupportedOperationException}: the method
     * arrived after the class, and a subclass written before does not know how to do it. Throwing
     * is right --doing nothing would be lying about an object that was left dirty--.
     *
     * @throws UnsupportedOperationException if the implementation does not support it
     */
    public void reset() {
        // The package can be null --a subclass in the default package-- and an error message that
        // throws NullPointerException is worse than having no message.
        Package p = this.getClass().getPackage();
        String title = (p == null) ? null : p.getSpecificationTitle();
        String version = (p == null) ? null : p.getSpecificationVersion();
        throw new UnsupportedOperationException(
                "This Transformer, \"" + this.getClass().getName() + "\", does not support the reset functionality."
                        + "  Specification \"" + title + "\""
                        + " version \"" + version + "\"");
    }

    /**
     * Transforms {@code xmlSource} and writes to {@code outputTarget}.
     *
     * @param xmlSource the input document
     * @param outputTarget where to leave the result
     * @throws TransformerException if the transformation fails
     */
    public abstract void transform(Source xmlSource, Result outputTarget) throws TransformerException;

    /**
     * Sets a parameter of the stylesheet.
     *
     * <p>The name can come qualified as {@code "{uri}local"}. Parameters belong to the transformer,
     * not to the transformation: they survive {@link #transform} and have to be cleared with {@link
     * #clearParameters} if the next job does not want them.
     *
     * @param name the name, possibly qualified
     * @param value the value
     */
    public abstract void setParameter(String name, Object value);

    /**
     * The value set with {@link #setParameter}, or null.
     *
     * <p>It returns what was set from Java, **not** what the stylesheet has as the default value
     * for that parameter: they are two different things and this API only sees the first.
     *
     * @param name the name, possibly qualified
     * @return the value, or null if it was not set
     */
    public abstract Object getParameter(String name);

    /** Clears all the parameters set. */
    public abstract void clearParameters();

    /**
     * Who resolves the `href`s of `document()`, `xsl:import` and `xsl:include`.
     *
     * @param resolver the resolver, or null to go back to the default one
     */
    public abstract void setURIResolver(URIResolver resolver);

    /** The resolver in use, or null. */
    public abstract URIResolver getURIResolver();

    /**
     * Sets all the serialization properties at once.
     *
     * <p>Passing {@code null} **resets** the stylesheet's; it does not leave them empty. And the
     * table's default properties (those of {@link Properties#defaults}) are not copied: they are
     * used as a fallback, as in any `Properties`.
     *
     * @param oformat the properties, or null to go back to the stylesheet's
     * @throws IllegalArgumentException if some key is not recognized
     */
    public abstract void setOutputProperties(Properties oformat);

    /**
     * A copy of the output properties in effect.
     *
     * <p>A copy: modifying it changes nothing. To change them one has to call {@link
     * #setOutputProperties} again.
     *
     * @return the properties, with the defaults underneath
     */
    public abstract Properties getOutputProperties();

    /**
     * Sets a single serialization property.
     *
     * <p>The recognized keys are those of {@link OutputKeys} plus extension ones, which go
     * qualified as {@code "{uri}local"}. An unknown **unqualified** key is an error; a qualified
     * one the processor does not understand is ignored, because it may belong to another processor.
     *
     * @param name the key
     * @param value the value
     * @throws IllegalArgumentException if the key is not recognized
     */
    public abstract void setOutputProperty(String name, String value) throws IllegalArgumentException;

    /**
     * The value of an output property.
     *
     * <p>It returns what was set with {@link #setOutputProperty} **or** what the stylesheet
     * declared, not the output method's default value. A property nobody touched gives null even
     * though the serializer has a value for it.
     *
     * @param name the key
     * @return the value, or null
     * @throws IllegalArgumentException if the key is not recognized
     */
    public abstract String getOutputProperty(String name) throws IllegalArgumentException;

    /**
     * Who receives the warnings and errors of the transformation.
     *
     * @param listener the listener; cannot be null
     * @throws IllegalArgumentException if it is null
     */
    public abstract void setErrorListener(ErrorListener listener) throws IllegalArgumentException;

    /** The listener in use; never null. */
    public abstract ErrorListener getErrorListener();
}
