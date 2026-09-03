package javax.xml.stream;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.events.Namespace;

/**
 * Una declaracion {@code xmlns} como evento.
 *
 * <p>Hereda de {@link EvtAttribute} porque {@link Namespace} hereda de
 * {@link javax.xml.stream.events.Attribute}, y el nombre que se le pasa al padre es el que la
 * especificacion de Namespaces le asigna a una declaracion: <code>{http://www.w3.org/2000/xmlns/}p</code>
 * para {@code xmlns:p}, y el nombre local {@code xmlns} --tambien en ese espacio de nombres-- para
 * la declaracion por omision.
 *
 * <p>Con eso, {@code getName()} y {@code getValue()} heredados dicen lo correcto y
 * {@link #escrito()} vuelve a producir el texto original.
 */
final class EvtNamespace extends EvtAttribute implements Namespace {

    private final String prefix;
    private final String uri;

    EvtNamespace(String prefix, String uri, Location location) {
        super(XMLStreamConstants.NAMESPACE, nameFor(prefix), uri, "CDATA", location);
        this.prefix = prefix;
        this.uri = uri;
    }

    private static QName nameFor(String prefix) {
        if (prefix == null || prefix.length() == 0) {
            return new QName(XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
                    XMLConstants.XMLNS_ATTRIBUTE, XMLConstants.DEFAULT_NS_PREFIX);
        }
        return new QName(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, prefix,
                XMLConstants.XMLNS_ATTRIBUTE);
    }

    public String getPrefix() {
        return prefix;
    }

    public String getNamespaceURI() {
        return uri;
    }

    public boolean isDefaultNamespaceDeclaration() {
        return prefix == null || prefix.length() == 0;
    }
}
