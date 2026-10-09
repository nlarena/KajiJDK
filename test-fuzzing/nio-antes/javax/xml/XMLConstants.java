package javax.xml;

/**
 * KajiLibrary's javax.xml.XMLConstants -- the shared vocabulary of all the XML APIs.
 *
 * <p>Sixteen strings, private constructor, zero state: it is a namespace, not an object. It exists
 * because these same URIs have to be written by SAX, DOM, XSLT, XPath and validation, and a
 * mistyped constant in any of them produces a silent failure -- a namespace that does not match
 * gives no error, it simply matches nothing.
 *
 * <p>The constants are grouped in three families worth telling apart because they are used
 * differently:
 *
 * <ul>
 *   <li>the <b>namespaces reserved</b> by the XML Namespaces spec, which cannot be redeclared
 *       (`xml`, `xmlns`);
 *   <li>the <b>schema language URIs</b>, which serve to ask a factory for a validator of that kind;
 *   <li>the <b>names of security features and properties</b>, which are the only thing here that is
 *       passed to a `setFeature`/`setProperty` instead of being compared.
 * </ul>
 */
public final class XMLConstants {

    /** Nobody instantiates this. */
    private XMLConstants() {
    }

    // ---- reserved by XML Namespaces -------------------------------------------------------------

    /**
     * The namespace of what **has no** namespace: the empty string.
     *
     * <p>That it is the empty string and not null is deliberate and saves a check in every
     * comparison: an unqualified element has a namespace URI, and it is this one.
     */
    public static final String NULL_NS_URI = "";

    /** The default prefix, which is also the empty string: a name without a colon. */
    public static final String DEFAULT_NS_PREFIX = "";

    /** The namespace of the `xml` prefix, fixed by the spec and not declarable. */
    public static final String XML_NS_URI = "http://www.w3.org/XML/1998/namespace";

    /** The `xml` prefix, bound by definition to {@link #XML_NS_URI} and to no other. */
    public static final String XML_NS_PREFIX = "xml";

    /** The namespace of the `xmlns` attributes, which are declarations and not attributes. */
    public static final String XMLNS_ATTRIBUTE_NS_URI = "http://www.w3.org/2000/xmlns/";

    /** The local name of the declaration attribute: `xmlns`. */
    public static final String XMLNS_ATTRIBUTE = "xmlns";

    // ---- schema languages -----------------------------------------------------------------------

    /** W3C XML Schema 1.0. */
    public static final String W3C_XML_SCHEMA_NS_URI = "http://www.w3.org/2001/XMLSchema";

    /** The instance attributes of W3C XML Schema (`xsi:type`, `xsi:nil`...). */
    public static final String W3C_XML_SCHEMA_INSTANCE_NS_URI = "http://www.w3.org/2001/XMLSchema-instance";

    /** The data types of XPath 2.0. */
    public static final String W3C_XPATH_DATATYPE_NS_URI = "http://www.w3.org/2003/11/xpath-datatypes";

    /** The XML DTD, to ask for DTD validation where a schema URI is expected. */
    public static final String XML_DTD_NS_URI = "http://www.w3.org/TR/REC-xml";

    /** RELAX NG 1.0. */
    public static final String RELAXNG_NS_URI = "http://relaxng.org/ns/structure/1.0";

    // ---- security -------------------------------------------------------------------------------

    /**
     * The **secure processing** feature, the only one the whole XML platform supports.
     *
     * <p>Turning it on puts limits on what a hostile document can do to the processor: it bounds
     * entity expansion --the "XML bomb", ten lines that expand to gigabytes-- and the depth of
     * structures. It is not a performance option, it is the difference between parsing someone
     * else's input and not being able to.
     *
     * <p>And it has an asymmetry worth knowing: an implementation that has it on <b>is not obliged
     * to let it be turned off</b>. Secure mode can be imposed by the environment.
     */
    public static final String FEATURE_SECURE_PROCESSING = "http://javax.xml.XMLConstants/feature/secure-processing";

    /**
     * Which protocols are admitted for fetching an external DTD.
     *
     * <p>The value is a comma-separated list of protocols; the empty string forbids everything, and
     * {@code "all"} allows everything. The three `ACCESS_EXTERNAL_*` are the defence against XXE: a
     * document that declares an entity pointing to `file:///etc/passwd` --or to an internal URL--
     * uses the parser as a proxy to read what the attacker cannot reach.
     */
    public static final String ACCESS_EXTERNAL_DTD = "http://javax.xml.XMLConstants/property/accessExternalDTD";

    /** Likewise for external schemas (`xsi:schemaLocation`, `xsd:import`). */
    public static final String ACCESS_EXTERNAL_SCHEMA = "http://javax.xml.XMLConstants/property/accessExternalSchema";

    /** Likewise for external stylesheets (`xsl:import`, `xsl:include`, `document()`). */
    public static final String ACCESS_EXTERNAL_STYLESHEET = "http://javax.xml.XMLConstants/property/accessExternalStylesheet";

    /**
     * Whether the XML catalog is used to resolve external references.
     *
     * <p>A catalog maps public identifiers to local copies, so turning it on is both faster and
     * safer than going out to the network. It is the constructive alternative to the
     * `ACCESS_EXTERNAL_*`: instead of forbidding, redirecting.
     */
    public static final String USE_CATALOG = "http://javax.xml.XMLConstants/feature/useCatalog";
}
