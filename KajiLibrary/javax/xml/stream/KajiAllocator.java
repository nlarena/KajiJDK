package javax.xml.stream;

import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.XMLEventAllocator;
import javax.xml.stream.util.XMLEventConsumer;

/**
 * El puente entre el cursor y los eventos: fotografia la posicion actual del lector.
 *
 * <p>Es donde se paga el costo del modelo de eventos, y esta bueno que sea visible. Cada
 * {@code START_ELEMENT} construye un {@link QName}, una lista de atributos, una lista de
 * declaraciones y una copia del contexto de espacios de nombres. El cursor no construia ninguna de
 * las cuatro.
 *
 * <p>No tiene estado, asi que {@link #newInstance()} podria devolverse a si mismo. Devuelve uno
 * nuevo igual, porque el contrato dice "un asignador nuevo" y una subclase que si tenga estado
 * heredaria la respuesta equivocada.
 */
final class KajiAllocator implements XMLEventAllocator {

    KajiAllocator() {
    }

    public XMLEventAllocator newInstance() {
        return new KajiAllocator();
    }

    public void allocate(XMLStreamReader reader, XMLEventConsumer consumer)
            throws XMLStreamException {
        consumer.add(allocate(reader));
    }

    public XMLEvent allocate(XMLStreamReader reader) throws XMLStreamException {
        int t = reader.getEventType();
        Location u = reader.getLocation();
        if (t == XMLStreamConstants.START_ELEMENT) {
            List<Namespace> ns = namespacesOf(reader, u);
            List<Attribute> at = new ArrayList<Attribute>();
            int n = reader.getAttributeCount();
            for (int i = 0; i < n; i++) {
                at.add(new EvtAttribute(reader.getAttributeName(i), reader.getAttributeValue(i),
                        reader.getAttributeType(i), u));
            }
            return new EvtStartElement(reader.getName(), at, ns, reader.getNamespaceContext(), u);
        }
        if (t == XMLStreamConstants.END_ELEMENT) {
            return new EvtEndElement(reader.getName(), namespacesOf(reader, u), u);
        }
        if (t == XMLStreamConstants.CHARACTERS || t == XMLStreamConstants.CDATA
                || t == XMLStreamConstants.SPACE) {
            return new EvtCharacters(t, reader.getText(), u);
        }
        if (t == XMLStreamConstants.COMMENT) {
            return new EvtComment(reader.getText(), u);
        }
        if (t == XMLStreamConstants.PROCESSING_INSTRUCTION) {
            return new EvtPI(reader.getPITarget(), reader.getPIData(), u);
        }
        if (t == XMLStreamConstants.DTD) {
            return new EvtDTD(reader.getText(), u);
        }
        if (t == XMLStreamConstants.ENTITY_REFERENCE) {
            return new EvtEntityRef(reader.getLocalName(), null, u);
        }
        if (t == XMLStreamConstants.START_DOCUMENT) {
            String enc = reader.getCharacterEncodingScheme();
            return new EvtStartDocument(u.getSystemId(), enc, enc != null, reader.getVersion(),
                    reader.isStandalone(), reader.standaloneSet(), u);
        }
        if (t == XMLStreamConstants.END_DOCUMENT) {
            return new EvtEndDocument(u);
        }
        throw new XMLStreamException("tipo de evento inesperado: " + t, u);
    }

    private static List<Namespace> namespacesOf(XMLStreamReader reader, Location u) {
        List<Namespace> ns = new ArrayList<Namespace>();
        int n = reader.getNamespaceCount();
        for (int i = 0; i < n; i++) {
            String p = reader.getNamespacePrefix(i);
            if (p == null) {
                p = XMLConstants.DEFAULT_NS_PREFIX;
            }
            ns.add(new EvtNamespace(p, reader.getNamespaceURI(i), u));
        }
        return ns;
    }
}
