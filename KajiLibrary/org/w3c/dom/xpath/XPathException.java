package org.w3c.dom.xpath;

/**
 * KajiLibrary's org.w3c.dom.xpath.XPathException -- la expresion no se pudo compilar o su resultado
 * no se pudo leer como se pidio.
 *
 * <p>Los dos codigos separan los dos momentos en que algo puede salir mal, y conviene distinguirlos
 * porque uno es del que escribio la expresion y el otro del que lee el resultado:
 * {@link #INVALID_EXPRESSION_ERR} es "esto no es XPath", {@link #TYPE_ERR} es "esto es XPath pero no
 * devuelve lo que pediste".
 *
 * <p>No chequeada y con el codigo en un campo publico, por la convencion del DOM.
 */
public class XPathException extends RuntimeException {

    private static final long serialVersionUID = 6156942920132862751L;

    /** La expresion no es XPath valido. */
    public static final short INVALID_EXPRESSION_ERR = 51;

    /** El resultado no se puede convertir al tipo pedido. */
    public static final short TYPE_ERR = 52;

    /** Cual de los dos. Publico por la convencion del DOM. */
    public short code;

    public XPathException(short code, String message) {
        super(message);
        this.code = code;
    }
}
