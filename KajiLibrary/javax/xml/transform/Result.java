package javax.xml.transform;

/**
 * KajiLibrary's javax.xml.transform.Result -- where an XML document goes.
 *
 * <p>The mirror of {@link Source}, with the same idea: the processor writes without knowing whether
 * on the other side there is a file, a tree or an event handler.
 *
 * <p>The two constants are processing instructions that are **written inside the document** to ask
 * the serializer to stop escaping `&lt;` and `&amp;`. It is an ugly and necessary escape hatch: it
 * serves to emit already built markup, and using it wrongly produces invalid XML without anybody
 * warning.
 */
public interface Result {

    /** Instruction that turns output escaping off. */
    String PI_DISABLE_OUTPUT_ESCAPING = "javax.xml.transform.disable-output-escaping";

    /** Instruction that turns it back on. */
    String PI_ENABLE_OUTPUT_ESCAPING = "javax.xml.transform.enable-output-escaping";

    /** The base URI of the destination. */
    void setSystemId(String systemId);

    String getSystemId();
}
