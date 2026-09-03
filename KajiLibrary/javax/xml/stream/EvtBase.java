package javax.xml.stream;

import java.io.StringWriter;

import javax.xml.namespace.QName;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * Lo comun a todos los eventos: el tipo, la ubicacion, los diez {@code isXxx} y los tres
 * {@code asXxx}.
 *
 * <p>Los {@code isXxx} se contestan mirando {@link #getEventType()} en vez de con
 * {@code instanceof}, que es lo que permite que un mismo tipo Java --{@link Characters}-- conteste
 * distinto segun de donde salio: texto normal, CDATA o espacio ignorable son la misma clase con
 * tres tipos de evento.
 *
 * <p>Los {@code asXxx} hacen el cast y dejan que {@link ClassCastException} salga sola, que es
 * exactamente lo que la interfaz promete.
 *
 * <p>{@link #toString()} devuelve el evento ya escrito como XML. No es parte del contrato --la
 * interfaz no dice nada de {@code toString}-- pero es lo que hace el original y lo que uno espera
 * al mirar un evento en el depurador.
 */
abstract class EvtBase implements XMLEvent {

    private final int type;
    private final Location location;

    EvtBase(int type, Location location) {
        this.type = type;
        if (location == null) {
            this.location = KajiLocation.NONE;
        } else {
            this.location = location;
        }
    }

    public int getEventType() {
        return type;
    }

    public Location getLocation() {
        return location;
    }

    public boolean isStartElement() {
        return type == XMLStreamConstants.START_ELEMENT;
    }

    public boolean isEndElement() {
        return type == XMLStreamConstants.END_ELEMENT;
    }

    public boolean isCharacters() {
        return type == XMLStreamConstants.CHARACTERS || type == XMLStreamConstants.CDATA
                || type == XMLStreamConstants.SPACE;
    }

    public boolean isStartDocument() {
        return type == XMLStreamConstants.START_DOCUMENT;
    }

    public boolean isEndDocument() {
        return type == XMLStreamConstants.END_DOCUMENT;
    }

    public boolean isAttribute() {
        return type == XMLStreamConstants.ATTRIBUTE;
    }

    public boolean isNamespace() {
        return type == XMLStreamConstants.NAMESPACE;
    }

    public boolean isEntityReference() {
        return type == XMLStreamConstants.ENTITY_REFERENCE;
    }

    public boolean isProcessingInstruction() {
        return type == XMLStreamConstants.PROCESSING_INSTRUCTION;
    }

    public StartElement asStartElement() {
        return (StartElement) this;
    }

    public EndElement asEndElement() {
        return (EndElement) this;
    }

    public Characters asCharacters() {
        return (Characters) this;
    }

    public QName getSchemaType() {
        return null;
    }

    public String toString() {
        StringWriter w = new StringWriter();
        try {
            writeAsEncodedUnicode(w);
        } catch (XMLStreamException e) {
            return super.toString();
        }
        return w.toString();
    }
}
