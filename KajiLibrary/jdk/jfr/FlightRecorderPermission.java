package jdk.jfr;

import java.security.BasicPermission;

/**
 * El permiso que historicamente guardaba las operaciones de JFR.
 *
 * <p>Los nombres son {@code accessFlightRecorder}, para manejar grabaciones, y
 * {@code registerEvent}, para registrar tipos de evento propios.
 *
 * <p>Se conserva porque es API publica, aunque el {@code SecurityManager} este deshabilitado
 * permanentemente desde JDK 24 y el chequeo ya no ocurra. Construir uno sigue funcionando; lo que
 * ya no pasa es que alguien lo consulte.
 *
 * @since 9
 */
public final class FlightRecorderPermission extends BasicPermission {

    private static final long serialVersionUID = -6989096058590316034L;

    /**
     * Un permiso con ese nombre.
     *
     * @param name {@code "accessFlightRecorder"} o {@code "registerEvent"}
     * @throws NullPointerException si el nombre es {@code null}
     * @throws IllegalArgumentException si el nombre es vacio
     */
    public FlightRecorderPermission(String name) {
        super(name);
    }
}
