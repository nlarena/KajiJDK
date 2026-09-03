package javax.security.auth;

import java.security.BasicPermission;

/**
 * KajiLibrary's javax.security.auth.AuthPermission -- permiso para las operaciones de autenticacion.
 *
 * <p>Es un {@link BasicPermission} y por lo tanto no tiene acciones: el nombre <b>es</b> el permiso.
 * Los nombres son {@code "doAs"}, {@code "getSubject"}, {@code "createLoginContext.<name>"} y
 * companía, y como en todo {@code BasicPermission} el {@code *} final abarca un prefijo:
 * {@code "createLoginContext.*"} implica {@code "createLoginContext.Kaji"}.
 *
 * <p>Hay una traduccion historica que se reproduce a proposito porque es observable: el nombre
 * {@code "createLoginContext"} pelado se guarda como {@code "createLoginContext.*"}. Viene de cuando
 * ese permiso no llevaba el nombre de la configuracion; escribirlo sin el punto hoy pediria un
 * permiso que no existe, asi que el JDK lo interpreta como el comodin en vez de dejarlo inutil.
 *
 * <p>Nota sobre para que sirve hoy: el gestor de seguridad ya no se puede habilitar, asi que ningun
 * chequeo de la biblioteca consulta este permiso. La clase existe igual porque su forma es parte del
 * API -- se guarda en politicas, se compara, se serializa -- y porque quien escriba su propio
 * control de acceso la puede usar como cualquier otro {@code Permission}.
 */
public final class AuthPermission extends BasicPermission {

    private static final long serialVersionUID = 5806031445061587174L;

    // Ver la nota de la clase: sin esto, el nombre viejo no implicaria nada.
    private static String translated(String name) {
        return "createLoginContext".equals(name) ? "createLoginContext.*" : name;
    }

    public AuthPermission(String name) {
        super(translated(name));
    }

    /**
     * Las acciones se ignoran: un {@code BasicPermission} no tiene. El constructor existe porque el
     * cargador de politicas construye todos los permisos con dos argumentos y no sabe cuales los
     * usan.
     */
    public AuthPermission(String name, String actions) {
        super(translated(name), actions);
    }
}
