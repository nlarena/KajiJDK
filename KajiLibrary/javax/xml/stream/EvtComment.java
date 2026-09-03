package javax.xml.stream;

import java.io.IOException;
import java.io.Writer;

import javax.xml.stream.events.Comment;

/** Un comentario como evento. */
final class EvtComment extends EvtBase implements Comment {

    private final String text;

    EvtComment(String text, Location location) {
        super(XMLStreamConstants.COMMENT, location);
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void writeAsEncodedUnicode(Writer writer) throws XMLStreamException {
        if (writer == null) {
            throw new XMLStreamException("el escritor no puede ser null");
        }
        try {
            writer.write("<!--");
            writer.write(text);
            writer.write("-->");
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }
}
