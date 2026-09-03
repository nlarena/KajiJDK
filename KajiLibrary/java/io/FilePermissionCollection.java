package java.io;

import java.security.Permission;
import java.security.PermissionCollection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

// La coleccion que devuelve `FilePermission.newPermissionCollection()`. Package-private: el contrato
// solo promete una `PermissionCollection`, y el nombre no es API.
//
// Recorre en vez de indexar, y el porque esta en el javadoc de `newPermissionCollection`: un `/a/-`
// cubre rutas de cualquier profundidad, asi que no hay un puñado de claves que consultar.
//
// `implies` es la disyuncion de los `implies` de sus miembros y **no** su union: tener
// `("/tmp/x", "read")` y `("/tmp/x", "write")` no da `("/tmp/x", "read,write")` aca, igual que en el
// JDK. Combinar mascaras entre permisos distintos seria conceder algo que nadie escribio.
final class FilePermissionCollection extends PermissionCollection {

    private final List<Permission> permisos = new ArrayList<Permission>();

    FilePermissionCollection() {
    }

    /**
     * @throws IllegalArgumentException si no es un `FilePermission` -- mezclar clases haria que la
     *     coleccion contestara por permisos que no entiende
     * @throws SecurityException si ya se marco de solo lectura
     */
    public void add(Permission permission) {
        if (!(permission instanceof FilePermission)) {
            throw new IllegalArgumentException("invalid permission: " + permission);
        }
        if (this.isReadOnly()) {
            throw new SecurityException("attempt to add a Permission to a readonly PermissionCollection");
        }
        synchronized (this) {
            this.permisos.add(permission);
        }
    }

    public boolean implies(Permission permission) {
        if (!(permission instanceof FilePermission)) {
            return false;
        }
        synchronized (this) {
            int i = 0;
            while (i < this.permisos.size()) {
                if (this.permisos.get(i).implies(permission)) {
                    return true;
                }
                i = i + 1;
            }
        }
        return false;
    }

    public Enumeration<Permission> elements() {
        synchronized (this) {
            return Collections.enumeration(new ArrayList<Permission>(this.permisos));
        }
    }
}
