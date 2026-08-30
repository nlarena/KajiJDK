package java.security;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;

// Una coleccion **heterogenea** de permisos: la de arriba de todo.
//
// Las `PermissionCollection` de cada clase son homogeneas a proposito, porque su `implies` sabe
// de la forma de esa clase. Esta las junta: guarda una coleccion por clase de permiso y le
// delega. `implies` busca la coleccion de la clase del permiso pedido y le pregunta a ella — no
// recorre las demas, porque dos permisos de clases distintas nunca se implican.
//
// El `AllPermission` es la excepcion y por eso se guarda aparte: si hay alguno, `implies`
// devuelve true sin consultar nada mas.
public final class Permissions extends PermissionCollection implements Serializable {

    // Clase de permiso -> su coleccion homogenea.
    private final HashMap<Class<?>, PermissionCollection> porClase =
        new HashMap<Class<?>, PermissionCollection>();

    // La coleccion de AllPermission, si se agrego alguno. Package-private como en el JDK.
    PermissionCollection allPermission;

    public Permissions() {
    }

    public void add(Permission permission) {
        if (this.isReadOnly()) {
            throw new SecurityException(
                "attempt to add a Permission to a readonly Permissions object");
        }
        PermissionCollection pc = this.coleccionPara(permission);
        pc.add(permission);
        if (permission instanceof AllPermission) {
            this.allPermission = pc;
        }
    }

    public boolean implies(Permission permission) {
        // Un AllPermission guardado corta la busqueda.
        if (this.allPermission != null && this.allPermission.implies(permission)) {
            return true;
        }
        PermissionCollection pc = this.porClase.get(permission.getClass());
        if (pc == null) {
            return false;
        }
        return pc.implies(permission);
    }

    public Enumeration<Permission> elements() {
        java.util.ArrayList<Permission> todos = new java.util.ArrayList<Permission>();
        Iterator<Class<?>> clases = this.porClase.keySet().iterator();
        while (clases.hasNext()) {
            PermissionCollection pc = this.porClase.get(clases.next());
            Enumeration<Permission> e = pc.elements();
            while (e.hasMoreElements()) {
                todos.add(e.nextElement());
            }
        }
        return new ListaPermEnum(todos);
    }

    // La coleccion de la clase del permiso, creandola si hace falta.
    //
    // Se le pide a la propia clase de permiso (`newPermissionCollection`) porque solo ella sabe
    // si tiene una implementacion mas rapida. Si dice `null` —no tengo nada mejor— se usa una
    // generica que compara de a uno.
    private PermissionCollection coleccionPara(Permission p) {
        Class<?> c = p.getClass();
        PermissionCollection pc = this.porClase.get(c);
        if (pc != null) {
            return pc;
        }
        pc = p.newPermissionCollection();
        if (pc == null) {
            pc = new PermisosGenericos();
        }
        this.porClase.put(c, pc);
        return pc;
    }
}

// La coleccion de ultimo recurso: guarda los permisos en una lista y pregunta de a uno.
//
// Es correcta para cualquier clase de permiso, y por eso sirve de respaldo; lo que no es, es
// rapida. Una clase que se use mucho deberia devolver la suya en `newPermissionCollection`.
final class PermisosGenericos extends PermissionCollection {

    private final java.util.ArrayList<Permission> permisos = new java.util.ArrayList<Permission>();

    public void add(Permission permission) {
        if (this.isReadOnly()) {
            throw new SecurityException(
                "attempt to add a Permission to a readonly PermissionCollection");
        }
        this.permisos.add(permission);
    }

    public boolean implies(Permission permission) {
        int i = 0;
        while (i < this.permisos.size()) {
            if (this.permisos.get(i).implies(permission)) {
                return true;
            }
            i = i + 1;
        }
        return false;
    }

    public Enumeration<Permission> elements() {
        return new ListaPermEnum(this.permisos);
    }
}

// Enumeracion sobre una lista de permisos.
final class ListaPermEnum implements Enumeration<Permission> {

    private final java.util.List<Permission> lista;
    private int cursor;

    ListaPermEnum(java.util.List<Permission> lista) {
        this.lista = lista;
    }

    public boolean hasMoreElements() {
        return this.cursor < this.lista.size();
    }

    public Permission nextElement() {
        if (this.cursor >= this.lista.size()) {
            throw new NoSuchElementException();
        }
        Permission p = this.lista.get(this.cursor);
        this.cursor = this.cursor + 1;
        return p;
    }
}
