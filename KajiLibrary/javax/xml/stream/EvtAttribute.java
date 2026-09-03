package javax.xml.stream;

import java.io.IOException;
import java.io.Writer;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.events.Attribute;

/**
 * Un atributo como evento.
 *
 * <p>{@link #getDTDType()} devuelve siempre {@code "CDATA"} y {@link #isSpecified()} siempre true:
 * el parser de esta biblioteca no lee el DTD, asi que todo lo que entrega estaba escrito en el
 * documento y no tiene tipo declarado. Las dos respuestas son ciertas, no valores de relleno.
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

    /** El nombre tal como se escribe: con prefijo si lo tiene. */
    String written() {
        String p = name.getPrefix();
        if (p == null || p.equals(XMLConstants.DEFAULT_NS_PREFIX)) {
            return name.getLocalPart();
        }
        return p + ":" + name.getLocalPart();
    }

    public void writeAsEncodedUnicode(Writer writer) throws XMLStreamException {
        if (writer == null) {
            throw new XMLStreamException("el escritor no puede ser null");
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
