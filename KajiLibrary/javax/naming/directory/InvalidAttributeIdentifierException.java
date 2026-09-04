package javax.naming.directory;

import javax.naming.NamingException;

/**
 * KajiLibrary's javax.naming.directory.InvalidAttributeIdentifierException -- el nombre del atributo no sirve.
 *
 * <p>El nombre, no el valor. Un directorio LDAP tiene reglas sobre que puede llamarse atributo, y
 * un identificador con caracteres que no corresponden se rechaza antes de mirar que hay
 * adentro.
 */
public class InvalidAttributeIdentifierException extends NamingException {

    private static final long serialVersionUID = -9036920266322999923L;

    /** Sin detalle. */
    public InvalidAttributeIdentifierException() {
        super();
    }

    /** Con un mensaje que diga cual fue el problema. */
    public InvalidAttributeIdentifierException(String explanation) {
        super(explanation);
    }
}
