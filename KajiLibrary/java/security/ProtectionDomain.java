package java.security;

// La unidad sobre la que se decide: de donde vino el codigo, quien lo carga, con que identidad
// corre, y que permisos tiene.
//
// Es el sujeto de toda la pregunta de control de acceso. Un permiso no se le concede "a una
// clase": se le concede a un dominio, y todas las clases que comparten origen, cargador y
// principales comparten dominio y por lo tanto permisos.
//
// ===============================================================================================
// PERMISOS ESTATICOS VS. DINAMICOS
// ===============================================================================================
//
// La diferencia entre los dos constructores es la mas importante de la clase y esta escondida en
// un booleano sin nombre visible:
//
//   - El de **dos** argumentos crea un dominio de permisos **estaticos**: los que se le pasaron y
//     nada mas, para siempre. `staticPermissionsOnly()` da `true`.
//   - El de **cuatro** crea uno **dinamico**: los que se le pasaron **mas** los que la `Policy`
//     vigente le conceda en el momento de preguntar. `staticPermissionsOnly()` da `false`.
//
// Refrescar la politica cambia lo que puede hacer un dominio dinamico y no toca a uno estatico. Es
// la unica forma de que un cambio de politica tenga efecto sobre codigo ya cargado.
//
// La coleccion de permisos se marca de **solo lectura** al construir el dominio: si no, quien
// entrego los permisos podria agregarse mas despues de que el dominio ya fue aceptado.
//
// (En KajiJDK no hay ninguna `Policy` con contenido, asi que la parte dinamica siempre suma cero.
// La distincion se implementa igual porque es contrato observable: `staticPermissionsOnly()`
// contesta distinto segun que constructor se uso.)
public class ProtectionDomain {

    // null significa "origen desconocido", y no implica nada.
    private final CodeSource codesource;

    private final PermissionCollection permissions;

    private final ClassLoader classloader;

    // Nunca null: un dominio sin principales tiene un arreglo vacio, no null. Simplifica a todos
    // los que lo recorren.
    private final Principal[] principals;

    // Atajo: si los permisos ya incluyen `AllPermission`, no hace falta consultar nada mas.
    private final boolean hasAllPerm;

    private final boolean staticPermissions;

    // Un dominio de permisos estaticos.
    public ProtectionDomain(CodeSource codesource, PermissionCollection permissions) {
        this.codesource = codesource;
        this.permissions = permissions;
        this.hasAllPerm = marcarYDetectarTodos(permissions);
        this.classloader = null;
        this.principals = new Principal[0];
        this.staticPermissions = true;
    }

    // Un dominio de permisos dinamicos: a los dados se les suman los que conceda la politica.
    public ProtectionDomain(CodeSource codesource, PermissionCollection permissions,
                            ClassLoader classloader, Principal[] principals) {
        this.codesource = codesource;
        this.permissions = permissions;
        this.hasAllPerm = marcarYDetectarTodos(permissions);
        this.classloader = classloader;
        this.principals = principals == null ? new Principal[0] : copiar(principals);
        this.staticPermissions = false;
    }

    private static Principal[] copiar(Principal[] a) {
        Principal[] c = new Principal[a.length];
        System.arraycopy(a, 0, c, 0, a.length);
        return c;
    }

    // Cierra la coleccion y avisa si trae el permiso universal.
    private static boolean marcarYDetectarTodos(PermissionCollection pc) {
        if (pc == null) {
            return false;
        }
        pc.setReadOnly();
        if (pc instanceof Permissions) {
            return ((Permissions) pc).allPermission != null;
        }
        return false;
    }

    public final CodeSource getCodeSource() {
        return this.codesource;
    }

    // El cargador de este dominio, o null si las clases las definio el cargador de arranque.
    public final ClassLoader getClassLoader() {
        return this.classloader;
    }

    public final Principal[] getPrincipals() {
        return copiar(this.principals);
    }

    // La coleccion estatica de permisos, o null. No incluye lo que la politica pueda conceder: para
    // eso esta `implies`.
    public final PermissionCollection getPermissions() {
        return this.permissions;
    }

    // Si este dominio ignora la politica.
    public final boolean staticPermissionsOnly() {
        return this.staticPermissions;
    }

    // Si este dominio tiene el permiso pedido.
    public boolean implies(Permission perm) {
        if (this.hasAllPerm) {
            return true;
        }
        if (!this.staticPermissions && Policy.getPolicy().implies(this, perm)) {
            return true;
        }
        if (this.permissions != null) {
            return this.permissions.implies(perm);
        }
        return false;
    }

    @Override
    public String toString() {
        String pals = "<no principals>";
        if (this.principals.length > 0) {
            StringBuilder b = new StringBuilder("(principals ");
            int i = 0;
            while (i < this.principals.length) {
                b.append(this.principals[i].getClass().getName());
                b.append(" \"");
                b.append(this.principals[i].getName());
                b.append("\"");
                if (i < this.principals.length - 1) {
                    b.append(",\n");
                } else {
                    b.append(")\n");
                }
                i = i + 1;
            }
            pals = b.toString();
        }
        return "ProtectionDomain "
            + " " + this.codesource + "\n"
            + " " + this.classloader + "\n"
            + " " + pals + "\n"
            + " " + this.permissions + "\n";
    }
}
