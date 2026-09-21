package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.DocumentFragment -- a lightweight container for moving several nodes at
 * once.
 *
 * <p>It declares no members, but it is not decorative: it has a **behaviour of its own on
 * insertion** that is not written in any signature. When a fragment is passed to {@link
 * Node#appendChild} or to {@link Node#insertBefore}, what is inserted is not the fragment but **its
 * children**, in order, and the fragment is left empty. It is the only way the DOM gives of moving
 * a group of siblings in one single operation, and the reason why it exists: without it, inserting
 * n nodes is n operations, with n notifications to whoever is observing and n chances of leaving
 * the tree in an odd intermediate state.
 *
 * <p>Hence also that it is a {@link Node} without being part of the document: it has no parent, and
 * {@link Node#getNodeName} returns {@code "#document-fragment"}. The note added that it "is not
 * serialised"; it is -- an {@code LSSerializer} writes its children.
 *
 * <p>The interface is declared whole; the JDK does not declare members here either.
 */
public interface DocumentFragment extends Node {
}
