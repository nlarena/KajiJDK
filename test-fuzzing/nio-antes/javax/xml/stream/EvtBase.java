package javax.xml.stream;

import java.io.StringWriter;

import javax.xml.namespace.QName;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * What all events have in common: the type, the location, the ten {@code isXxx} and the three
 * {@code asXxx}.
 *
 * <p>The {@code isXxx} are answered by looking at {@link #getEventType()} instead of with {@code
 * instanceof}, which is what allows the same Java type --{@link Characters}-- to answer differently
 * depending on where it came from: normal text, CDATA or ignorable space are the same class with
 * three event types.
 *
 * <p>The {@code asXxx} do the cast and let {@link ClassCastException} come out by itself, which is
 * exactly what the interface promises.
 *
 * <p>{@link #toString()} returns the event already written as XML. It is not part of the contract
 * --the interface says nothing about {@code toString}-- but it is what the original does and what
 * one expects when looking at an event in the debugger.
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
