package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.DOMStringList -- una lista ordenada de cadenas.
 *
 * <p>La usa {@link DOMConfiguration#getParameterNames}. Es la misma forma minima que
 * {@link NodeList} pero para cadenas, y existe por la misma razon que aquella: el DOM se
 * especifico en IDL y no podia devolver un {@code java.util.List}.
 *
 * <p>El detalle propio esta en {@link #contains}, que compara **sin distinguir mayusculas**: los
 * nombres de parametro del DOM son insensibles a la caja, y una lista que compare exacto haria
 * fallar la mitad de las consultas.
 *
 * <p>Interfaz declarada entera.
 */
public interface DOMStringList {

    /** La cadena en esa posicion, o {@code null} si el indice se fue de rango. */
    public String item(int index);

    /** Cuantas cadenas hay. */
    public int getLength();

    /** Si esa cadena esta en la lista, comparando sin distinguir mayusculas. */
    public boolean contains(String str);
}
