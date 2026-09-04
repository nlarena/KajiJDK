package javax.print;

import java.net.URI;

/**
 * KajiLibrary's javax.print.URIException -- el fallo fue por un URI.
 *
 * <p>Una interfaz; ver la nota de {@link PrintException}. Aparece cuando un atributo lleva un URI
 * --tipicamente {@code Destination}, que dice a que archivo escribir-- y ese URI no sirve.
 *
 * <p>{@link #URIOtherProblem} vale -1 y no 3; las constantes vienen del estandar IPP y ahi el "otro"
 * es un valor aparte.
 */
public interface URIException {

    /** No se puede llegar. */
    int URIInaccessible = 1;

    /** El esquema no esta soportado. */
    int URISchemeNotSupported = 2;

    /** Otra cosa. */
    int URIOtherProblem = -1;

    /** El URI que fallo. */
    URI getUnsupportedURI();

    /** Cual de los tres motivos. */
    int getReason();
}
