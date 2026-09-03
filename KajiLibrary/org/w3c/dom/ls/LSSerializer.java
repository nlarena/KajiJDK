package org.w3c.dom.ls;

import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;

/**
 * KajiLibrary's org.w3c.dom.ls.LSSerializer -- convierte un arbol otra vez en texto.
 *
 * <p>La vuelta de {@link LSParser}. Los tres {@code write} se diferencian en el destino y en una
 * cosa mas importante: {@link #writeToString} tiene que armar toda la salida en memoria, asi que
 * para un documento grande hay que usar alguno de los otros dos.
 *
 * <h2>Serializar no es lo inverso de analizar</h2>
 *
 * <p>Vale tenerlo presente porque sorprende: leer y volver a escribir un documento <b>no</b> devuelve
 * los mismos bytes. El orden de los atributos no se conserva --el modelo DOM no lo guarda--, las
 * comillas pueden cambiar, el espacio entre atributos se normaliza y las entidades pueden quedar
 * expandidas. Lo que se conserva es el documento en el sentido de XML, no su forma. Comparar dos XML
 * comparando texto es, por eso, casi siempre el metodo equivocado.
 *
 * <p>{@link #getNewLine} es la excepcion util a eso: es lo unico de la forma que se puede fijar, y
 * existe justamente porque el fin de linea es lo primero que rompe una comparacion entre sistemas.
 */
public interface LSSerializer {

    /** Los parametros de la salida, por nombre. */
    DOMConfiguration getDomConfig();

    /** El fin de linea que se usa, o null para el del sistema. */
    String getNewLine();

    /** Ver {@link #getNewLine}; null vuelve al del sistema. */
    void setNewLine(String newLine);

    /** El filtro que decide que nodos salen, o null. */
    LSSerializerFilter getFilter();

    /** Ver {@link #getFilter}. */
    void setFilter(LSSerializerFilter filter);

    /**
     * Escribe el nodo en ese destino.
     *
     * @return si se escribio; false cuando el destino no se pudo resolver
     * @throws LSException con {@link LSException#SERIALIZE_ERR} si algo no se pudo serializar
     */
    boolean write(Node nodeArg, LSOutput destination) throws LSException;

    /** Idem, a un URI. */
    boolean writeToURI(Node nodeArg, String uri) throws LSException;

    /**
     * Idem, a una cadena.
     *
     * <p>Arma todo en memoria; ver la nota de la clase.
     *
     * @throws DOMException si el resultado no entra en un {@code String}
     */
    String writeToString(Node nodeArg) throws DOMException, LSException;
}
