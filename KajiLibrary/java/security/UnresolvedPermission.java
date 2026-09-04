package java.security;

import java.io.Serializable;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.NoSuchElementException;

// El marcador de un permiso que la politica menciona pero cuya clase todavia no se pudo cargar.
//
// ===============================================================================================
// POR QUE ESTO EXISTE
// ===============================================================================================
//
// La politica se lee al arrancar, cuando la mitad de las clases de permiso todavia no estan: las
// que trae una aplicacion en su propio jar no se pueden cargar antes que la aplicacion. Habria dos
// salidas malas —fallar al leer la politica, o descartar la linea— y una buena: guardar los
// **strings** y resolverlos cuando la clase aparezca. Esta clase es esa guardada.
//
// ===============================================================================================
// `implies` DEVUELVE SIEMPRE `false`, Y ESO ES LO CORRECTO
// ===============================================================================================
//
// Un permiso sin resolver no concede nada. No es una limitacion de esta implementacion: un
// `UnresolvedPermission` **no sabe** que significa el permiso que representa, porque el que lo
// sabe es el `implies` de la clase que todavia no se cargo. Contestar cualquier cosa distinta de
// `false` seria conceder por adelantado un permiso cuya semantica se desconoce.
//
// A KajiLibrary subset: falta la resolucion propiamente dicha —el metodo `resolve` es
// package-private en el JDK y lo llama el cargador de politicas, que aca no existe— porque
// construir el permiso real por reflexion no tiene a quien servirle mientras no haya una `Policy`
// instalable. Lo que si esta es todo lo que se necesita para transportar la informacion hasta que
// alguien pueda resolverla.
public final class UnresolvedPermission extends Permission implements Serializable {

    private final String type;
    private final String name;
    private final String actions;
    private final Certificate[] certs;

    // `type` es el nombre de la clase de permiso; `name` y `actions` son sus argumentos tal como
    // aparecian en la politica. `certs` son los certificados con los que la clase de permiso tiene
    // que estar firmada para que la resolucion sea aceptada — sin esa condicion, cualquiera que
    // pueda poner una clase con ese nombre en el classpath define que significa el permiso.
    public UnresolvedPermission(String type, String name, String actions, Certificate[] certs) {
        super(type);
        if (type == null) {
            throw new NullPointerException("type can't be null");
        }
        this.type = type;
        this.name = name;
        this.actions = actions;
        this.certs = certs == null ? null : copiar(certs);
    }

    private static Certificate[] copiar(Certificate[] a) {
        Certificate[] c = new Certificate[a.length];
        System.arraycopy(a, 0, c, 0, a.length);
        return c;
    }

    // Siempre `false`. Ver la cabecera.
    @Override
    public boolean implies(Permission p) {
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof UnresolvedPermission)) {
            return false;
        }
        UnresolvedPermission that = (UnresolvedPermission) obj;
        if (!this.type.equals(that.type)) {
            return false;
        }
        if (!iguales(this.name, that.name) || !iguales(this.actions, that.actions)) {
            return false;
        }
        // Los certificados se comparan como conjunto en las dos direcciones: el orden en que la
        // politica los listo no cambia la condicion que expresan.
        return contieneTodos(this.certs, that.certs) && contieneTodos(that.certs, this.certs);
    }

    private static boolean iguales(String a, String b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }

    private static boolean contieneTodos(Certificate[] conjunto, Certificate[] buscados) {
        if (buscados == null || buscados.length == 0) {
            return true;
        }
        if (conjunto == null) {
            return false;
        }
        int i = 0;
        while (i < buscados.length) {
            boolean hallado = false;
            int j = 0;
            while (j < conjunto.length) {
                if (buscados[i].equals(conjunto[j])) {
                    hallado = true;
                    j = conjunto.length;
                } else {
                    j = j + 1;
                }
            }
            if (!hallado) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = this.type.hashCode();
        if (this.name != null) {
            hash = hash ^ this.name.hashCode();
        }
        if (this.actions != null) {
            hash = hash ^ this.actions.hashCode();
        }
        return hash;
    }

    // "" — las acciones del permiso sin resolver estan en `getUnresolvedActions()`, no aca.
    //
    // La distincion no es una formalidad: `getActions()` es lo que devuelve **este** permiso, y
    // este permiso no tiene acciones porque no permite nada.
    @Override
    public String getActions() {
        return "";
    }

    public String getUnresolvedType() {
        return this.type;
    }

    public String getUnresolvedName() {
        return this.name;
    }

    public String getUnresolvedActions() {
        return this.actions;
    }

    public Certificate[] getUnresolvedCerts() {
        return this.certs == null ? null : copiar(this.certs);
    }

    @Override
    public String toString() {
        return "(unresolved " + this.type + " " + this.name + " " + this.actions + ")";
    }

    @Override
    public PermissionCollection newPermissionCollection() {
        return new ColeccionSinResolver();
    }
}

// La coleccion de permisos sin resolver.
//
// Su `implies` devuelve `false` sin mirar nada, por la misma razon que el de cada elemento:
// ninguno de ellos concede nada todavia. Guarda igual, porque el sentido de la coleccion es tener
// donde ir a buscar cuando las clases aparezcan.
final class ColeccionSinResolver extends PermissionCollection {

    private final ArrayList<Permission> permisos = new ArrayList<Permission>();

    @Override
    public void add(Permission permission) {
        if (!(permission instanceof UnresolvedPermission)) {
            throw new IllegalArgumentException("invalid permission: " + permission);
        }
        if (this.isReadOnly()) {
            throw new SecurityException(
                "attempt to add a Permission to a readonly PermissionCollection");
        }
        this.permisos.add(permission);
    }

    @Override
    public boolean implies(Permission permission) {
        return false;
    }

    @Override
    public Enumeration<Permission> elements() {
        return new EnumSinResolver(this.permisos);
    }
}

final class EnumSinResolver implements Enumeration<Permission> {

    private final ArrayList<Permission> lista;
    private int cursor;

    EnumSinResolver(ArrayList<Permission> lista) {
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
