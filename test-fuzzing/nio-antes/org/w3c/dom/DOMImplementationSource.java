package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.DOMImplementationSource -- the one who knows where there are DOM
 * implementations.
 *
 * <p>One step above {@link DOMImplementation}: the implementation manufactures documents, and this
 * finds implementations. Each provider implements it and the bootstrap registry consults it, which
 * in the JDK is {@code org.w3c.dom.bootstrap.DOMImplementationRegistry}.
 *
 * <p>The {@code features} string has a syntax of its own: module names separated by spaces, each
 * one with an optional version behind --for example {@code "XML 3.0 Traversal +Events 2.0"}-- where
 * the {@code +} asks for the module to be available even if only through {@link
 * DOMImplementation#getFeature} and not directly on the object.
 *
 * <p>The interface is declared whole.
 */
public interface DOMImplementationSource {

    /** Some implementation that meets that, or {@code null} if there is none. */
    public DOMImplementation getDOMImplementation(String features);

    /** All the ones that meet it; the list may come empty. */
    public DOMImplementationList getDOMImplementationList(String features);
}
