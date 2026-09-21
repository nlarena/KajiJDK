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
 * The bridge between the cursor and the events: it takes a snapshot of the reader's current
 * position.
 *
 * <p>It is where the cost of the event model is paid, and it is good that it is visible. Each
 * {@code START_ELEMENT} builds a {@link QName}, an attribute list, a declaration list and a copy of
 * the namespace context. The cursor built none of the four.
 *
 * <p>It has no state, so {@link #newInstance()} could return itself. It returns a new one anyway,
 * because the contract says "a new allocator" and a subclass that does have state would inherit the
 * wrong answer.
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
        throw new XMLStreamException("unexpected event type: " + t, u);
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
