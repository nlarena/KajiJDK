package javax.xml.crypto.dsig.spec;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * KajiLibrary's javax.xml.crypto.dsig.spec.XPathFilterParameterSpec -- the expression of an XPath
 * transform.
 *
 * <p>The XPath transform selects <b>which part</b> of the document is signed. The expression is
 * evaluated on each node and the node goes in if it gives true.
 *
 * <p>That model --node by node-- makes it slow on large documents, and that is why
 * {@link XPathFilter2ParameterSpec} exists, which works by subtrees.
 *
 * <p>The namespace map is needed for the same reason as in {@code javax.xml.xpath}: without
 * declared prefixes, an expression finds nothing in a document with namespaces. Here it weighs more
 * than elsewhere -- an expression that selects nothing produces a signature that covers nothing,
 * and validates all the same.
 */
public final class XPathFilterParameterSpec implements TransformParameterSpec {

    /** The expression. */
    private final String xPath;

    /** Prefix to namespace; never null. */
    private final Map<String, String> nsMap;

    /**
     * Without namespaces.
     *
     * @throws NullPointerException if the expression is null
     */
    public XPathFilterParameterSpec(String xPath) {
        if (xPath == null) {
            throw new NullPointerException("xPath cannot be null");
        }
        this.xPath = xPath;
        this.nsMap = Collections.emptyMap();
    }

    /**
     * With the declared prefixes.
     *
     * <p>The map is copied. See the class note on why it is almost always needed.
     *
     * @throws NullPointerException if either of the two is null
     */
    public XPathFilterParameterSpec(String xPath, Map<String, String> namespaceMap) {
        if (xPath == null || namespaceMap == null) {
            throw new NullPointerException("xPath and namespaceMap cannot be null");
        }
        this.xPath = xPath;
        this.nsMap = Collections.unmodifiableMap(new HashMap<String, String>(namespaceMap));
    }

    /** The expression. */
    public String getXPath() {
        return this.xPath;
    }

    /** The declared prefixes. Unmodifiable. */
    public Map<String, String> getNamespaceMap() {
        return this.nsMap;
    }
}
