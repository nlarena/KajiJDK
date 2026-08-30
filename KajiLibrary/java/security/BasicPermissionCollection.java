package java.security;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;

// La coleccion que devuelve `BasicPermission.newPermissionCollection()`. Package-private: el
// contrato solo promete una `PermissionCollection`.
//
// Indexa por nombre canonico, y de ahi sale toda su razon de ser: para saber si el conjunto
// implica `"a.b.c"` no hace falta recorrerlo entero, alcanza con probar `"a.b.c"`, `"a.b.*"`,
// `"a.*"` y `"*"` — cuatro consultas de tabla en vez de N comparaciones. Con un puñado de
// permisos da igual; con cientos, no.
//
// Todos los permisos de una coleccion tienen que ser de **la misma clase**: mezclar un
// `PropertyPermission` con un `RuntimePermission` haria que el indice mintiera, porque dos
// permisos de clases distintas con el mismo nombre no se implican.
final class BasicPermissionCollection extends PermissionCollection {

    // Nombre canonico -> permiso.
    private final HashMap<String, Permission> permisos = new HashMap<String, Permission>();

    // La clase que esta coleccion acepta.
    private final Class<?> permClass;

    // Si alguno de los permisos es el comodin universal `"*"`, que implica todo de un saque.
    private boolean todos;

    BasicPermissionCollection(Class<?> permClass) {
        this.permClass = permClass;
    }

    public void add(Permission permission) {
        if (!(permission instanceof BasicPermission)) {
            throw new IllegalArgumentException("invalid permission: " + permission);
        }
        if (permission.getClass() != this.permClass) {
            throw new IllegalArgumentException("invalid permission: " + permission);
        }
        if (this.isReadOnly()) {
            throw new SecurityException(
                "attempt to add a Permission to a readonly PermissionCollection");
        }
        BasicPermission bp = (BasicPermission) permission;
        String canonico = bp.getCanonicalName();
        this.permisos.put(canonico, permission);
        if (canonico.equals("*")) {
            this.todos = true;
        }
    }

    public boolean implies(Permission permission) {
        if (!(permission instanceof BasicPermission)) {
            return false;
        }
        if (permission.getClass() != this.permClass) {
            return false;
        }
        if (this.todos) {
            return true;
        }
        BasicPermission bp = (BasicPermission) permission;
        String nombre = bp.getCanonicalName();

        // Coincidencia exacta.
        Permission exacto = this.permisos.get(nombre);
        if (exacto != null) {
            return true;
        }

        // Los comodines de cada prefijo, del mas especifico al mas general: para "a.b.c" se
        // prueban "a.b.*" y "a.*". El `lastIndexOf` recorta un segmento por vuelta.
        int corte = nombre.length() - 1;
        while (corte >= 0) {
            int punto = lastIndexOf(nombre, '.', corte);
            if (punto < 0) {
                break;
            }
            Permission comodin = this.permisos.get(nombre.substring(0, punto + 1) + "*");
            if (comodin != null) {
                return true;
            }
            corte = punto - 1;
        }
        return false;
    }

    // El ultimo `c` en `s` en la posicion `desde` o antes, o -1. Escrito a mano porque
    // `String.lastIndexOf(int, int)` no esta en esta biblioteca.
    private static int lastIndexOf(String s, char c, int desde) {
        int i = desde;
        if (i >= s.length()) {
            i = s.length() - 1;
        }
        while (i >= 0) {
            if (s.charAt(i) == c) {
                return i;
            }
            i = i - 1;
        }
        return -1;
    }

    public Enumeration<Permission> elements() {
        return new PermisoEnum(this.permisos.keySet().iterator(), this.permisos);
    }
}

// La enumeracion sobre los permisos de una BasicPermissionCollection.
final class PermisoEnum implements Enumeration<Permission> {

    private final Iterator<String> claves;
    private final HashMap<String, Permission> permisos;

    PermisoEnum(Iterator<String> claves, HashMap<String, Permission> permisos) {
        this.claves = claves;
        this.permisos = permisos;
    }

    public boolean hasMoreElements() {
        return this.claves.hasNext();
    }

    public Permission nextElement() {
        if (!this.claves.hasNext()) {
            throw new NoSuchElementException();
        }
        return this.permisos.get(this.claves.next());
    }
}
