package javax.xml.crypto;

import java.util.Iterator;

/**
 * KajiLibrary's javax.xml.crypto.NodeSetData -- a set of nodes, as data to sign.
 *
 * <p>It is {@code Iterable}, so it goes into an enhanced {@code for}. The type parameter says what
 * the nodes are: the DOM implementation instantiates it with {@code org.w3c.dom.Node}, but nothing
 * forces it to be DOM -- an implementation over another model uses its own.
 *
 * <p>That parameter arrived in Java 9. Before, it was an untyped list and each node had to be cast,
 * which over data coming from a document signed by somebody else is exactly where guessing is
 * unwise.
 */
public interface NodeSetData<T> extends Data, Iterable<T> {

    /** The nodes, in document order. */
    Iterator<T> iterator();
}
