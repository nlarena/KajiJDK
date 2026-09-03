package javax.xml.stream;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.Namespace;

/** El cierre de un elemento como evento. */
final class EvtEndElement extends EvtBase implements EndElement {

    private final QName name;
    private final List<Namespace> namespaces;

    EvtEndElement(QName name, List<Namespace> namespaces, Location location) {
        super(XMLStreamConstants.END_ELEMENT, location);
        this.name = name;
        if (namespaces == null) {
            this.namespaces = new ArrayList<Namespace>(0);
        } else {
            this.namespaces = namespaces;
        }
    }

    public QName getName() {
        return name;
    }

    public Iterator<Namespace> getNamespaces() {
        return namespaces.iterator();
    }

    public void writeAsEncodedUnicode(Writer writer) throws XMLStreamException {
        if (writer == null) {
            throw new XMLStreamException("el escritor no puede ser null");
        }
        try {
            writer.write("</");
            writer.write(Names.written(name));
            writer.write('>');
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }
}
