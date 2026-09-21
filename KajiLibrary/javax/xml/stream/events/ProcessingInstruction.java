package javax.xml.stream.events;

/**
 * KajiLibrary's javax.xml.stream.events.ProcessingInstruction -- a {@code <?target data?>}.
 *
 * <h2>The only place where XML lets through something that is not XML</h2>
 *
 * <p>A processing instruction is a message for a specific application, put into the document
 * without the parser having to understand it. The classic case is {@code <?xml-stylesheet
 * type="text/xsl" href="sheet.xsl"?>}: it says nothing about the data, it tells whoever displays
 * them how to display them.
 *
 * <p>What makes it different from a comment is that it <b>is</b> addressed to someone: {@link
 * #getTarget()} names that someone, and {@link #getData()} returns the rest raw, uninterpreted. XML
 * defines no structure for the data --that they look like attributes in the example above is a
 * convention, not a rule-- so the application parses them however it wants.
 *
 * <p>The {@code <?xml version="1.0"?>} declaration at the start of the document is <b>not</b> a
 * processing instruction, even though it looks like one: it is the prolog's own syntax and arrives
 * as {@link StartDocument}.
 */
public interface ProcessingInstruction extends XMLEvent {

    /**
     * Whom it is addressed to: the name that goes immediately after {@code <?}.
     *
     * @return the target; never null and never empty
     */
    String getTarget();

    /**
     * The rest of the instruction, raw.
     *
     * <p>The space separating the target from the data is discarded, and nothing more: there are no
     * entities to resolve nor escapes to undo inside a processing instruction.
     *
     * @return the data, or null if the instruction only carried the target
     */
    String getData();
}
