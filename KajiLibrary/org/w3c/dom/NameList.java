package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.NameList -- una lista ordenada de pares (espacio de nombres, nombre).
 *
 * <p>Es {@link DOMStringList} con namespaces: cada posicion tiene dos cadenas en vez de una, y se
 * accede a cada mitad por separado con {@link #getName} y {@link #getNamespaceURI} sobre el mismo
 * indice. No hay un tipo "nombre calificado" en el DOM, de ahi el par de accesores paralelos en vez
 * de un objeto.
 *
 * <p>La declara la norma para las APIs de validacion --que preguntan que nombres son legales en un
 * lugar-- y el nucleo no la devuelve en ningun lado. Esta igual porque es API publica del paquete.
 *
 * <p>Interfaz declarada entera.
 */
public interface NameList {

    /** El nombre en esa posicion, o {@code null} si el indice se fue de rango. */
    public String getName(int index);

    /** La URI del espacio de nombres en esa posicion, o {@code null}. */
    public String getNamespaceURI(int index);

    /** Cuantos pares hay. */
    public int getLength();

    /** Si ese nombre esta, mirando solo los nombres. */
    public boolean contains(String str);

    /** Si ese par (URI, nombre) esta. */
    public boolean containsNS(String namespaceURI, String name);
}
