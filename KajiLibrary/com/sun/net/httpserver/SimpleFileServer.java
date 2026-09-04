package com.sun.net.httpserver;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Path;

/**
 * El servidor de archivos que respalda al comando {@code jwebserver}.
 *
 * <h2>Que es y que no</h2>
 *
 * <p>Es para servir un directorio en una prueba o una demo, y su documentacion lo dice de frente:
 * <strong>no esta pensado para produccion</strong>. Solo atiende {@code GET} y {@code HEAD}, no
 * tiene autenticacion ni cifrado, y no interpreta nada — sirve bytes.
 *
 * <p>La ruta que recibe tiene que ser <strong>absoluta</strong>, y eso no es formalismo: es lo que
 * fija la raiz contra la que se resuelve cada pedido, o sea lo unico que impide que un
 * {@code ../../} se lleve el resto del disco.
 *
 * <p>Sin proveedor de servidor instalado, {@link #createFileServer} tira
 * {@link UnsupportedOperationException} — ver {@link HttpServer}.
 */
public final class SimpleFileServer {

    private SimpleFileServer() {
    }

    /**
     * Cuanto registra el filtro de salida.
     *
     * <p>{@link #NONE} no es lo mismo que no poner filtro: sigue existiendo en la cadena, y la
     * diferencia se nota si alguien la recorre.
     */
    public enum OutputLevel {

        /** Nada. */
        NONE,
        /** Una linea por pedido: metodo, URI, codigo. */
        INFO,
        /** Ademas, todos los encabezados del pedido y de la respuesta. */
        VERBOSE
    }

    /**
     * Un servidor que sirve {@code rootDirectory}.
     *
     * @throws IllegalArgumentException si la ruta no es absoluta o no es un directorio
     * @throws UnsupportedOperationException si no hay proveedor de servidor
     */
    public static HttpServer createFileServer(InetSocketAddress addr, Path rootDirectory,
            OutputLevel outputLevel) {
        throw new UnsupportedOperationException(
                "esta VM no trae proveedor de HttpServer; ver com.sun.net.httpserver.spi");
    }

    /**
     * Solo el manejador, para montarlo en una ruta de un servidor propio.
     *
     * @throws IllegalArgumentException si la ruta no es absoluta o no es un directorio
     */
    public static HttpHandler createFileHandler(Path rootDirectory) {
        throw new UnsupportedOperationException(
                "esta VM no implementa el manejador de archivos de jwebserver");
    }

    /**
     * Solo el filtro de registro, que sirve para cualquier manejador y no solo para este.
     *
     * @throws NullPointerException si falta la salida o el nivel
     */
    public static Filter createOutputFilter(OutputStream out, OutputLevel outputLevel) {
        throw new UnsupportedOperationException(
                "esta VM no implementa el filtro de registro de jwebserver");
    }
}
