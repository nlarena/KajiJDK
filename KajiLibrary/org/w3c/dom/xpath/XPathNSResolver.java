package org.w3c.dom.xpath;

/**
 * KajiLibrary's org.w3c.dom.xpath.XPathNSResolver -- traduce prefijos a espacios de nombres.
 *
 * <p>Hace falta porque los prefijos de una expresion XPath <b>no tienen por que ser los del
 * documento</b>: la expresion la escribe quien consulta, el documento lo escribio otro, y los dos
 * pueden usar prefijos distintos para el mismo espacio de nombres -- o el mismo prefijo para dos
 * distintos. Lo que se compara es el espacio, y este resolvedor es quien lo dice.
 */
public interface XPathNSResolver {

    /**
     * El espacio de nombres de ese prefijo, o null si no lo conoce.
     *
     * @param prefix el prefijo, o null para el espacio por omision
     */
    String lookupNamespaceURI(String prefix);
}
