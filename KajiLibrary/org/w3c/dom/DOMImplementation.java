package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.DOMImplementation -- the operations that do not depend on a document.
 *
 * <p>It exists because of a bootstrapping problem: {@link Document} is the factory of all nodes,
 * but then who manufactures the first {@code Document}. This interface, which is obtained from
 * outside the DOM --from a parser, from a {@link DOMImplementationSource}-- and from an already
 * built document through {@link Document#getImplementation}.
 *
 * <p>The other role is answering what the implementation knows how to do, with {@link #hasFeature}
 * and {@link #getFeature}. The module names are those of the standard: {@code "Core"}, {@code
 * "XML"}, {@code "LS"}, {@code "Traversal"}, {@code "Events"}, case-insensitive.
 *
 * <p>The interface is declared whole.
 */
public interface DOMImplementation {

    /**
     * Whether it supports that module in that version.
     *
     * @param version {@code null} or {@code ""} asks about any version
     */
    public boolean hasFeature(String feature, String version);

    /**
     * An empty {@link DocumentType}, with no associated document yet.
     *
     * @throws DOMException {@code INVALID_CHARACTER_ERR}, {@code NAMESPACE_ERR} or
     *     {@code NOT_SUPPORTED_ERR}
     */
    public DocumentType createDocumentType(String qualifiedName, String publicId, String systemId)
            throws DOMException;

    /**
     * A new document with its root element, or with no root element if {@code qualifiedName} is
     * {@code null}.
     *
     * @throws DOMException {@code INVALID_CHARACTER_ERR}, {@code NAMESPACE_ERR},
     *     {@code WRONG_DOCUMENT_ERR} if the {@code doctype} is already used by another document, or
     *     {@code NOT_SUPPORTED_ERR}
     */
    public Document createDocument(String namespaceURI, String qualifiedName, DocumentType doctype)
            throws DOMException;

    /**
     * The object that implements the APIs of that module, or {@code null}.
     *
     * <p>It returns {@code Object} and not something more precise because what comes out of here
     * lives in other packages --{@code org.w3c.dom.ls}, {@code org.w3c.dom.events}-- and the DOM
     * core does not depend on its optional modules.
     */
    public Object getFeature(String feature, String version);
}
