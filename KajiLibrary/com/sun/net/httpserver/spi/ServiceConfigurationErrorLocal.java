package com.sun.net.httpserver.spi;

/**
 * El error de un proveedor nombrado por propiedad del sistema que no se pudo instanciar.
 *
 * <p>De paquete: es un detalle de {@link HttpServerProvider#provider()}. Existe para que el fallo
 * lleve <strong>el nombre que se pidio</strong> ademas de la causa — sin eso, un typo en la
 * propiedad produce un {@code ClassNotFoundException} pelado que no dice de donde salio el nombre.
 */
final class ServiceConfigurationErrorLocal extends Error {

    private static final long serialVersionUID = 8712374126493827162L;

    ServiceConfigurationErrorLocal(String nombre, Throwable causa) {
        super("no se pudo instanciar el HttpServerProvider '" + nombre + "'", causa);
    }
}
