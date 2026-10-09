package javax.xml.stream;

import java.io.IOException;
import java.io.Writer;

import javax.xml.stream.events.ProcessingInstruction;

/** A processing instruction as an event. */
final class EvtPI extends EvtBase implements ProcessingInstruction {

    private final String target;
    private final String data;

    EvtPI(String target, String data, Location location) {
        super(XMLStreamConstants.PROCESSING_INSTRUCTION, location);
        this.target = target;
        this.data = data;
    }

    public String getTarget() {
        return target;
    }

    public String getData() {
        return data;
    }

    public void writeAsEncodedUnicode(Writer writer) throws XMLStreamException {
        if (writer == null) {
            throw new XMLStreamException("the writer cannot be null");
        }
        try {
            writer.write("<?");
            writer.write(target);
            if (data != null && data.length() > 0) {
                writer.write(' ');
                writer.write(data);
            }
            writer.write("?>");
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }
}
