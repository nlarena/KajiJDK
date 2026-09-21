package javax.xml.stream;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.events.Namespace;

/**
 * An {@code xmlns} declaration as an event.
 *
 * <p>It inherits from {@link EvtAttribute} because {@link Namespace} inherits from {@link
 * javax.xml.stream.events.Attribute}, and the name passed to the parent is the one the Namespaces
 * specification assigns to a declaration: <code>{http://www.w3.org/2000/xmlns/}p</code> for {@code
 * xmlns:p}, and the local name {@code xmlns} --also in that namespace-- for the default
 * declaration.
 *
 * <p>With that, the inherited {@code getName()} and {@code getValue()} say the right thing and
 * {@link #written()} produces the original text again.
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
