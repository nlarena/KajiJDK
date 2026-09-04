package javax.naming.directory;

import javax.naming.NamingException;

/**
 * KajiLibrary's javax.naming.directory.NoSuchAttributeException -- ese atributo no esta en la entrada.
 *
 * <p>Distinta de {@link InvalidAttributeIdentifierException}: alla el nombre no era valido, aca es
 * valido y la entrada no lo tiene.
 */
public class NoSuchAttributeException extends NamingException {

    private static final long serialVersionUID = 4836415647935888137L;

    /** Sin detalle. */
    public NoSuchAttributeException() {
        super();
    }

    /** Con un mensaje que diga cual fue el problema. */
    public NoSuchAttributeException(String explanation) {
        super(explanation);
    }
}
