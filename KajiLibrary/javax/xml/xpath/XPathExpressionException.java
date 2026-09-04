package javax.xml.xpath;

/**
 * KajiLibrary's javax.xml.xpath.XPathExpressionException -- la expresion no se pudo compilar o
 * evaluar.
 *
 * <p>Cubre las dos etapas y no las separa, que es una decision discutible del API: un error de
 * sintaxis --que se sabe al compilar-- y un error al evaluar --que depende del documento-- llegan
 * como el mismo tipo. Quien quiera distinguirlos tiene que mirar en que llamada salto.
 */
public class XPathExpressionException extends XPathException {

    private static final long serialVersionUID = -1837080260374986980L;

    /** Con un mensaje. */
    public XPathExpressionException(String message) {
        super(message);
    }

    /** Con la causa de abajo. */
    public XPathExpressionException(Throwable cause) {
        super(cause);
    }
}
