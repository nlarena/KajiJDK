package java.util.logging;

/**
 * KajiLibrary's java.util.logging.LoggingPermission -- el unico permiso del paquete.
 *
 * <p>Tiene un solo nombre valido, `"control"`, y ninguna accion. Esa pobreza es el diseno: el
 * paquete no distingue "puede leer la configuracion" de "puede cambiarla", porque cambiar el nivel de
 * un logger ya alcanza para apagar la traza de auditoria de otro. Si hay una sola cosa que proteger,
 * hay un solo permiso.
 *
 * <p>El constructor **rechaza** cualquier otro nombre y cualquier accion no vacia en vez de
 * ignorarlos. Es lo correcto para un permiso: un `new LoggingPermission("controll", null)` mal
 * escrito que se construyera en silencio seria un permiso que nunca implica nada y una politica que
 * parece decir algo y no dice nada.
 *
 * <p>Esta deprecada para remocion en el JDK junto con el gestor de seguridad, que es lo unico que la
 * consultaba. Se trae igual porque sigue siendo parte de la API y porque
 * {@link java.security.BasicPermission} --de donde sale toda la logica de implicacion-- esta completo
 * en este arbol: aca no hay nada que simular.
 */
@Deprecated(since = "17", forRemoval = true)
public final class LoggingPermission extends java.security.BasicPermission {

    /**
     * @throws NullPointerException si `name` es `null`
     * @throws IllegalArgumentException si `name` no es `"control"`, o si `actions` no es vacio
     */
    public LoggingPermission(String name, String actions) throws IllegalArgumentException {
        super(name);
        if (!name.equals("control")) {
            throw new IllegalArgumentException("name: " + name);
        }
        if (actions != null && actions.length() > 0) {
            throw new IllegalArgumentException("actions: " + actions);
        }
    }
}
