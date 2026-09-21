package javax.xml.transform;

/**
 * KajiLibrary's javax.xml.transform.SourceLocator -- where, within a document, something happened.
 *
 * <p>It is the counterpart of `org.xml.sax.Locator` for the world of transformations, and it exists
 * for a very concrete reason: an XSLT error happens in **two** places at once --a line of the
 * stylesheet and a node of the document-- and a message that only says "unexpected element" is no
 * use for fixing anything. This interface is the common minimum for being able to say *where*.
 *
 * <p>The four coordinates are SAX's and with the same conventions, which are worth remembering
 * because they are not obvious:
 *
 * <ul>
 *   <li>the URIs (`publicId`, `systemId`) can be null: not every document came from a named place
 *       --one built in memory has none--;
 *   <li>the positions (`line`, `column`) are counted **from 1**, and **0 means "unknown"**. There
 *       is no line zero, so the value serves as a sentinel without needing a null `Integer`. That
 *       is why {@link TransformerException#getLocationAsString} leaves out of the text the line and
 *       column that are zero: reporting "Line#: 0" would be worse than saying nothing.
 * </ul>
 *
 * <p>And a warning the spec makes and is worth repeating: a `SourceLocator` a processor hands over
 * during the walk is **valid only during the call**. The processor may reuse the same object and
 * move it. Keeping it to look at later gives coordinates from somewhere else; what is kept is a
 * copy of the four values.
 */
public interface SourceLocator {

    /** The public identifier of the document, or null if it has none. */
    String getPublicId();

    /** The URI of the document, or null if it came from none. */
    String getSystemId();

    /** The line, counted from 1; 0 if unknown. */
    int getLineNumber();

    /** The column, counted from 1; 0 if unknown. */
    int getColumnNumber();
}
