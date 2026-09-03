package javax.xml.stream;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import javax.xml.stream.events.DTD;
import javax.xml.stream.events.EntityDeclaration;
import javax.xml.stream.events.NotationDeclaration;

/**
 * Una declaracion de tipo de documento como evento, guardada como texto crudo.
 *
 * <p>{@link #getEntities()}, {@link #getNotations()} y {@link #getProcessedDTD()} devuelven null
 * porque el parser no interpreta el DTD: null es la respuesta que la interfaz reserva justamente
 * para eso, y devolver listas vacias seria peor --diria "lo lei y no habia nada" cuando lo cierto es
 * "no lo lei"--.
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
            throw new XMLStreamException("el escritor no puede ser null");
        }
        try {
            writer.write(text);
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }
}
