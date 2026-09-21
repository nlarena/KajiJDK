package org.w3c.dom.traversal;

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;

/**
 * KajiLibrary's org.w3c.dom.traversal.DocumentTraversal -- the factory of walks.
 *
 * <p>The {@code Document} implements it, and it is the only entry door to the package: there are no
 * public constructors of {@link NodeIterator} nor of {@link TreeWalker}. It has to be so because a
 * walk is <b>tied to its document</b> -- the iterator adjusts when the document changes, and for
 * that the document has to know it exists.
 *
 * <p>A {@code Document} that does not support walks does not implement this interface; one asks
 * with {@code hasFeature("Traversal", "2.0")}.
 */
public interface DocumentTraversal {

    /**
     * A flat walk, in document order.
     *
     * @param root                   where from. It cannot be null
     * @param whatToShow             an OR of the {@code SHOW_*} of {@link NodeFilter}
     * @param filter                 the filter, or null
     * @param entityReferenceExpansion whether entity references are entered
     * @throws DOMException {@code NOT_SUPPORTED_ERR} if the root is null
     */
    NodeIterator createNodeIterator(Node root, int whatToShow, NodeFilter filter,
        boolean entityReferenceExpansion) throws DOMException;

    /**
     * A walk with the shape of a tree. See {@link TreeWalker} for how it differs from the flat one.
     *
     * @throws DOMException {@code NOT_SUPPORTED_ERR} if the root is null
     */
    TreeWalker createTreeWalker(Node root, int whatToShow, NodeFilter filter,
        boolean entityReferenceExpansion) throws DOMException;
}
