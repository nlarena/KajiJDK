package javax.xml.stream;

import java.util.Iterator;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.Comment;
import javax.xml.stream.events.DTD;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.EntityReference;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.ProcessingInstruction;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * El escritor de eventos de esta biblioteca: traduce cada evento a llamadas al escritor de cursor.
 *
 * <p>Podria escribir directo, llamando a {@code writeAsEncodedUnicode} de cada evento, y seria mas
 * corto. No se hace, y el motivo es concreto: el escritor de cursor lleva la cuenta de los espacios
 * de nombres y sabe reparar los que falten, cosa que un evento suelto no puede hacer porque no
 * conoce su entorno. Pasando por el cursor, los dos modelos de escritura producen lo mismo y
 * respetan la misma configuracion.
 *
 * <p>El orden dentro de un {@link StartElement} es el que manda XML: primero las declaraciones,
 * despues los atributos. Si se invirtiera, un atributo podria usar un prefijo que se declara mas
 * adelante en la misma etiqueta --que es legal en XML, pero el escritor de cursor en modo reparador
 * lo veria como no declarado y agregaria una segunda declaracion--.
 */
final class KajiEventWriter implements XMLEventWriter {

    private final XMLStreamWriter w;

    KajiEventWriter(XMLStreamWriter w) {
        this.w = w;
    }

    public void add(XMLEvent event) throws XMLStreamException {
        if (event == null) {
            throw new XMLStreamException("el evento no puede ser null");
        }
        int t = event.getEventType();
        if (t == XMLStreamConstants.START_DOCUMENT) {
            StartDocument d = (StartDocument) event;
            if (d.encodingSet()) {
                w.writeStartDocument(d.getCharacterEncodingScheme(), d.getVersion());
            } else {
                w.writeStartDocument(d.getVersion());
            }
            return;
        }
        if (t == XMLStreamConstants.END_DOCUMENT) {
            w.writeEndDocument();
            return;
        }
        if (t == XMLStreamConstants.START_ELEMENT) {
            StartElement e = event.asStartElement();
            QName q = e.getName();
            w.writeStartElement(prefixOf(q), q.getLocalPart(), q.getNamespaceURI());
            Iterator<Namespace> ns = e.getNamespaces();
            while (ns.hasNext()) {
                writeNs(ns.next());
            }
            Iterator<Attribute> at = e.getAttributes();
            while (at.hasNext()) {
                writeAttributeEvent(at.next());
            }
            return;
        }
        if (t == XMLStreamConstants.END_ELEMENT) {
            EndElement e = event.asEndElement();
            // El nombre no se usa: el escritor de cursor lleva su propia pila y cierra el que
            // corresponde. Pedirle el nombre seria darle la oportunidad de discrepar.
            if (e != null) {
                w.writeEndElement();
            }
            return;
        }
        if (t == XMLStreamConstants.CDATA) {
            w.writeCData(((Characters) event).getData());
            return;
        }
        if (t == XMLStreamConstants.CHARACTERS || t == XMLStreamConstants.SPACE) {
            w.writeCharacters(((Characters) event).getData());
            return;
        }
        if (t == XMLStreamConstants.COMMENT) {
            w.writeComment(((Comment) event).getText());
            return;
        }
        if (t == XMLStreamConstants.PROCESSING_INSTRUCTION) {
            ProcessingInstruction p = (ProcessingInstruction) event;
            w.writeProcessingInstruction(p.getTarget(), p.getData());
            return;
        }
        if (t == XMLStreamConstants.DTD) {
            w.writeDTD(((DTD) event).getDocumentTypeDeclaration());
            return;
        }
        if (t == XMLStreamConstants.ENTITY_REFERENCE) {
            w.writeEntityRef(((EntityReference) event).getName());
            return;
        }
        if (t == XMLStreamConstants.NAMESPACE) {
            writeNs((Namespace) event);
            return;
        }
        if (t == XMLStreamConstants.ATTRIBUTE) {
            writeAttributeEvent((Attribute) event);
            return;
        }
        throw new XMLStreamException("no se sabe escribir el evento de tipo " + t);
    }

    private void writeNs(Namespace ns) throws XMLStreamException {
        if (ns.isDefaultNamespaceDeclaration()) {
            w.writeDefaultNamespace(ns.getNamespaceURI());
        } else {
            w.writeNamespace(ns.getPrefix(), ns.getNamespaceURI());
        }
    }

    private void writeAttributeEvent(Attribute a) throws XMLStreamException {
        QName q = a.getName();
        if (q.getNamespaceURI() == null || q.getNamespaceURI().length() == 0) {
            w.writeAttribute(q.getLocalPart(), a.getValue());
        } else {
            w.writeAttribute(prefixOf(q), q.getNamespaceURI(), q.getLocalPart(), a.getValue());
        }
    }

    private static String prefixOf(QName q) {
        String p = q.getPrefix();
        if (p == null) {
            return XMLConstants.DEFAULT_NS_PREFIX;
        }
        return p;
    }

    public void add(XMLEventReader reader) throws XMLStreamException {
        if (reader == null) {
            throw new XMLStreamException("el lector no puede ser null");
        }
        while (reader.hasNext()) {
            add(reader.nextEvent());
        }
    }

    public String getPrefix(String uri) throws XMLStreamException {
        return w.getPrefix(uri);
    }

    public void setPrefix(String prefix, String uri) throws XMLStreamException {
        w.setPrefix(prefix, uri);
    }

    public void setDefaultNamespace(String uri) throws XMLStreamException {
        w.setDefaultNamespace(uri);
    }

    public void setNamespaceContext(NamespaceContext context) throws XMLStreamException {
        w.setNamespaceContext(context);
    }

    public NamespaceContext getNamespaceContext() {
        return w.getNamespaceContext();
    }

    public void flush() throws XMLStreamException {
        w.flush();
    }

    public void close() throws XMLStreamException {
        w.close();
    }
}
