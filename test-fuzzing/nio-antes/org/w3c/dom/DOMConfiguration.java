package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.DOMConfiguration -- the parameters a document is normalised with.
 *
 * <p>{@link Document#getDomConfig} returns it and {@link Document#normalizeDocument} consumes it.
 * It is a map of named parameters, and it is so --and not an interface with one method per option--
 * because the set of parameters is **open**: the standard defines about twenty
 * ({@code "comments"}, {@code "cdata-sections"}, {@code "entities"}, {@code "namespaces"},
 * {@code "validate"}, {@code "error-handler"}...) and each implementation adds its own with a
 * prefix of its own.
 *
 * <p>The names are case-insensitive. The value is an {@code Object} because almost all of them are
 * booleans but some are not --{@code "error-handler"} wants a {@link DOMErrorHandler},
 * {@code "schema-location"} a string.
 *
 * <p>The part that gets forgotten: {@link #canSetParameter} exists because a parameter may be
 * **supported but not at that value**. An implementation that always validates accepts
 * {@code ("validate", true)} and rejects {@code ("validate", false)}, and without this method the
 * only way of finding out would be provoking the exception.
 *
 * <p>The interface is declared whole.
 */
public interface DOMConfiguration {

    /**
     * It sets a parameter.
     *
     * @throws DOMException {@code NOT_FOUND_ERR} if the parameter is not recognised, or
     *     {@code NOT_SUPPORTED_ERR} if it is recognised but that value is not supported
     */
    public void setParameter(String name, Object value) throws DOMException;

    /**
     * The current value of the parameter.
     *
     * @throws DOMException {@code NOT_FOUND_ERR} if the parameter is not recognised
     */
    public Object getParameter(String name) throws DOMException;

    /** Whether that parameter can be set to that value, without trying. */
    public boolean canSetParameter(String name, Object value);

    /** The names of all the parameters this configuration recognises. */
    public DOMStringList getParameterNames();
}
