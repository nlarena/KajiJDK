package javax.naming.directory;

import javax.naming.NamingException;

/**
 * KajiLibrary's javax.naming.directory.InvalidAttributesException -- faltan atributos obligatorios, o sobran.
 *
 * <p>Es sobre el <b>conjunto</b> y no sobre uno: crear una entrada sin los atributos que su clase
 * de objeto exige cae aca. Por eso es distinta de las dos de arriba, que hablan de un
 * atributo en particular.
 */
public class InvalidAttributesException extends NamingException {

    private static final long serialVersionUID = 2607612850539889765L;

    /** Sin detalle. */
    public InvalidAttributesException() {
        super();
    }

    /** Con un mensaje que diga cual fue el problema. */
    public InvalidAttributesException(String explanation) {
        super(explanation);
    }
}
