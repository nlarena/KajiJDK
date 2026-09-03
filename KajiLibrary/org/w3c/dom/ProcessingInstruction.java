package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.ProcessingInstruction -- un {@code <?destino datos?>}.
 *
 * <p>Es el mecanismo que da XML para meter en el documento algo dirigido a **una** aplicacion sin
 * que sea contenido: el ejemplo de todos los dias es
 * {@code <?xml-stylesheet type="text/xsl" href="v.xsl"?>}. El destino nombra al destinatario y los
 * datos son opacos --el parser no los interpreta, ni siquiera como pares clave-valor, aunque casi
 * todo el mundo los escriba asi.
 *
 * <p>Notar que **no** extiende {@link CharacterData} aunque tenga texto: los datos de una PI no son
 * contenido del documento, no cuentan para {@link Node#getTextContent} del padre, y por eso tiene su
 * propio par de accesores en vez de heredar las ocho operaciones de edicion de texto.
 *
 * <p>La declaracion {@code <?xml version="1.0"?>} del principio **no** es una PI y no aparece como
 * nodo: es parte de la sintaxis del documento y se llega por {@link Document#getXmlVersion} y
 * compañia.
 *
 * <p>Interfaz declarada entera.
 */
public interface ProcessingInstruction extends Node {

    /** A quien va dirigida; es tambien lo que devuelve {@link Node#getNodeName}. */
    public String getTarget();

    /** El texto que sigue al destino hasta el {@code ?>}, sin interpretar. */
    public String getData();

    /**
     * Cambia los datos.
     *
     * @throws DOMException {@code NO_MODIFICATION_ALLOWED_ERR} si el nodo es de solo lectura
     */
    public void setData(String data) throws DOMException;
}
