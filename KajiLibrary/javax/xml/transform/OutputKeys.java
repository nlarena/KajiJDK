package javax.xml.transform;

/**
 * KajiLibrary's javax.xml.transform.OutputKeys -- the names of the serialization properties.
 *
 * <p>Ten constants and nothing more: the class is a namespace, not an object. The constructor is
 * private on purpose and there is no instance member.
 *
 * <p>Why a whole class exists for ten strings: {@link Transformer#setOutputProperty} receives the
 * property name as a `String`, because XSLT also allows extension properties with a qualified name
 * (`{uri}local`), and with an `enum` they would not fit. The price of that openness is that a
 * misspelt name is not seen by the compiler -- and `setOutputProperty("ident", "yes")` indents
 * nothing and does not fail either. These constants are the part of the vocabulary that can be
 * checked.
 *
 * <p>The values are **exactly** the attributes of `&lt;xsl:output&gt;` in the XSLT 1.0 spec, §16:
 * the API does not reinvent the vocabulary, it exposes it. That is why they are `method` and not
 * `METHOD`, and why booleans are written `"yes"`/`"no"` and not `"true"`/`"false"`.
 */
public class OutputKeys {

    /** Nobody instantiates this. */
    private OutputKeys() {
    }

    /**
     * What kind of document to emit: {@code "xml"}, {@code "html"}, {@code "text"}, or a qualified
     * name for an extension method.
     *
     * <p>It is the property that changes the result most, and the only one the processor chooses by
     * itself if it is not set: it looks at the output document and uses `html` if the root element
     * is `&lt;html&gt;`.
     */
    public static final String METHOD = "method";

    /** The version of the output method; for {@code xml}, the XML version. */
    public static final String VERSION = "version";

    /** The name of the preferred character set for the output. */
    public static final String ENCODING = "encoding";

    /** {@code "yes"} to not write the XML declaration. */
    public static final String OMIT_XML_DECLARATION = "omit-xml-declaration";

    /** {@code "yes"} or {@code "no"} for the `standalone` attribute of the declaration. */
    public static final String STANDALONE = "standalone";

    /** The public identifier of the DOCTYPE that is emitted. */
    public static final String DOCTYPE_PUBLIC = "doctype-public";

    /** The system identifier of the DOCTYPE that is emitted. */
    public static final String DOCTYPE_SYSTEM = "doctype-system";

    /**
     * The space-separated list of the elements whose text content goes inside a CDATA section
     * instead of escaped.
     */
    public static final String CDATA_SECTION_ELEMENTS = "cdata-section-elements";

    /**
     * {@code "yes"} so that the serializer may add whitespace.
     *
     * <p>Worth clarifying because it is misread: indenting **changes the document**. The added
     * space is text and appears in the tree of whoever reads it. It serves for reading it with the
     * eyes, not for comparing two outputs.
     */
    public static final String INDENT = "indent";

    /** The media type (MIME) of the output document. */
    public static final String MEDIA_TYPE = "media-type";
}
