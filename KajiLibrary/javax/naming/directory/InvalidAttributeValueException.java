package javax.naming.directory;

import javax.naming.NamingException;

/**
 * KajiLibrary's javax.naming.directory.InvalidAttributeValueException -- el valor no sirve para ese atributo.
 *
 * <p>La contracara de {@link InvalidAttributeIdentifierException}: el nombre esta bien y el valor
 * no. Tipicamente porque el esquema declara el atributo como de un solo valor y se le
 * pasaron dos, o porque el tipo no coincide.
 */
public class InvalidAttributeValueException extends NamingException {

    private static final long serialVersionUID = 8720050295499275011L;

    /** Sin detalle. */
    public InvalidAttributeValueException() {
        super();
    }

    /** Con un mensaje que diga cual fue el problema. */
    public InvalidAttributeValueException(String explanation) {
        super(explanation);
    }
}
