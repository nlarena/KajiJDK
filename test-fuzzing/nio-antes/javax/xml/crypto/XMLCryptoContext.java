package javax.xml.crypto;

/**
 * KajiLibrary's javax.xml.crypto.XMLCryptoContext -- the context of an XML cryptographic operation.
 *
 * <p>It gathers everything signing or validating needs to know that is not in the document: with
 * which key ({@link KeySelector}), how to resolve the references ({@link URIDereferencer}), against
 * which base relative things are resolved, and which prefixes to use when writing.
 *
 * <h2>Two maps, not one</h2>
 *
 * <p>{@link #setProperty} and {@link #put} look the same and are not:
 *
 * <ul>
 *   <li>the <b>properties</b> have a {@code String} key and are defined by the implementation: they
 *       are configuration;
 *   <li>the map of {@link #put} has an {@code Object} key and is for whoever uses the API to pass
 *       information of their own from one place to another -- between a custom {@code
 *       URIDereferencer} and a custom {@code KeySelector}, for example.
 * </ul>
 *
 * <p>They are separate so that the implementation's configuration and the application's data do not
 * overwrite each other by choosing the same name.
 *
 * <h2>Namespace prefixes</h2>
 *
 * <p>{@link #putNamespacePrefix} only affects what is <b>written</b>. When reading it does not
 * matter: the document's prefixes are what they are. It serves so that the signature that is
 * generated reads well, and so that it matches another tool's if they need to be compared.
 */
public interface XMLCryptoContext {

    /** What relative things are resolved against. */
    String getBaseURI();

    /** Ver {@link #getBaseURI}. */
    void setBaseURI(String baseURI);

    /** With which key. */
    KeySelector getKeySelector();

    /** Ver {@link #getKeySelector}. */
    void setKeySelector(KeySelector ks);

    /** How the references are resolved. See {@link URIDereferencer}. */
    URIDereferencer getURIDereferencer();

    /** Ver {@link #getURIDereferencer}. */
    void setURIDereferencer(URIDereferencer dereferencer);

    /**
     * The prefix used for that namespace.
     *
     * @param defaultPrefix what to return if none is registered
     */
    String getNamespacePrefix(String namespaceURI, String defaultPrefix);

    /**
     * Registers it.
     *
     * @return the one that was there, or null
     */
    String putNamespacePrefix(String namespaceURI, String prefix);

    /** The default prefix when writing. */
    String getDefaultNamespacePrefix();

    /** Ver {@link #getDefaultNamespacePrefix}. */
    void setDefaultNamespacePrefix(String defaultPrefix);

    /**
     * A property of the implementation.
     *
     * @return the value that was there, or null
     */
    Object setProperty(String name, Object value);

    /** Ver {@link #setProperty}. */
    Object getProperty(String name);

    /** A datum of the application. See the class note on the two maps. */
    Object get(Object key);

    /** Ver {@link #get}. */
    Object put(Object key, Object value);
}
