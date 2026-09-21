package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.EntityReference -- a {@code &name;} that was left unexpanded.
 *
 * <p>With no members of its own: what it says is **that the tree kept the reference** instead of
 * having replaced it by its contents. A parser that expands entities produces none of these nodes
 * and a perfectly valid tree may not have a single one; whether they appear or not is a decision of
 * the implementation, and that is why code that walks a DOM has to tolerate both forms.
 *
 * <p>When they appear, their children are a copy of the contents of the {@link Entity} and are
 * **read-only**, together with everything that hangs from there: changing the expansion of one
 * reference and not that of another would leave two copies of the same text saying different
 * things. To change the text the whole reference has to be replaced.
 *
 * <p>The interface is declared whole; the JDK does not declare members here either.
 */
public interface EntityReference extends Node {
}
