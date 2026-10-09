package javax.xml.stream;

import java.io.IOException;
import java.io.Writer;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.events.Attribute;

/**
 * An attribute as an event.
 *
 * <p>{@link #getDTDType()} always returns {@code "CDATA"} and {@link #isSpecified()} always true:
 * this library's parser does not read the DTD, so everything it delivers was written in the
 * document and has no declared type. Both answers are true, not filler values.
 */
class EvtAttribute extends EvtBase implements Attribute {

    private final QName name;
    private final String value;
    private final String type;

    EvtAttribute(QName name, String value, String type, Location location) {
        this(XMLStreamConstants.ATTRIBUTE, name, value, type, location);
    }

    EvtAttribute(int eventType, QName name, String value, String type, Location location) {
        super(eventType, location);
        this.name = name;
        this.value = value;
        if (type == null) {
            this.type = "CDATA";
        } else {
            this.type = type;
        }
    }

    public QName getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public String getDTDType() {
        return type;
    }

    public boolean isSpecified() {
        return true;
    }

    /** The name as it is written: with a prefix if it has one. */
    String written() {
        String p = name.getPrefix();
        if (p == null || p.equals(XMLConstants.DEFAULT_NS_PREFIX)) {
            return name.getLocalPart();
        }
        return p + ":" + name.getLocalPart();
    }

    public void writeAsEncodedUnicode(Writer writer) throws XMLStreamException {
        if (writer == null) {
            throw new XMLStreamException("the writer cannot be null");
        }
        try {
            writer.write(written());
            writer.write("=\"");
            Escapes.attribute(writer, value);
            writer.write('"');
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }
}
