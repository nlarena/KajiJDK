package javax.xml.xpath;

import java.util.List;

/**
 * KajiLibrary's javax.xml.xpath.XPathFunction -- una funcion propia, llamable desde la expresion.
 *
 * <p>XPath 1.0 trae unas treinta funciones y no hay forma de escribir una nueva <b>en</b> XPath. Esta
 * interfaz es la salida: una funcion escrita en Java que la expresion llama por nombre.
 *
 * <p>Los argumentos llegan como una lista de {@code Object} y el mapeo importa: un numero de XPath
 * llega como {@code Double} --XPath 1.0 no tiene enteros--, un conjunto de nodos como
 * {@code NodeList}, y una cadena como {@code String}. Devolver algo que no sea uno de esos tipos deja
 * el resultado sin definir.
 */
public interface XPathFunction {

    /**
     * Corre la funcion.
     *
     * @param args los argumentos, ya convertidos a los tipos de XPath
     * @throws XPathFunctionException si la funcion falla
     */
    Object evaluate(List<?> args) throws XPathFunctionException;
}
