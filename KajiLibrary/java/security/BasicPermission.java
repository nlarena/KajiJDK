package java.security;

import java.io.Serializable;

// El permiso de nombre jerarquico con comodin, que es la forma que usan casi todos.
//
// Un nombre es una cadena separada por puntos —`java.home`, `os.name`— y `*` al final de un
// segmento significa "y todo lo que cuelgue". Las tres formas:
//
//   "*"            implica todo
//   "java.*"       implica "java.home", "java.version", "java.a.b" — pero NO "java" a secas
//   "java.home"    implica solo a si mismo
//
// El detalle de que `"java.*"` **no** implique `"java"` es del contrato y no una arbitrariedad: el
// comodin reemplaza a un segmento que existe, y `"java"` no tiene ese segmento. Un permiso que
// diga "todo lo que hay debajo" no deberia dar acceso al nodo de arriba.
//
// No tiene acciones: `getActions()` devuelve "". Una subclase que las necesite —como
// `PropertyPermission`, con read/write— las agrega ella.
public abstract class BasicPermission extends Permission implements Serializable {

    // El nombre sin el `*` final si lo tenia; "" para el comodin universal.
    private transient String path;

    // Si el nombre terminaba en un `*` que cuenta como comodin.
    private transient boolean wildcard;

    // Si el nombre es el `"exitVM"` pelado de antes de 1.6. Ver `init`.
    private transient boolean exitVM;

    // Un permiso con el nombre dado.
    public BasicPermission(String name) {
        super(name);
        this.init(name);
    }

    // Un permiso con el nombre dado. `actions` se ignora: esta clase no las usa, y el constructor
    // existe para que las subclases puedan encadenar y para deserializar.
    public BasicPermission(String name, String actions) {
        super(name);
        this.init(name);
    }

    // Parte el nombre en camino y comodin.
    //
    // El `*` solo cuenta como comodin si es todo el nombre o viene precedido por un punto:
    // `"a.b*"` es un nombre literal que termina en asterisco, no un comodin sobre `"a.b"`.
    private void init(String name) {
        if (name == null) {
            throw new NullPointerException("name can't be null");
        }
        int len = name.length();
        if (len == 0) {
            throw new IllegalArgumentException("name can't be empty");
        }
        char last = name.charAt(len - 1);
        if (last == '*' && (len == 1 || name.charAt(len - 2) == '.')) {
            this.wildcard = true;
            if (len == 1) {
                this.path = "";
            } else {
                this.path = name.substring(0, len - 1);
            }
        } else if (name.equals("exitVM")) {
            // La unica excepcion a la regla, y viene de una compatibilidad vieja: hasta 1.6 el
            // permiso para terminar la VM se llamaba `"exitVM"` a secas, y despues paso a ser
            // `"exitVM.<codigo>"` con `"exitVM.*"` para cualquiera. Los dos nombres tienen que
            // seguir significando lo mismo, asi que el viejo se parsea como si fuera el comodin:
            // sin esto, un `"exitVM.*"` no implicaria a un `"exitVM"` y una policy escrita antes
            // de 1.6 dejaria de valer.
            //
            // Vive aca y no en `RuntimePermission` —que es la unica clase donde el nombre
            // aparece— porque el JDK lo puso aca, y moverlo cambiaria a que permiso se aplica.
            this.wildcard = true;
            this.path = "exitVM.";
            this.exitVM = true;
        } else {
            this.path = name;
        }
    }

    // Si este permiso implica al otro.
    //
    // Primero la clase: dos permisos de clases distintas nunca se implican, aunque el nombre
    // coincida. Un `PropertyPermission("x")` no da un `RuntimePermission("x")`.
    public boolean implies(Permission p) {
        if (p == null || p.getClass() != this.getClass()) {
            return false;
        }
        BasicPermission that = (BasicPermission) p;
        if (this.wildcard) {
            if (that.wildcard) {
                // "a.*" implica "a.b.*"
                return that.path.startsWith(this.path);
            }
            // "a.*" implica "a.b" pero no "a"
            return that.path.length() > this.path.length() && that.path.startsWith(this.path);
        }
        if (that.wildcard) {
            // un nombre concreto nunca implica un comodin
            return false;
        }
        return this.path.equals(that.path);
    }

    // Igualdad por clase y nombre canonico.
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        BasicPermission bp = (BasicPermission) obj;
        return this.getName().equals(bp.getName());
    }

    public int hashCode() {
        return this.getName().hashCode();
    }

    // "" — esta clase no tiene acciones.
    public String getActions() {
        return "";
    }

    // Una coleccion que sabe resolver `implies` sobre nombres jerarquicos sin recorrer todo.
    public PermissionCollection newPermissionCollection() {
        return new BasicPermissionCollection(this.getClass());
    }

    // El nombre tal como quedo tras parsear el comodin. Package-private, como en el JDK.
    //
    // Es el nombre con el que la coleccion indexa, y por eso el `"exitVM"` viejo tiene que
    // canonizarse como `"exitVM.*"`: los dos nombres son el mismo permiso y deben caer en la misma
    // entrada.
    final String getCanonicalName() {
        if (this.exitVM) {
            return "exitVM.*";
        }
        if (this.wildcard) {
            return this.path + "*";
        }
        return this.path;
    }
}
