package javax.xml.stream;

/**
 * KajiLibrary's javax.xml.stream.Location -- where, in the input text, what just happened happened.
 *
 * <p>It exists for one reason only: an XML error without a line number is useless. Every event and
 * every {@link XMLStreamException} can carry a location, and it is what turns a "badly closed
 * element" into something that can be fixed.
 *
 * <p>The three numeric coordinates overlap on purpose and serve different things: line and column
 * are for a person to read, and the character offset is for an editor to position the cursor
 * without counting lines again. All three return -1 when the implementation does not keep them; the
 * two identifiers return null.
 *
 * <p>An implementation not keeping count is legitimate and common: maintaining line and column
 * costs in the parser's hottest loop, and there are uses --reading a document already known to be
 * correct-- that do not pay for it.
 */
public interface Location {

    /**
     * The line, counting from 1, or -1 if not kept.
     *
     * @return the line number
     */
    int getLineNumber();

    /**
     * The column, counting from 1, or -1 if not kept.
     *
     * @return the column number
     */
    int getColumnNumber();

    /**
     * How many characters have been read since the start of the input, or -1.
     *
     * @return the character offset
     */
    int getCharacterOffset();

    /**
     * The public identifier of the entity this came from, or null.
     *
     * @return the public identifier
     */
    String getPublicId();

    /**
     * The system identifier --typically the file's URI-- or null.
     *
     * @return the system identifier
     */
    String getSystemId();
}
