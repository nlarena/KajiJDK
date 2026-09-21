package javax.xml.stream;

import java.io.Writer;

import javax.xml.stream.events.EndDocument;

/**
 * The end of the document as an event.
 *
 * <p>{@code writeAsEncodedUnicode} writes nothing, and it is not an omission: the end of an XML
 * document has no textual representation. It is the only event for which that is true.
 */
final class EvtEndDocument extends EvtBase implements EndDocument {

    EvtEndDocument(Location location) {
        super(XMLStreamConstants.END_DOCUMENT, location);
    }

    public void writeAsEncodedUnicode(Writer writer) throws XMLStreamException {
        if (writer == null) {
            throw new XMLStreamException("the writer cannot be null");
        }
    }
}
