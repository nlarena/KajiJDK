package javax.xml.transform.dom;

import javax.xml.transform.SourceLocator;
import org.w3c.dom.Node;

/**
 * KajiLibrary's javax.xml.transform.dom.DOMLocator -- where it failed, when the source was a tree.
 *
 * <p>It extends {@link SourceLocator} adding a single method, and that method exists because what
 * the parent offers --line and column number-- <b>means nothing</b> in a DOM tree. A node in memory
 * has no line: if the document was built by hand it never had one, and if it was read from a file,
 * DOM does not keep it.
 *
 * <p>So an error in a transformation over {@link DOMSource} can only point out the place by
 * pointing at the node itself, and that is {@link #getOriginatingNode}. Whoever catches the error
 * uses it to show the context: the element's name, its attributes, its path up to the root.
 */
public interface DOMLocator extends SourceLocator {

    /** The node where what is being reported happened. */
    Node getOriginatingNode();
}
