package javax.xml.crypto.dom;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.URIDereferencer;
import javax.xml.crypto.XMLCryptoContext;
import org.w3c.dom.Element;

/**
 * KajiLibrary's javax.xml.crypto.dom.DOMCryptoContext -- the context of an XML signature operation
 * over DOM.
 *
 * <p>The common implementation of {@link XMLCryptoContext} for the DOM mechanism.
 * {@code DOMSignContext} and {@code DOMValidateContext} inherit from here everything they share.
 *
 * <h2>Three different maps, and they get confused</h2>
 *
 * <p>It is the only complicated thing about the class. It keeps three separate things:
 *
 * <ul>
 *   <li>the <b>namespace prefixes</b> ({@link #putNamespacePrefix}): with which prefix to write
 *       each namespace when generating the signature;
 *   <li>the <b>properties</b> ({@link #setProperty}): the implementation's configuration;
 *   <li>the map of <b>context objects</b> ({@link #put}): data the parts of the operation pass each
 *       other while it runs.
 * </ul>
 *
 * <p>Each has its pair of methods and they <b>do not cross</b>: what is kept with {@code
 * setProperty} does not come out through {@code get}. Checked against JDK 25.
 *
 * <h2>Identifiers have to be declared</h2>
 *
 * <p>{@link #getElementById} does not search the document: it searches a registry filled by hand
 * with {@link #setIdAttributeNS}.
 *
 * <p>It sounds awkward and it is a security decision. A DOM without a schema does not know which
 * attributes are identifiers, and guessing --taking any attribute called {@code Id}-- lets a
 * hostile document declare a false identifier and make the signature validate against other
 * content. Registering them explicitly is what closes that door.
 *
 * <p>{@link #iterator} walks that registry, and it is read-only: trying to remove an entry with the
 * iterator throws {@link UnsupportedOperationException}.
 */
public class DOMCryptoContext implements XMLCryptoContext {

    /** Namespace to prefix. */
    private final HashMap<String, String> nsMap = new HashMap<String, String>();

    /** Identifier to element. See the class note. */
    private final HashMap<String, Element> idMap = new HashMap<String, Element>();

    /** The objects the parts of the operation pass each other. */
    private final HashMap<Object, Object> objMap = new HashMap<Object, Object>();

    /** The base relative URIs are resolved against. */
    private String baseURI;

    /** What the keys are chosen with. */
    private KeySelector ks;

    /** How the references are resolved. */
    private URIDereferencer dereferencer;

    /** The implementation's configuration. */
    private final HashMap<String, Object> propMap = new HashMap<String, Object>();

    /** The prefix for the default namespace. */
    private String defaultPrefix;

    /** For the subclasses; it is not instantiated directly. */
    protected DOMCryptoContext() {
    }

    /**
     * With which prefix to write that namespace.
     *
     * @param defaultPrefix what to return if none is registered
     * @throws NullPointerException if the namespace is null
     */
    public String getNamespacePrefix(String namespaceURI, String defaultPrefix) {
        if (namespaceURI == null) {
            throw new NullPointerException("namespaceURI cannot be null");
        }
        String prefix = this.nsMap.get(namespaceURI);
        if (prefix != null) {
            return prefix;
        }
        return defaultPrefix;
    }

    /**
     * Registers a prefix.
     *
     * @return the one that was there before, or null
     * @throws NullPointerException if the namespace is null
     */
    public String putNamespacePrefix(String namespaceURI, String prefix) {
        if (namespaceURI == null) {
            throw new NullPointerException("namespaceURI is null");
        }
        return this.nsMap.put(namespaceURI, prefix);
    }

    /** The prefix for the default namespace, or null. */
    public String getDefaultNamespacePrefix() {
        return this.defaultPrefix;
    }

    /** Sets it; null goes back to the default behaviour. */
    public void setDefaultNamespacePrefix(String defaultPrefix) {
        this.defaultPrefix = defaultPrefix;
    }

    /** What relative URIs are resolved against, or null. */
    public String getBaseURI() {
        return this.baseURI;
    }

    /**
     * Sets it.
     *
     * <p>It is validated when set and not when used: a malformed URI is discovered at the place
     * that wrote it, not inside a signature.
     *
     * @throws IllegalArgumentException if it is not a valid URI
     */
    public void setBaseURI(String baseURI) {
        if (baseURI != null) {
            try {
                new URI(baseURI);
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
        this.baseURI = baseURI;
    }

    /** How the references are resolved, or null for the implementation's. */
    public URIDereferencer getURIDereferencer() {
        return this.dereferencer;
    }

    /** Changes it; null goes back to the implementation's. */
    public void setURIDereferencer(URIDereferencer dereferencer) {
        this.dereferencer = dereferencer;
    }

    /**
     * A property of the implementation. See the class note: it is not the map of {@link #get}.
     *
     * @throws NullPointerException if the name is null
     */
    public Object getProperty(String name) {
        if (name == null) {
            throw new NullPointerException("name is null");
        }
        return this.propMap.get(name);
    }

    /**
     * Sets it.
     *
     * @return the one that was there before, or null
     * @throws NullPointerException if the name is null
     */
    public Object setProperty(String name, Object value) {
        if (name == null) {
            throw new NullPointerException("name is null");
        }
        return this.propMap.put(name, value);
    }

    /** What the keys are chosen with, or null. */
    public KeySelector getKeySelector() {
        return this.ks;
    }

    /** Changes it. */
    public void setKeySelector(KeySelector ks) {
        this.ks = ks;
    }

    /**
     * The element with that identifier, or null.
     *
     * <p>It only finds the ones registered with {@link #setIdAttributeNS}; see the class note.
     *
     * @throws NullPointerException if the identifier is null
     */
    public Element getElementById(String idValue) {
        if (idValue == null) {
            throw new NullPointerException("idValue is null");
        }
        return this.idMap.get(idValue);
    }

    /**
     * Declares that attribute of that element to be its identifier.
     *
     * <p>See the class note on why it has to be declared.
     *
     * @param namespaceURI the attribute's, or null if it has none
     * @param localName the local name of the attribute
     * @throws NullPointerException if the element or the local name is null
     * @throws IllegalArgumentException if the element does not have that attribute
     */
    public void setIdAttributeNS(Element element, String namespaceURI, String localName) {
        if (element == null) {
            throw new NullPointerException("element is null");
        }
        if (localName == null) {
            throw new NullPointerException("localName is null");
        }
        String idValue = element.getAttributeNS(namespaceURI, localName);
        if (idValue == null || idValue.length() == 0) {
            throw new IllegalArgumentException(localName + " is not an attribute");
        }
        this.idMap.put(idValue, element);
    }

    /**
     * Walks the registered identifiers.
     *
     * <p>Read-only: the iterator's {@code remove} throws {@link UnsupportedOperationException}.
     */
    public Iterator<Map.Entry<String, Element>> iterator() {
        return java.util.Collections.unmodifiableMap(this.idMap).entrySet().iterator();
    }

    /**
     * An object of the context map. See the class note: it is not the one of the properties.
     *
     * <p>It admits a null key, unlike the other two.
     */
    public Object get(Object key) {
        return this.objMap.get(key);
    }

    /**
     * Keeps it.
     *
     * @return the one that was there before, or null
     */
    public Object put(Object key, Object value) {
        return this.objMap.put(key, value);
    }
}
