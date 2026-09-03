package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.DOMLocator -- donde ocurrio algo, dicho de cuatro maneras a la vez.
 *
 * <p>Lo devuelve {@link DOMError#getLocation}. Que haya cuatro coordenadas no es redundancia: cada
 * una sirve para un consumidor distinto y **no todas estan siempre disponibles**. Linea y columna
 * son para mostrarle el problema a una persona; el desplazamiento en bytes le sirve a quien tenga el
 * archivo original abierto; el desplazamiento en unidades UTF-16 le sirve a quien tenga el texto ya
 * decodificado en memoria, que es otra cosa. Y {@link #getRelatedNode} es la unica util cuando el
 * documento no vino de ningun texto sino que se armo nodo por nodo, caso en el que las tres
 * anteriores devuelven {@code -1}.
 *
 * <p>Ese {@code -1} es el convenio para "no se sabe" en las cuatro numericas. La numeracion de linea
 * y columna arranca en 1.
 *
 * <p>Interfaz declarada entera.
 */
public interface DOMLocator {

    /** La linea, contando desde 1, o {@code -1} si no se sabe. */
    public int getLineNumber();

    /** La columna, contando desde 1, o {@code -1} si no se sabe. */
    public int getColumnNumber();

    /** El desplazamiento en bytes dentro de la entrada, o {@code -1}. */
    public int getByteOffset();

    /** El desplazamiento en unidades de codigo UTF-16, o {@code -1}. */
    public int getUtf16Offset();

    /** El nodo al que apunta, o {@code null}. */
    public Node getRelatedNode();

    /** La URI de donde salio, o {@code null}. */
    public String getUri();
}
