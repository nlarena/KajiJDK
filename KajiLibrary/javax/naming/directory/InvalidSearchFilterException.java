package javax.naming.directory;

import javax.naming.NamingException;

/**
 * KajiLibrary's javax.naming.directory.InvalidSearchFilterException -- el filtro de busqueda esta mal escrito.
 *
 * <p>El filtro es una cadena con la sintaxis de RFC 2254 --parentesis, operadores, escapes-- y
 * armarla concatenando texto es como armar SQL asi: un valor con un parentesis adentro
 * cambia lo que el filtro selecciona. Para eso existe la version de {@code search} con
 * argumentos numerados.
 */
public class InvalidSearchFilterException extends NamingException {

    private static final long serialVersionUID = 2902700940682875441L;

    /** Sin detalle. */
    public InvalidSearchFilterException() {
        super();
    }

    /** Con un mensaje que diga cual fue el problema. */
    public InvalidSearchFilterException(String explanation) {
        super(explanation);
    }
}
