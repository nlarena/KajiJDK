package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.Element -- una etiqueta del documento.
 *
 * <p>Es el unico tipo de nodo con atributos, y de lejos el que mas metodos propios agrega. Casi
 * todos vienen de a pares --{@code getAttribute} / {@code getAttributeNS}-- porque el DOM Level 1
 * se escribio antes de que existieran los espacios de nombres y el Level 2 no pudo cambiar las
 * firmas viejas sin romper todo. Las dos familias conviven sobre el **mismo** conjunto de
 * atributos y verlas como dos colecciones distintas es el error clasico.
 *
 * <p>La diferencia real: la version sin {@code NS} indexa por el nombre completo tal cual esta
 * escrito, prefijo incluido, y la version {@code NS} por el par (URI, nombre local). Para un
 * documento sin namespaces son lo mismo; para uno con namespaces, {@code getAttribute("x:id")}
 * encuentra lo que {@code getAttributeNS(uri, "id")} tambien encuentra, pero
 * {@code getAttribute("id")} no encuentra nada.
 *
 * <p>Otro par que conviene distinguir: {@link #getAttribute} devuelve el **valor** --y devuelve
 * {@code ""} tanto si el atributo vale vacio como si no existe, de ahi que exista
 * {@link #hasAttribute}-- mientras que {@link #getAttributeNode} devuelve el **nodo**, o
 * {@code null}.
 *
 * <p>Los tres {@code setIdAttribute*} son de DOM Level 3 y hacen algo que suena raro: marcan un
 * atributo como de tipo ID **despues** de haber armado el arbol, para que
 * {@link Document#getElementById} lo encuentre sin que haya habido DTD ni esquema.
 *
 * <p>Interfaz declarada entera.
 */
public interface Element extends Node {

    /** El nombre de la etiqueta, igual que {@link Node#getNodeName}. */
    public String getTagName();

    /** El valor del atributo, o {@code ""} si no existe --que es indistinguible de un valor vacio. */
    public String getAttribute(String name);

    /**
     * Pone o reemplaza un atributo. El valor se toma literal, sin parsear entidades.
     *
     * @throws DOMException {@code INVALID_CHARACTER_ERR} o {@code NO_MODIFICATION_ALLOWED_ERR}
     */
    public void setAttribute(String name, String value) throws DOMException;

    /**
     * Saca el atributo. Si el DTD le declaraba un valor por omision, vuelve a aparecer con ese
     * valor; no falla si el atributo no estaba.
     *
     * @throws DOMException {@code NO_MODIFICATION_ALLOWED_ERR}
     */
    public void removeAttribute(String name) throws DOMException;

    /** El nodo del atributo, o {@code null}. */
    public Attr getAttributeNode(String name);

    /**
     * Agrega el nodo de atributo y devuelve el que reemplazo, o {@code null}.
     *
     * @throws DOMException {@code WRONG_DOCUMENT_ERR}, {@code NO_MODIFICATION_ALLOWED_ERR} o
     *     {@code INUSE_ATTRIBUTE_ERR} si el atributo ya pertenece a otro elemento
     */
    public Attr setAttributeNode(Attr newAttr) throws DOMException;

    /**
     * Saca ese nodo de atributo y lo devuelve.
     *
     * @throws DOMException {@code NO_MODIFICATION_ALLOWED_ERR} o {@code NOT_FOUND_ERR}
     */
    public Attr removeAttributeNode(Attr oldAttr) throws DOMException;

    /**
     * Los descendientes con esa etiqueta, en orden de documento; {@code "*"} los trae todos. La
     * lista esta viva.
     */
    public NodeList getElementsByTagName(String name);

    /**
     * El valor del atributo con ese espacio de nombres y nombre local, o {@code ""}.
     *
     * @throws DOMException {@code NOT_SUPPORTED_ERR} si la implementacion no maneja XML
     */
    public String getAttributeNS(String namespaceURI, String localName) throws DOMException;

    /**
     * Pone o reemplaza un atributo con espacio de nombres.
     *
     * @throws DOMException {@code INVALID_CHARACTER_ERR}, {@code NO_MODIFICATION_ALLOWED_ERR},
     *     {@code NAMESPACE_ERR} o {@code NOT_SUPPORTED_ERR}
     */
    public void setAttributeNS(String namespaceURI, String qualifiedName, String value)
            throws DOMException;

    /**
     * Saca el atributo con ese espacio de nombres y nombre local.
     *
     * @throws DOMException {@code NO_MODIFICATION_ALLOWED_ERR} o {@code NOT_SUPPORTED_ERR}
     */
    public void removeAttributeNS(String namespaceURI, String localName) throws DOMException;

    /**
     * El nodo del atributo con ese espacio de nombres y nombre local, o {@code null}.
     *
     * @throws DOMException {@code NOT_SUPPORTED_ERR}
     */
    public Attr getAttributeNodeNS(String namespaceURI, String localName) throws DOMException;

    /**
     * Agrega el nodo de atributo por (URI, nombre local) y devuelve el que reemplazo, o {@code null}.
     *
     * @throws DOMException {@code WRONG_DOCUMENT_ERR}, {@code NO_MODIFICATION_ALLOWED_ERR},
     *     {@code INUSE_ATTRIBUTE_ERR} o {@code NOT_SUPPORTED_ERR}
     */
    public Attr setAttributeNodeNS(Attr newAttr) throws DOMException;

    /**
     * Los descendientes con ese espacio de nombres y nombre local, en orden de documento.
     *
     * @throws DOMException {@code NOT_SUPPORTED_ERR}
     */
    public NodeList getElementsByTagNameNS(String namespaceURI, String localName)
            throws DOMException;

    /** Si el atributo existe, o si el DTD le da un valor por omision. */
    public boolean hasAttribute(String name);

    /**
     * Si existe el atributo con ese espacio de nombres y nombre local.
     *
     * @throws DOMException {@code NOT_SUPPORTED_ERR}
     */
    public boolean hasAttributeNS(String namespaceURI, String localName) throws DOMException;

    /** La informacion de tipo del esquema para este elemento, o {@code null}. */
    public TypeInfo getSchemaTypeInfo();

    /**
     * Marca --o desmarca-- ese atributo como de tipo ID.
     *
     * @throws DOMException {@code NO_MODIFICATION_ALLOWED_ERR} o {@code NOT_FOUND_ERR}
     */
    public void setIdAttribute(String name, boolean isId) throws DOMException;

    /**
     * Igual que {@link #setIdAttribute} pero identificando el atributo por (URI, nombre local).
     *
     * @throws DOMException {@code NO_MODIFICATION_ALLOWED_ERR} o {@code NOT_FOUND_ERR}
     */
    public void setIdAttributeNS(String namespaceURI, String localName, boolean isId)
            throws DOMException;

    /**
     * Igual que {@link #setIdAttribute} pero pasando el nodo del atributo.
     *
     * @throws DOMException {@code NO_MODIFICATION_ALLOWED_ERR} o {@code NOT_FOUND_ERR}
     */
    public void setIdAttributeNode(Attr idAttr, boolean isId) throws DOMException;
}
