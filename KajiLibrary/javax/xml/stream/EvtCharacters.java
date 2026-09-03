package javax.xml.stream;

import java.io.IOException;
import java.io.Writer;

import javax.xml.stream.events.Characters;

/**
 * Texto como evento: los tres sabores.
 *
 * <p>El tipo de evento que se le pase al constructor decide cual es --{@code CHARACTERS},
 * {@code CDATA} o {@code SPACE}-- y de ahi salen las tres preguntas de la interfaz.
 *
 * <p>{@link #isIgnorableWhiteSpace()} contesta true unicamente cuando el evento es {@code SPACE},
 * que en esta biblioteca solo lo produce
 * {@link XMLEventFactory#createIgnorableSpace}: el parser no lo emite nunca, porque sin DTD no hay
 * forma de saber que un espacio es ignorable. Ver {@link Characters}.
 */
final class EvtCharacters extends EvtBase implements Characters {

    private final String data;

    EvtCharacters(int type, String data, Location location) {
        super(type, location);
        this.data = data;
    }

    public String getData() {
        return data;
    }

    public boolean isCData() {
        return getEventType() == XMLStreamConstants.CDATA;
    }

    public boolean isIgnorableWhiteSpace() {
        return getEventType() == XMLStreamConstants.SPACE;
    }

    public boolean isWhiteSpace() {
        int n = data.length();
        for (int i = 0; i < n; i++) {
            char c = data.charAt(i);
            if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
                return false;
            }
        }
        return true;
    }

    public void writeAsEncodedUnicode(Writer writer) throws XMLStreamException {
        if (writer == null) {
            throw new XMLStreamException("el escritor no puede ser null");
        }
        try {
            if (isCData()) {
                writer.write("<![CDATA[");
                writer.write(data);
                writer.write("]]>");
            } else {
                Escapes.content(writer, data);
            }
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }
}
