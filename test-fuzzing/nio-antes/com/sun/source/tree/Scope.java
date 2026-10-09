package com.sun.source.tree;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

/**
 * The lexical scope at a point of the program, in order to ask what names are visible.
 *
 * <p>Like {@link LineMap}, it is not a node: it is a *resolved* view of the tree, and that is why
 * it returns {@code Element} of the element model and not nodes of the tree. The chain of
 * {@link #getEnclosingScope} is what does the lookup of a name.
 */
public interface Scope {

    Scope getEnclosingScope();

    TypeElement getEnclosingClass();

    ExecutableElement getEnclosingMethod();

    Iterable<? extends Element> getLocalElements();
}
