package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.Attr -- un atributo de un elemento.
 *
 * <p>Un {@code Attr} extiende {@link Node}, y ahi esta la peculiaridad que confunde a todo el que
 * llega al DOM desde otro modelo: **un atributo es un nodo pero no es hijo de nadie**.
 * {@link Node#getParentNode} devuelve siempre {@code null}, no aparece en
 * {@link Node#getChildNodes} de su elemento, y la relacion con el elemento va por
 * {@link #getOwnerElement}, que es un camino aparte. Los atributos de un elemento se llegan por
 * {@link Node#getAttributes}, que devuelve un {@link NamedNodeMap} y no una {@link NodeList}.
 *
 * <p>Lo otro que sorprende es que el valor **no** es una cadena guardada en el nodo sino los hijos
 * del atributo: uno o mas {@link Text} y {@link EntityReference}. {@link #getValue} los concatena
 * resolviendo las entidades. Por eso un atributo tiene hijos aunque no tenga padre.
 *
 * <p>{@link #getSpecified} distingue el atributo que estaba escrito en el documento del que aparecio
 * por un valor por omision del DTD, distincion que solo existe si hubo gramatica.
 *
 * <p>Interfaz declarada entera.
 */
public interface Attr extends Node {

    /** El nombre del atributo. */
    public String getName();

    /**
     * Si el atributo estaba escrito en el documento ({@code true}) o vino de un valor por omision
     * del DTD ({@code false}).
     */
    public boolean getSpecified();

    /** El valor, con las entidades resueltas y los hijos {@link Text} concatenados. */
    public String getValue();

    /**
     * Fija el valor. El texto se toma **literal**: no se parsea, asi que un {@code "&amp;"} queda
     * como esos cinco caracteres y no como un ampersand.
     *
     * @throws DOMException {@code NO_MODIFICATION_ALLOWED_ERR} si el nodo es de solo lectura
     */
    public void setValue(String value) throws DOMException;

    /** El elemento al que pertenece, o {@code null} si el atributo esta suelto. */
    public Element getOwnerElement();

    /** La informacion de tipo del esquema, o {@code null} si no hay validacion. */
    public TypeInfo getSchemaTypeInfo();

    /**
     * Si este atributo es de tipo ID, o sea si sirve para {@link Document#getElementById}.
     *
     * <p>Un atributo llamado {@code "id"} no es un ID por llamarse asi: lo tiene que declarar el
     * DTD, el esquema, o alguien con {@link Element#setIdAttribute}.
     */
    public boolean isId();
}
