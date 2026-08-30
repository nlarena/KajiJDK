package java.util;

import java.security.BasicPermission;
import java.security.Permission;
import java.security.PermissionCollection;

// El permiso de leer o escribir una propiedad del sistema.
//
// Hereda de `BasicPermission` los nombres jerarquicos con comodin —`"java.*"` cubre `"java.home"`
// y `"java.version"`— y le agrega lo unico que `BasicPermission` no tiene: **acciones**. Un
// permiso de propiedad no es solo "sobre cual", es "para que".
//
// Las acciones son `read` y `write`, separadas por coma, sin distinguir mayusculas y en cualquier
// orden. `getActions()` las devuelve **canonicas** —siempre `"read,write"` en ese orden— porque
// dos permisos que dicen lo mismo tienen que ser iguales y tener el mismo hash; si el texto
// original sobreviviera, `"write,read"` y `"read,write"` serian permisos distintos que implican
// exactamente lo mismo.
//
// Nota sobre el estado del modelo: desde JDK 24 el SecurityManager esta permanentemente
// deshabilitado, asi que esta clase ya no gobierna el acceso a `System.getProperty`. Se
// implementa porque es contrato.
public final class PropertyPermission extends BasicPermission {

    private static final int LEER = 1;
    private static final int ESCRIBIR = 2;

    // Las acciones, como bits. Es la forma en que `implies` puede preguntar "¿cubre todo lo que
    // hace falta?" con un `and`, en vez de comparar cadenas.
    private final int mask;

    // Un permiso sobre `name` con las acciones dadas.
    public PropertyPermission(String name, String actions) {
        super(name);
        this.mask = parsear(actions);
    }

    // Convierte "read", "write", "read,write" —en cualquier orden y capitalizacion— a bits.
    //
    // Una accion desconocida es IllegalArgumentException y no se ignora en silencio: un typo en
    // una politica de seguridad que se traga sin decir nada es un agujero, no una molestia.
    private static int parsear(String actions) {
        if (actions == null) {
            throw new NullPointerException("actions can't be null");
        }
        int m = 0;
        int i = 0;
        int n = actions.length();
        while (i < n) {
            // Saltear blancos y comas.
            while (i < n && (actions.charAt(i) == ' ' || actions.charAt(i) == ','
                    || actions.charAt(i) == '\t' || actions.charAt(i) == '\n'
                    || actions.charAt(i) == '\r' || actions.charAt(i) == '\f')) {
                i = i + 1;
            }
            if (i >= n) {
                break;
            }
            int inicio = i;
            while (i < n && actions.charAt(i) != ',') {
                i = i + 1;
            }
            String palabra = recortar(actions.substring(inicio, i));
            if (palabra.equalsIgnoreCase("read")) {
                m = m | LEER;
            } else if (palabra.equalsIgnoreCase("write")) {
                m = m | ESCRIBIR;
            } else if (palabra.length() > 0) {
                throw new IllegalArgumentException("invalid actions: " + actions);
            }
        }
        if (m == 0) {
            throw new IllegalArgumentException("invalid actions: " + actions);
        }
        return m;
    }

    private static String recortar(String s) {
        int a = 0;
        int b = s.length();
        while (a < b && esBlanco(s.charAt(a))) {
            a = a + 1;
        }
        while (b > a && esBlanco(s.charAt(b - 1))) {
            b = b - 1;
        }
        return s.substring(a, b);
    }

    private static boolean esBlanco(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f';
    }

    // Si este permiso implica al otro: el nombre tiene que cubrirlo **y** las acciones tambien.
    //
    // Las dos condiciones son necesarias y ninguna alcanza sola: `("java.*", "read")` no implica
    // `("java.home", "write")` aunque el nombre le quede grande.
    public boolean implies(Permission p) {
        if (!(p instanceof PropertyPermission)) {
            return false;
        }
        PropertyPermission that = (PropertyPermission) p;
        if ((this.mask & that.mask) != that.mask) {
            return false;
        }
        return super.implies(that);
    }

    // Igualdad por nombre y acciones. Dos permisos con el mismo nombre y distintas acciones son
    // distintos, aunque uno implique al otro.
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof PropertyPermission)) {
            return false;
        }
        PropertyPermission that = (PropertyPermission) obj;
        return this.mask == that.mask && this.getName().equals(that.getName());
    }

    public int hashCode() {
        return this.getName().hashCode();
    }

    // Las acciones en forma canonica: "read", "write" o "read,write".
    public String getActions() {
        if (this.mask == (LEER | ESCRIBIR)) {
            return "read,write";
        }
        if (this.mask == LEER) {
            return "read";
        }
        return "write";
    }

    // Una coleccion que acumula las acciones de los permisos que cubren un nombre.
    public PermissionCollection newPermissionCollection() {
        return new PropertyPermissionCollection();
    }
}

// La coleccion de PropertyPermission.
//
// No alcanza con preguntarle a cada permiso de a uno: tener `("java.*", "read")` y
// `("java.home", "write")` **si** implica `("java.home", "read,write")`, y ningun permiso solo lo
// implica. Hay que acumular las acciones de todos los que cubren el nombre y recien despues
// comparar. Es la razon por la que `PermissionCollection.implies` existe como operacion propia y
// no como un bucle sobre `Permission.implies`.
final class PropertyPermissionCollection extends PermissionCollection {

    private final ArrayList<PropertyPermission> permisos = new ArrayList<PropertyPermission>();

    public void add(Permission permission) {
        if (!(permission instanceof PropertyPermission)) {
            throw new IllegalArgumentException("invalid permission: " + permission);
        }
        if (this.isReadOnly()) {
            throw new SecurityException(
                "attempt to add a Permission to a readonly PermissionCollection");
        }
        this.permisos.add((PropertyPermission) permission);
    }

    public boolean implies(Permission permission) {
        if (!(permission instanceof PropertyPermission)) {
            return false;
        }
        PropertyPermission pedido = (PropertyPermission) permission;
        // Se acumulan las acciones de todos los que cubren el nombre; alcanza con que la union
        // cubra lo pedido.
        int acumulado = 0;
        int i = 0;
        while (i < this.permisos.size()) {
            PropertyPermission tengo = this.permisos.get(i);
            if (cubreNombre(tengo.getName(), pedido.getName())) {
                // Un permiso con las mismas acciones y ese nombre: se le pregunta a el, que ya
                // sabe comparar mascaras.
                if (tengo.implies(new PropertyPermission(pedido.getName(), tengo.getActions()))) {
                    acumulado = acumulado | mascara(tengo.getActions());
                }
            }
            i = i + 1;
        }
        return (acumulado & mascara(pedido.getActions())) == mascara(pedido.getActions());
    }

    // Si `tengo` cubre a `pedido` como nombre, con las mismas reglas de comodin que
    // BasicPermission. Se reimplementa aca porque `getCanonicalName()` es package-private de
    // `java.security` y desde `java.util` no se ve.
    private static boolean cubreNombre(String tengo, String pedido) {
        if (tengo.equals("*")) {
            return true;
        }
        if (tengo.endsWith(".*")) {
            String prefijo = tengo.substring(0, tengo.length() - 1);
            return pedido.length() > prefijo.length() && pedido.startsWith(prefijo);
        }
        return tengo.equals(pedido);
    }

    private static int mascara(String actions) {
        int m = 0;
        if (actions.equals("read") || actions.equals("read,write")) {
            m = m | 1;
        }
        if (actions.equals("write") || actions.equals("read,write")) {
            m = m | 2;
        }
        return m;
    }

    public Enumeration<Permission> elements() {
        ArrayList<Permission> copia = new ArrayList<Permission>();
        int i = 0;
        while (i < this.permisos.size()) {
            copia.add(this.permisos.get(i));
            i = i + 1;
        }
        return new PropPermEnum(copia);
    }
}

// Enumeracion sobre los permisos de una PropertyPermissionCollection.
final class PropPermEnum implements Enumeration<Permission> {

    private final ArrayList<Permission> lista;
    private int cursor;

    PropPermEnum(ArrayList<Permission> lista) {
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
