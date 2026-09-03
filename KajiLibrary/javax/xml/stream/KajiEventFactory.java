package javax.xml.stream;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.Comment;
import javax.xml.stream.events.DTD;
import javax.xml.stream.events.EndDocument;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.EntityDeclaration;
import javax.xml.stream.events.EntityReference;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.ProcessingInstruction;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.StartElement;

/**
 * La fabrica de eventos de esta biblioteca.
 *
 * <p>No tiene mas estado que la ubicacion pegajosa que fija {@link #setLocation}; ver
 * {@link XMLEventFactory}.
 */
final class KajiEventFactory extends XMLEventFactory {

    private Location location = KajiLocation.NONE;

    KajiEventFactory() {
    }

    public void setLocation(Location location) {
        if (location == null) {
            location = KajiLocation.NONE;
        } else {
            location = location;
        }
    }

    // ---- atributos y espacios de nombres ----------------------------------------------------

    public Attribute createAttribute(String prefix, String namespaceURI, String localName,
            String value) {
        return new EvtAttribute(new QName(namespaceURI, localName, nonNullPrefix(prefix)), value,
                "CDATA", location);
    }

    public Attribute createAttribute(String localName, String value) {
        return new EvtAttribute(new QName(localName), value, "CDATA", location);
    }

    public Attribute createAttribute(QName name, String value) {
        return new EvtAttribute(name, value, "CDATA", location);
    }

    public Namespace createNamespace(String namespaceURI) {
        return new EvtNamespace(XMLConstants.DEFAULT_NS_PREFIX, namespaceURI, location);
    }

    public Namespace createNamespace(String prefix, String namespaceUri) {
        return new EvtNamespace(nonNullPrefix(prefix), namespaceUri, location);
    }

    // ---- elementos --------------------------------------------------------------------------

    public StartElement createStartElement(QName name, Iterator<? extends Attribute> attributes,
            Iterator<? extends Namespace> namespaces) {
        return buildStart(name, attributes, namespaces, null);
    }

    public StartElement createStartElement(String prefix, String namespaceUri, String localName) {
        return buildStart(new QName(namespaceUri, localName, nonNullPrefix(prefix)), null, null, null);
    }

    public StartElement createStartElement(String prefix, String namespaceUri, String localName,
            Iterator<? extends Attribute> attributes, Iterator<? extends Namespace> namespaces) {
        return buildStart(new QName(namespaceUri, localName, nonNullPrefix(prefix)), attributes,
                namespaces, null);
    }

    public StartElement createStartElement(String prefix, String namespaceUri, String localName,
            Iterator<? extends Attribute> attributes, Iterator<? extends Namespace> namespaces,
            NamespaceContext context) {
        return buildStart(new QName(namespaceUri, localName, nonNullPrefix(prefix)), attributes,
                namespaces, context);
    }

    public EndElement createEndElement(QName name, Iterator<? extends Namespace> namespaces) {
        return new EvtEndElement(name, nsList(namespaces), location);
    }

    public EndElement createEndElement(String prefix, String namespaceUri, String localName) {
        return new EvtEndElement(new QName(namespaceUri, localName, nonNullPrefix(prefix)), null,
                location);
    }

    public EndElement createEndElement(String prefix, String namespaceUri, String localName,
            Iterator<? extends Namespace> namespaces) {
        return new EvtEndElement(new QName(namespaceUri, localName, nonNullPrefix(prefix)),
                nsList(namespaces), location);
    }

    // ---- texto ------------------------------------------------------------------------------

    public Characters createCharacters(String content) {
        return new EvtCharacters(XMLStreamConstants.CHARACTERS, content, location);
    }

    public Characters createCData(String content) {
        return new EvtCharacters(XMLStreamConstants.CDATA, content, location);
    }

    public Characters createSpace(String content) {
        return new EvtCharacters(XMLStreamConstants.CHARACTERS, content, location);
    }

    public Characters createIgnorableSpace(String content) {
        return new EvtCharacters(XMLStreamConstants.SPACE, content, location);
    }

    // ---- documento --------------------------------------------------------------------------

    public StartDocument createStartDocument() {
        return new EvtStartDocument(null, "UTF-8", false, "1.0", false, false, location);
    }

    public StartDocument createStartDocument(String encoding, String version, boolean standalone) {
        return new EvtStartDocument(null, encoding, encoding != null, version, standalone, true,
                location);
    }

    public StartDocument createStartDocument(String encoding, String version) {
        return new EvtStartDocument(null, encoding, encoding != null, version, false, false,
                location);
    }

    public StartDocument createStartDocument(String encoding) {
        return new EvtStartDocument(null, encoding, encoding != null, "1.0", false, false,
                location);
    }

    public EndDocument createEndDocument() {
        return new EvtEndDocument(location);
    }

    // ---- los demas --------------------------------------------------------------------------

    public EntityReference createEntityReference(String name, EntityDeclaration declaration) {
        return new EvtEntityRef(name, declaration, location);
    }

    public Comment createComment(String text) {
        return new EvtComment(text, location);
    }

    public ProcessingInstruction createProcessingInstruction(String target, String data) {
        return new EvtPI(target, data, location);
    }

    public DTD createDTD(String dtd) {
        return new EvtDTD(dtd, location);
    }

    // ---- auxiliares -------------------------------------------------------------------------

    private static String nonNullPrefix(String p) {
        if (p == null) {
            return XMLConstants.DEFAULT_NS_PREFIX;
        }
        return p;
    }

    private StartElement buildStart(QName name, Iterator<? extends Attribute> attributes,
            Iterator<? extends Namespace> namespaces, NamespaceContext context) {
        List<Attribute> attrs = new ArrayList<Attribute>();
        if (attributes != null) {
            while (attributes.hasNext()) {
                attrs.add(attributes.next());
            }
        }
        List<Namespace> nss = nsList(namespaces);
        NamespaceContext ctx = context;
        if (ctx == null) {
            // Sin contexto dado, el unico alcance que se conoce es el que declara esta etiqueta.
            KajiNsContext own = new KajiNsContext();
            int n = nss.size();
            for (int i = 0; i < n; i++) {
                Namespace ns = nss.get(i);
                own.declare(ns.getPrefix(), ns.getNamespaceURI());
            }
            ctx = own;
        }
        return new EvtStartElement(name, attrs, nss, ctx, location);
    }

    private static List<Namespace> nsList(Iterator<? extends Namespace> it) {
        List<Namespace> l = new ArrayList<Namespace>();
        if (it != null) {
            while (it.hasNext()) {
                l.add(it.next());
            }
        }
        return l;
    }
}
