package javax.xml.stream;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

/**
 * The namespace scope, as a stack of prefix-URI pairs.
 *
 * <p>Two parallel arrays and a stack of marks: opening an element pushes a mark, closing it
 * discards everything declared after it. Looking up a prefix is walking backwards, so the innermost
 * declaration wins without anything having to be copied when entering each element --which is the
 * cost of the version with one map per level--.
 *
 * <p>Walking backwards is linear in the number of live declarations. In a real document that is a
 * handful; a map would be faster in the pathological case and slower in all the others, besides
 * needing the shadowing to be undone when popping.
 *
 * <p>The two prefixes the specification fixes --{@code xml} and {@code xmlns}-- are answered
 * separately and cannot be overridden, which is what {@link NamespaceContext} asks for.
 *
 * <p>{@link #snapshot()} returns an immutable copy, which is what a {@link
 * javax.xml.stream.events.StartElement} needs: the event survives the parser, so it cannot keep
 * looking at a stack that is going to keep changing.
 */
class KajiNsContext implements NamespaceContext {

    /** Declared prefixes, from oldest to newest. */
    String[] prefixes = new String[8];

    /** URIs, parallel to {@link #prefixes}. */
    String[] uris = new String[8];

    /** How many live declarations there are. */
    int n;

    /** Where each open level starts. */
    int[] marks = new int[8];

    /** How many levels are open. */
    int levels;

    KajiNsContext() {
    }

    /** Opens a level: whatever is declared from here on dies with it. */
    void openScope() {
        if (levels == marks.length) {
            int[] bigger = new int[marks.length * 2];
            System.arraycopy(marks, 0, bigger, 0, marks.length);
            marks = bigger;
        }
        marks[levels] = n;
        levels++;
    }

    /** Closes the top level and discards its declarations. */
    void closeScope() {
        if (levels > 0) {
            levels--;
            n = marks[levels];
        }
    }

    /** How many declarations the top level made. */
    int declaredInScope() {
        if (levels == 0) {
            return n;
        }
        return n - marks[levels - 1];
    }

    /** The i-th declaration of the top level. */
    int indexInScope(int i) {
        if (levels == 0) {
            return i;
        }
        return marks[levels - 1] + i;
    }

    /** Declares a prefix in the top level. */
    void declare(String prefix, String uri) {
        if (n == prefixes.length) {
            String[] p = new String[n * 2];
            String[] u = new String[n * 2];
            System.arraycopy(prefixes, 0, p, 0, n);
            System.arraycopy(uris, 0, u, 0, n);
            prefixes = p;
            uris = u;
        }
        prefixes[n] = prefix;
        uris[n] = uri;
        n++;
    }

    public String getNamespaceURI(String prefix) {
        if (prefix == null) {
            throw new IllegalArgumentException("the prefix cannot be null");
        }
        if (prefix.equals(XMLConstants.XML_NS_PREFIX)) {
            return XMLConstants.XML_NS_URI;
        }
        if (prefix.equals(XMLConstants.XMLNS_ATTRIBUTE)) {
            return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
        }
        for (int i = n - 1; i >= 0; i--) {
            if (prefixes[i].equals(prefix)) {
                return uris[i];
            }
        }
        return XMLConstants.NULL_NS_URI;
    }

    public String getPrefix(String namespaceURI) {
        if (namespaceURI == null) {
            throw new IllegalArgumentException("the namespace cannot be null");
        }
        if (namespaceURI.equals(XMLConstants.XML_NS_URI)) {
            return XMLConstants.XML_NS_PREFIX;
        }
        if (namespaceURI.equals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI)) {
            return XMLConstants.XMLNS_ATTRIBUTE;
        }
        for (int i = n - 1; i >= 0; i--) {
            if (uris[i].equals(namespaceURI) && getNamespaceURI(prefixes[i]).equals(namespaceURI)) {
                return prefixes[i];
            }
        }
        return null;
    }

    public Iterator<String> getPrefixes(String namespaceURI) {
        if (namespaceURI == null) {
            throw new IllegalArgumentException("the namespace cannot be null");
        }
        List<String> r = new ArrayList<String>();
        if (namespaceURI.equals(XMLConstants.XML_NS_URI)) {
            r.add(XMLConstants.XML_NS_PREFIX);
            return r.iterator();
        }
        if (namespaceURI.equals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI)) {
            r.add(XMLConstants.XMLNS_ATTRIBUTE);
            return r.iterator();
        }
        for (int i = n - 1; i >= 0; i--) {
            if (uris[i].equals(namespaceURI)
                    && getNamespaceURI(prefixes[i]).equals(namespaceURI)
                    && !r.contains(prefixes[i])) {
                r.add(prefixes[i]);
            }
        }
        return r.iterator();
    }

    /** A frozen copy, to hang on an event that is going to outlive the parser. */
    KajiNsContext snapshot() {
        KajiNsContext c = new KajiNsContext();
        c.prefixes = new String[n < 1 ? 1 : n];
        c.uris = new String[n < 1 ? 1 : n];
        System.arraycopy(prefixes, 0, c.prefixes, 0, n);
        System.arraycopy(uris, 0, c.uris, 0, n);
        c.n = n;
        return c;
    }
}
