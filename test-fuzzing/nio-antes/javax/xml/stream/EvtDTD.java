package javax.xml.stream;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import javax.xml.stream.events.DTD;
import javax.xml.stream.events.EntityDeclaration;
import javax.xml.stream.events.NotationDeclaration;

/**
 * A document type declaration as an event, kept as raw text.
 *
 * <p>{@link #getEntities()}, {@link #getNotations()} and {@link #getProcessedDTD()} return null
 * because the parser does not interpret the DTD: null is the answer the interface reserves
 * precisely for that, and returning empty lists would be worse --it would say "I read it and there
 * was nothing" when the truth is "I did not read it"--.
 */
final class EvtDTD extends EvtBase implements DTD {

    private final String text;

    EvtDTD(String text, Location location) {
        super(XMLStreamConstants.DTD, location);
        this.text = text;
    }

    public String getDocumentTypeDeclaration() {
        return text;
    }

    public Object getProcessedDTD() {
        return null;
    }

    public List<EntityDeclaration> getEntities() {
        return null;
    }

    public List<NotationDeclaration> getNotations() {
        return null;
    }

    public void writeAsEncodedUnicode(Writer writer) throws XMLStreamException {
        if (writer == null) {
            throw new XMLStreamException("the writer cannot be null");
        }
        try {
            writer.write(text);
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }
}
