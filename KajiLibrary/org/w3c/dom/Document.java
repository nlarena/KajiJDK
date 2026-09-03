package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.Document -- la raiz del arbol y la fabrica de todo lo que va adentro.
 *
 * <p>Tiene dos papeles distintos que conviene no mezclar. Como **nodo** es la raiz: su unico hijo
 * elemento es el elemento raiz del XML, al que se llega por {@link #getDocumentElement}, y ojo con
 * la diferencia --el documento y el elemento raiz no son el mismo nodo, y al lado del elemento raiz
 * pueden colgar comentarios, instrucciones de procesamiento y el {@link DocumentType}. Como
 * **fabrica** es el unico lugar de donde salen nodos nuevos: no hay constructores en el DOM, todo
 * sale de un {@code createXxx}.
 *
 * <p>Que la fabrica sea el documento y no una clase suelta es lo que sostiene la regla de la que
 * cuelga la mitad de los errores del DOM: cada nodo pertenece **al documento que lo creo**, y meter
 * en un arbol un nodo fabricado por otro documento es {@code WRONG_DOCUMENT_ERR}. Para cruzarlo hay
 * dos caminos y son distintos: {@link #importNode} deja el original donde estaba y trae una copia,
 * {@link #adoptNode} se lo lleva del otro arbol.
 *
 * <p>Interfaz declarada entera. Los {@code createXxx} estan declarados como corresponde a un
 * contrato: aca no hay implementacion, y declarar la firma no promete un arbol.
 */
public interface Document extends Node {

    /** El {@code <!DOCTYPE>}, o {@code null} si no hay. */
    public DocumentType getDoctype();

    /** El objeto que maneja las preguntas sobre que soporta esta implementacion. */
    public DOMImplementation getImplementation();

    /** El elemento raiz. No es el documento: es su hijo. */
    public Element getDocumentElement();

    /**
     * Un elemento nuevo, sin espacio de nombres.
     *
     * @throws DOMException {@code INVALID_CHARACTER_ERR} si el nombre es malformado
     */
    public Element createElement(String tagName) throws DOMException;

    /** Un fragmento vacio, para juntar nodos antes de insertarlos de una. */
    public DocumentFragment createDocumentFragment();

    /** Un nodo de texto con ese contenido. */
    public Text createTextNode(String data);

    /** Un comentario con ese contenido. */
    public Comment createComment(String data);

    /**
     * Una seccion CDATA con ese contenido.
     *
     * @throws DOMException {@code NOT_SUPPORTED_ERR} en un documento HTML
     */
    public CDATASection createCDATASection(String data) throws DOMException;

    /**
     * Una instruccion de procesamiento.
     *
     * @throws DOMException {@code INVALID_CHARACTER_ERR} o {@code NOT_SUPPORTED_ERR}
     */
    public ProcessingInstruction createProcessingInstruction(String target, String data)
            throws DOMException;

    /**
     * Un atributo suelto, sin elemento. Para pegarlo hay que usar {@link Element#setAttributeNode}.
     *
     * @throws DOMException {@code INVALID_CHARACTER_ERR}
     */
    public Attr createAttribute(String name) throws DOMException;

    /**
     * Una referencia a entidad sin expandir.
     *
     * @throws DOMException {@code INVALID_CHARACTER_ERR} o {@code NOT_SUPPORTED_ERR}
     */
    public EntityReference createEntityReference(String name) throws DOMException;

    /** Los elementos del documento con esa etiqueta, en orden. La lista esta viva. */
    public NodeList getElementsByTagName(String tagname);

    /**
     * Una copia del nodo, creada por **este** documento, lista para insertar; el original no se
     * toca. Con {@code deep} en {@code false} viene sin hijos.
     *
     * @throws DOMException {@code NOT_SUPPORTED_ERR} si el tipo de nodo no se puede importar
     */
    public Node importNode(Node importedNode, boolean deep) throws DOMException;

    /**
     * Un elemento nuevo con espacio de nombres.
     *
     * @throws DOMException {@code INVALID_CHARACTER_ERR}, {@code NAMESPACE_ERR} o
     *     {@code NOT_SUPPORTED_ERR}
     */
    public Element createElementNS(String namespaceURI, String qualifiedName) throws DOMException;

    /**
     * Un atributo nuevo con espacio de nombres.
     *
     * @throws DOMException {@code INVALID_CHARACTER_ERR}, {@code NAMESPACE_ERR} o
     *     {@code NOT_SUPPORTED_ERR}
     */
    public Attr createAttributeNS(String namespaceURI, String qualifiedName) throws DOMException;

    /** Los elementos con ese espacio de nombres y nombre local, en orden. */
    public NodeList getElementsByTagNameNS(String namespaceURI, String localName);

    /**
     * El elemento cuyo atributo de tipo ID vale asi, o {@code null}.
     *
     * <p>Depende de que **algo** haya declarado ese atributo como ID: el DTD, un esquema, o
     * {@link Element#setIdAttribute}. Sin eso no encuentra nada, aunque el atributo se llame
     * {@code "id"}.
     */
    public Element getElementById(String elementId);

    /** La codificacion detectada al parsear, o {@code null} si el documento no vino de un parser. */
    public String getInputEncoding();

    /** La codificacion declarada en la declaracion XML, o {@code null}. */
    public String getXmlEncoding();

    /** Lo que decia --o dice-- {@code standalone} en la declaracion XML. */
    public boolean getXmlStandalone();

    /**
     * Fija {@code standalone}.
     *
     * @throws DOMException {@code NOT_SUPPORTED_ERR} en un documento que no soporta XML
     */
    public void setXmlStandalone(boolean xmlStandalone) throws DOMException;

    /** La version XML, tipicamente {@code "1.0"} o {@code "1.1"}. */
    public String getXmlVersion();

    /**
     * Cambia la version XML.
     *
     * @throws DOMException {@code NOT_SUPPORTED_ERR} si la version no se soporta
     */
    public void setXmlVersion(String xmlVersion) throws DOMException;

    /**
     * Si se chequean errores en cada operacion.
     *
     * <p>Apagarlo es una valvula de escape para armar arboles grandes rapido, a cambio de que el
     * documento pueda quedar invalido sin que nadie avise.
     */
    public boolean getStrictErrorChecking();

    /** Prende o apaga el chequeo de errores. */
    public void setStrictErrorChecking(boolean strictErrorChecking);

    /** La URI del documento, o {@code null} si no se sabe. */
    public String getDocumentURI();

    /** Fija la URI del documento. */
    public void setDocumentURI(String documentURI);

    /**
     * Se lleva el nodo de su documento anterior a este, **moviendolo**: el original queda sin ese
     * subarbol.
     *
     * @throws DOMException {@code NOT_SUPPORTED_ERR} o {@code NO_MODIFICATION_ALLOWED_ERR}
     */
    public Node adoptNode(Node source) throws DOMException;

    /** La configuracion que usa {@link #normalizeDocument}. */
    public DOMConfiguration getDomConfig();

    /**
     * Deja el documento como si se lo hubiera serializado y vuelto a parsear, aplicando lo que diga
     * {@link #getDomConfig}: junta textos, resuelve espacios de nombres, y valida si se le pidio.
     */
    public void normalizeDocument();

    /**
     * Cambia el nombre --y el espacio de nombres-- de un {@link Element} o un {@link Attr},
     * devolviendo el nodo renombrado, que puede ser otro objeto.
     *
     * @throws DOMException {@code NOT_SUPPORTED_ERR}, {@code INVALID_CHARACTER_ERR},
     *     {@code WRONG_DOCUMENT_ERR} o {@code NAMESPACE_ERR}
     */
    public Node renameNode(Node n, String namespaceURI, String qualifiedName) throws DOMException;
}
