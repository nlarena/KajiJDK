package java.lang.management;

import java.security.BasicPermission;

/**
 * KajiLibrary's java.lang.management.ManagementPermission -- permiso para mirar o tocar la maquina
 * virtual.
 *
 * <p>Solo dos nombres, y cualquier otro es un error de argumento:
 *
 * <ul>
 *   <li>{@code "monitor"} para leer -- volcados de hilos, uso de memoria, propiedades del sistema;
 *   <li>{@code "control"} para modificar -- forzar una recoleccion, cambiar umbrales, activar el
 *       seguimiento de contencion.
 * </ul>
 *
 * <p>No admite comodines, a diferencia de la mayoria de las {@link BasicPermission}. Con dos nombres
 * no haria falta, y permitirlos abriria la puerta a conceder {@code control} sin querer.
 *
 * <p>Marcada para eliminacion junto con todo el mecanismo de {@code SecurityManager}, que ya no
 * controla nada. Se mantiene para que el codigo viejo compile.
 */
@Deprecated(since = "25", forRemoval = true)
public final class ManagementPermission extends BasicPermission {

    private static final long serialVersionUID = 1897496590799378737L;

    /**
     * @param name {@code "monitor"} o {@code "control"}
     * @throws NullPointerException si es null
     * @throws IllegalArgumentException si es cualquier otra cosa
     */
    public ManagementPermission(String name) {
        super(name);
        if (!name.equals("control") && !name.equals("monitor")) {
            throw new IllegalArgumentException("name: " + name);
        }
    }

    /**
     * Idem; las acciones tienen que ser null o vacio.
     *
     * @throws IllegalArgumentException si se dan acciones
     */
    public ManagementPermission(String name, String actions) throws IllegalArgumentException {
        super(name);
        if (!name.equals("control") && !name.equals("monitor")) {
            throw new IllegalArgumentException("name: " + name);
        }
        if (actions != null && actions.length() > 0) {
            throw new IllegalArgumentException("actions: " + actions);
        }
    }
}
