package java.net.spi;

import java.net.URLStreamHandlerFactory;

/**
 * KajiLibrary's java.net.spi.URLStreamHandlerProvider -- protocolos de URL nuevos.
 *
 * <p>No declara nada propio: hereda el unico metodo de {@link URLStreamHandlerFactory} y solo agrega
 * el hecho de ser cargable como servicio. Esa es toda la diferencia con la fabrica vieja, y es una
 * diferencia real: la fabrica se instala llamando a {@code URL.setURLStreamHandlerFactory}, que se
 * puede llamar <b>una sola vez por proceso</b>, asi que la primera biblioteca que la usaba dejaba
 * afuera a todas las demas. Como servicio, cada una registra el suyo y la plataforma les pregunta a
 * todas por orden.
 *
 * <p>Un proveedor devuelve null para los protocolos que no le interesan, y ahi se le pregunta al que
 * sigue.
 */
public abstract class URLStreamHandlerProvider implements URLStreamHandlerFactory {

    /** Para las subclases. */
    protected URLStreamHandlerProvider() {
    }
}
