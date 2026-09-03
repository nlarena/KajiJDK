package javax.xml.stream;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;

/**
 * La apertura de un elemento como evento.
 *
 * <p>Copia las dos listas al construirse: el que lo crea suele pasarle iteradores sobre estructuras
 * que va a reusar --el parser reusa sus arreglos en cada elemento-- y el evento tiene que
 * sobrevivir a eso. Es el costo del modelo de eventos, y esta puesto aca a proposito para que sea
 * visible.
 */
final class EvtStartElement extends EvtBase implements StartElement {

    private final QName name;
    private final List<Attribute> attributes;
    private final List<Namespace> namespaces;
    private final NamespaceContext context;

    EvtStartElement(QName name, List<Attribute> attributes, List<Namespace> namespaces,
            NamespaceContext context, Location location) {
        super(XMLStreamConstants.START_ELEMENT, location);
        this.name = name;
        if (attributes == null) {
            this.attributes = new ArrayList<Attribute>(0);
        } else {
            this.attributes = attributes;
        }
        if (namespaces == null) {
            this.namespaces = new ArrayList<Namespace>(0);
        } else {
            this.namespaces = namespaces;
        }
        this.context = context;
    }

    public QName getName() {
        return name;
    }

    public Iterator<Attribute> getAttributes() {
        return attributes.iterator();
    }

    public Iterator<Namespace> getNamespaces() {
        return namespaces.iterator();
    }

    public Attribute getAttributeByName(QName name) {
        if (name == null) {
            return null;
        }
        int n = attributes.size();
        for (int i = 0; i < n; i++) {
            Attribute a = attributes.get(i);
            if (a.getName().equals(name)) {
                return a;
            }
        }
        return null;
    }

    public NamespaceContext getNamespaceContext() {
        return context;
    }

    public String getNamespaceURI(String prefix) {
        if (context == null) {
            return null;
        }
        String u = context.getNamespaceURI(prefix);
        if (u == null || u.length() == 0) {
            return null;
        }
        return u;
    }

    public void writeAsEncodedUnicode(Writer writer) throws XMLStreamException {
        if (writer == null) {
            throw new XMLStreamException("el escritor no puede ser null");
        }
        try {
            writer.write('<');
            writer.write(Names.written(name));
            int m = namespaces.size();
            for (int i = 0; i < m; i++) {
                writer.write(' ');
                Namespace ns = namespaces.get(i);
                if (ns.isDefaultNamespaceDeclaration()) {
                    writer.write(XMLConstants.XMLNS_ATTRIBUTE);
                } else {
                    writer.write(XMLConstants.XMLNS_ATTRIBUTE + ":" + ns.getPrefix());
                }
                writer.write("=\"");
                Escapes.attribute(writer, ns.getNamespaceURI());
                writer.write('"');
            }
            int n = attributes.size();
            for (int i = 0; i < n; i++) {
                writer.write(' ');
                attributes.get(i).writeAsEncodedUnicode(writer);
            }
            writer.write('>');
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }
}
