package javax.xml.stream;

import java.io.IOException;
import java.io.Writer;

import javax.xml.stream.events.Characters;

/**
 * Text as an event: the three flavours.
 *
 * <p>The event type passed to the constructor decides which one it is --{@code CHARACTERS},
 * {@code CDATA} or {@code SPACE}-- and the three questions of the interface follow from that.
 *
 * <p>{@link #isIgnorableWhiteSpace()} answers true only when the event is {@code SPACE}, which in
 * this library only {@link XMLEventFactory#createIgnorableSpace} produces: the parser never emits
 * it, because without a DTD there is no way of knowing that a space is ignorable. See {@link
 * Characters}.
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
            throw new XMLStreamException("the writer cannot be null");
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
