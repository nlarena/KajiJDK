package javax.xml.parsers;

/**
 * KajiLibrary's javax.xml.parsers.ParserConfigurationException -- no se pudo armar el analizador.
 *
 * <p>Es de <b>configuracion</b>, no de contenido: se pidio algo que la implementacion no sabe hacer
 * --validar, o entender espacios de nombres, o una propiedad que no conoce-- y por eso no hay
 * analizador. La distincion con {@code SAXException} importa: aca todavia no se leyo un solo byte
 * del documento, asi que reintentar con el mismo pedido no puede andar.
 */
public class ParserConfigurationException extends Exception {

    private static final long serialVersionUID = -3688849216575373917L;

    /** Sin detalle. */
    public ParserConfigurationException() {
        super();
    }

    /** Con un mensaje que diga que se pidio y no se pudo dar. */
    public ParserConfigurationException(String msg) {
        super(msg);
    }
}
