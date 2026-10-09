package javax.xml.xpath;

import java.util.Iterator;
import org.w3c.dom.Node;

/**
 * KajiLibrary's javax.xml.xpath.XPathNodes -- a node-set you can walk with {@code for}.
 *
 * <p>It exists to replace {@code org.w3c.dom.NodeList} as a result, and the difference is all about
 * convenience: {@code NodeList} is from 1998, is not {@code Iterable}, and walking it takes an
 * indexed loop and a cast per element. This one is {@code Iterable<Node>}, so it goes into an
 * enhanced {@code for} without ceremony.
 *
 * <p>{@link #get} throws instead of returning null when out of range, unlike {@code NodeList#item}.
 * It is the modern criterion and the right one: an out-of-range index is a programmer error, and a
 * null propagates until it blows up somewhere else.
 */
public interface XPathNodes extends Iterable<Node> {

    /** The nodes, in document order. */
    Iterator<Node> iterator();

    /** How many there are. */
    int size();

    /**
     * The one at that position.
     *
     * @throws XPathException if the index is out of range; see the class note
     */
    Node get(int index) throws XPathException;
}
