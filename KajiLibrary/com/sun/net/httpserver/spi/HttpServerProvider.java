package com.sun.net.httpserver.spi;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.ServiceLoader;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsServer;

/**
 * Quien fabrica los servidores; el punto de extension detras de {@link HttpServer#create}.
 *
 * <h2>Como se elige</h2>
 *
 * <p>Tres lugares, en orden: la propiedad del sistema
 * {@code com.sun.net.httpserver.HttpServerProvider}, despues los proveedores que encuentre
 * {@link ServiceLoader}, y por ultimo el proveedor por omision de la plataforma.
 *
 * <p><strong>Esta VM no trae el tercero</strong>, asi que sin ninguno registrado
 * {@link #provider()} tira {@link UnsupportedOperationException} con el motivo. No es un stub: los
 * dos primeros mecanismos funcionan, y registrar un proveedor hace andar todo `com.sun.net.httpserver`
 * sin tocar una linea de aca.
 */
public abstract class HttpServerProvider {

    private static HttpServerProvider elegido;

    /** Para las implementaciones. */
    protected HttpServerProvider() {
    }

    /** Un servidor HTTP; {@code addr} puede ser {@code null} para no ligarlo todavia. */
    public abstract HttpServer createHttpServer(InetSocketAddress addr, int backlog)
            throws IOException;

    /** Un servidor HTTPS, al que despues hay que ponerle su configurador de TLS. */
    public abstract HttpsServer createHttpsServer(InetSocketAddress addr, int backlog)
            throws IOException;

    /**
     * El proveedor a usar, buscado una sola vez.
     *
     * @throws UnsupportedOperationException si no hay ninguno — ver la nota de la clase
     */
    public static synchronized HttpServerProvider provider() {
        if (elegido != null) {
            return elegido;
        }
        String nombre = System.getProperty("com.sun.net.httpserver.HttpServerProvider");
        if (nombre != null) {
            try {
                Class<?> c = Class.forName(nombre, true, ClassLoader.getSystemClassLoader());
                elegido = (HttpServerProvider) c.getDeclaredConstructor().newInstance();
                return elegido;
            } catch (Exception e) {
                throw new ServiceConfigurationErrorLocal(nombre, e);
            }
        }
        Iterator<HttpServerProvider> it =
                ServiceLoader.load(HttpServerProvider.class).iterator();
        if (it.hasNext()) {
            elegido = it.next();
            return elegido;
        }
        throw new UnsupportedOperationException(
                "no hay ningun HttpServerProvider: esta VM no trae el proveedor por omision");
    }
}
