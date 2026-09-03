package javax.xml.stream;

import java.io.Writer;

import javax.xml.stream.events.EndDocument;

/**
 * El final del documento como evento.
 *
 * <p>{@code writeAsEncodedUnicode} no escribe nada, y no es una omision: el final de un documento
 * XML no tiene representacion textual. Es el unico evento del que eso es cierto.
 */
final class EvtEndDocument extends EvtBase implements EndDocument {

    EvtEndDocument(Location location) {
        super(XMLStreamConstants.END_DOCUMENT, location);
    }

    public void writeAsEncodedUnicode(Writer writer) throws XMLStreamException {
        if (writer == null) {
            throw new XMLStreamException("el escritor no puede ser null");
        }
    }
}
