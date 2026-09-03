package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.DOMConfiguration -- los parametros con que se normaliza un documento.
 *
 * <p>La devuelve {@link Document#getDomConfig} y la consume {@link Document#normalizeDocument}.
 * Es un mapa de parametros con nombre, y es asi --y no una interfaz con un metodo por opcion--
 * porque el juego de parametros es **abierto**: la norma define unos veinte
 * ({@code "comments"}, {@code "cdata-sections"}, {@code "entities"}, {@code "namespaces"},
 * {@code "validate"}, {@code "error-handler"}...) y cada implementacion agrega los suyos con un
 * prefijo propio.
 *
 * <p>Los nombres no distinguen mayusculas. El valor es un {@code Object} porque casi todos son
 * booleanos pero algunos no --{@code "error-handler"} quiere un {@link DOMErrorHandler},
 * {@code "schema-location"} una cadena.
 *
 * <p>La parte que se olvida: {@link #canSetParameter} existe porque un parametro puede estar
 * **soportado pero no en ese valor**. Una implementacion que siempre valide acepta
 * {@code ("validate", true)} y rechaza {@code ("validate", false)}, y sin este metodo la unica
 * forma de averiguarlo seria provocando la excepcion.
 *
 * <p>Interfaz declarada entera.
 */
public interface DOMConfiguration {

    /**
     * Fija un parametro.
     *
     * @throws DOMException {@code NOT_FOUND_ERR} si el parametro no se reconoce, o
     *     {@code NOT_SUPPORTED_ERR} si se reconoce pero ese valor no se soporta
     */
    public void setParameter(String name, Object value) throws DOMException;

    /**
     * El valor actual del parametro.
     *
     * @throws DOMException {@code NOT_FOUND_ERR} si el parametro no se reconoce
     */
    public Object getParameter(String name) throws DOMException;

    /** Si ese parametro se puede poner en ese valor, sin intentarlo. */
    public boolean canSetParameter(String name, Object value);

    /** Los nombres de todos los parametros que esta configuracion reconoce. */
    public DOMStringList getParameterNames();
}
