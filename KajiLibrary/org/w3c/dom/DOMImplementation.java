package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.DOMImplementation -- las operaciones que no dependen de un documento.
 *
 * <p>Existe por un problema de arranque: {@link Document} es la fabrica de todos los nodos, pero
 * entonces quien fabrica el primer {@code Document}. Esta interfaz, que se obtiene por fuera del DOM
 * --de un parser, de un {@link DOMImplementationSource}-- y desde un documento ya armado por
 * {@link Document#getImplementation}.
 *
 * <p>El otro papel es responder que sabe hacer la implementacion, con
 * {@link #hasFeature} y {@link #getFeature}. Los nombres de modulo son los de la norma:
 * {@code "Core"}, {@code "XML"}, {@code "LS"}, {@code "Traversal"}, {@code "Events"}, sin distinguir
 * mayusculas.
 *
 * <p>Interfaz declarada entera.
 */
public interface DOMImplementation {

    /**
     * Si soporta ese modulo en esa version.
     *
     * @param version {@code null} o {@code ""} pregunta por cualquier version
     */
    public boolean hasFeature(String feature, String version);

    /**
     * Un {@link DocumentType} vacio, sin documento asociado todavia.
     *
     * @throws DOMException {@code INVALID_CHARACTER_ERR}, {@code NAMESPACE_ERR} o
     *     {@code NOT_SUPPORTED_ERR}
     */
    public DocumentType createDocumentType(String qualifiedName, String publicId, String systemId)
            throws DOMException;

    /**
     * Un documento nuevo con su elemento raiz, o sin elemento raiz si {@code qualifiedName} es
     * {@code null}.
     *
     * @throws DOMException {@code INVALID_CHARACTER_ERR}, {@code NAMESPACE_ERR},
     *     {@code WRONG_DOCUMENT_ERR} si el {@code doctype} ya lo usa otro documento, o
     *     {@code NOT_SUPPORTED_ERR}
     */
    public Document createDocument(String namespaceURI, String qualifiedName, DocumentType doctype)
            throws DOMException;

    /**
     * El objeto que implementa las APIs de ese modulo, o {@code null}.
     *
     * <p>Devuelve {@code Object} y no algo mas preciso porque lo que sale de aca vive en otros
     * paquetes --{@code org.w3c.dom.ls}, {@code org.w3c.dom.events}-- y el nucleo del DOM no
     * depende de sus modulos opcionales.
     */
    public Object getFeature(String feature, String version);
}
