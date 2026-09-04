package javax.xml.xpath;

/**
 * KajiLibrary's javax.xml.xpath.XPathFactoryConfigurationException -- no hay fabrica para ese modelo
 * de objetos.
 *
 * <p>A diferencia de {@code FactoryConfigurationError} de {@code javax.xml.parsers}, esta es una
 * <b>excepcion comprobada</b> y no un error. La diferencia tiene sentido: alla la falla es que no hay
 * XML en la plataforma, que es irrecuperable; aca es que no hay soporte para <b>un modelo de objetos
 * en particular</b>, y un programa razonable puede probar con otro.
 */
public class XPathFactoryConfigurationException extends XPathException {

    private static final long serialVersionUID = -1837080260374986980L;

    /** Con un mensaje. */
    public XPathFactoryConfigurationException(String message) {
        super(message);
    }

    /** Con la causa de abajo. */
    public XPathFactoryConfigurationException(Throwable cause) {
        super(cause);
    }
}
