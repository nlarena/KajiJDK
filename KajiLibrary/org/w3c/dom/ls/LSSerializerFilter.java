package org.w3c.dom.ls;

import org.w3c.dom.traversal.NodeFilter;

/**
 * KajiLibrary's org.w3c.dom.ls.LSSerializerFilter -- decide que nodos salen al serializar.
 *
 * <p>Reusa {@code NodeFilter} en vez de definir lo suyo, que es la decision de diseno interesante
 * del tipo: escribir un documento es recorrerlo, y recorrerlo con un filtro ya estaba resuelto en
 * {@code org.w3c.dom.traversal}. Lo unico que agrega es redeclarar {@link #getWhatToShow} para
 * documentar una diferencia real: aca la mascara <b>no</b> puede excluir a los atributos, porque un
 * elemento sin sus atributos no seria el mismo elemento.
 *
 * <p>El {@code FILTER_SKIP} heredado tiene el mismo sentido que en {@link LSParserFilter}: el nodo
 * no sale pero sus hijos si.
 */
public interface LSSerializerFilter extends NodeFilter {

    /**
     * Que tipos de nodo se le pasan.
     *
     * <p>Los atributos se serializan siempre, se los incluya o no en la mascara; ver la nota de la
     * clase.
     */
    int getWhatToShow();
}
