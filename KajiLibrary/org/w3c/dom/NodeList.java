package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.NodeList -- una coleccion ordenada de nodos, indexada desde cero.
 *
 * <p>Lo que agrega sobre una `List` de Java: **nada**, y ese es justamente el punto. El DOM se
 * especifico para varios lenguajes a la vez y no podia apoyarse en la biblioteca de ninguno, asi que
 * declaro su propia coleccion minima. De ahi que no sea `Iterable` ni tenga `size()`.
 *
 * <p><strong>Lo que si tiene y una `List` no: casi siempre esta viva.</strong> La lista que devuelve
 * `getChildNodes()` o `getElementsByTagName()` refleja el arbol en el momento en que se la consulta,
 * no una foto de cuando se la pidio. Por eso el bucle `for (int i = 0; i &lt; l.getLength(); i++)`
 * que borra nodos adentro se saltea la mitad: cada borrado corre el resto un lugar. Es el error
 * clasico del DOM y no hay nada en la firma que avise.
 */
public interface NodeList {

    /** `null` --no una excepcion-- si el indice esta fuera de rango. */
    Node item(int index);

    int getLength();
}
