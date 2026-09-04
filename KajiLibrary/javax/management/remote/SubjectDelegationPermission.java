package javax.management.remote;

import java.security.BasicPermission;

/**
 * KajiLibrary's javax.management.remote.SubjectDelegationPermission -- permiso para actuar en nombre
 * de otro.
 *
 * <p>Un cliente JMX autenticado como uno puede pedir que sus operaciones corran como otro; este era el
 * permiso que lo habilitaba, con el nombre del delegado como objetivo.
 *
 * <p>Marcada para eliminacion junto con todo el {@code SecurityManager}: sin gestor de seguridad no
 * hay quien la controle, asi que ya no protege nada. Se mantiene para que el codigo viejo compile.
 *
 * <p>Hereda de {@link BasicPermission}, asi que soporta comodines: {@code "*"} permite delegar en
 * cualquiera y {@code "a.b.*"} en cualquiera de ese prefijo.
 */
@Deprecated(since = "25", forRemoval = true)
public final class SubjectDelegationPermission extends BasicPermission {

    private static final long serialVersionUID = 1481618113008682343L;

    /**
     * @param name el objetivo, con comodines opcionales
     * @throws NullPointerException si es null
     * @throws IllegalArgumentException si esta vacio
     */
    public SubjectDelegationPermission(String name) {
        super(name);
    }

    /**
     * Idem; las acciones tienen que ser null o vacio.
     *
     * @throws IllegalArgumentException si se dan acciones
     */
    public SubjectDelegationPermission(String name, String actions) {
        super(name, actions);
    }
}
