package javax.naming.directory;

import javax.naming.NamingException;

/**
 * KajiLibrary's javax.naming.directory.InvalidSearchControlsException -- los controles de busqueda no sirven.
 *
 * <p>Un {@link SearchControls} con un alcance que no existe, o con limites incoherentes. Se sabe
 * antes de tocar el directorio, asi que aparece en la llamada y no a mitad de la
 * enumeracion.
 */
public class InvalidSearchControlsException extends NamingException {

    private static final long serialVersionUID = -5124108943352665777L;

    /** Sin detalle. */
    public InvalidSearchControlsException() {
        super();
    }

    /** Con un mensaje que diga cual fue el problema. */
    public InvalidSearchControlsException(String explanation) {
        super(explanation);
    }
}
