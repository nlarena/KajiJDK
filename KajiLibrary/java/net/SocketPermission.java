package java.net;

import java.security.Permission;
import java.security.PermissionCollection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

// "A que hosts y puertos, y para hacer que."
//
// El nombre del permiso es `host:puertos` y las acciones son un subconjunto de
// connect/listen/accept/resolve. Todo lo interesante de la clase esta en `implies`, que es lo que
// hace que un permiso escrito una vez cubra un conjunto: `*.ejemplo.org:1-1023` con "connect"
// implica `www.ejemplo.org:80` con "connect".
//
// ===========================================================================================
// LO QUE ESTA CLASE ES SIN RED
// ===========================================================================================
//
// Un permiso no conecta: describe. Toda la clase es parsing y comparacion de conjuntos --nombres
// con comodin, rangos de puertos, mascaras de bits-- y eso se computa entero aca.
//
// **La unica diferencia con el JDK, y hay que decirla:** el `implies` del JDK, cuando la
// comparacion por nombre no alcanza, **resuelve los dos hosts por DNS** y compara direcciones IP,
// para que `ejemplo.org` implique `93.184.216.34`. Eso necesita un resolutor, que en esta VM no
// existe (el porque esta en la cabecera de `InetAddress`).
//
// Aca la comparacion es **solo textual**: nombre canonico contra nombre canonico, sin distinguir
// mayusculas, mas el comodin de prefijo. Eso hace que este `implies` sea **mas restrictivo** que el
// del JDK, nunca mas permisivo -- puede decir "no" donde el JDK diria "si", y no al reves.
//
// Esa direccion del error es la que importa: un permiso que niega de mas se nota al primer uso; uno
// que otorga de mas no se nota nunca. Y como la comparacion por IP solo suma casos, ninguna
// respuesta afirmativa de aca es una respuesta que el JDK no daria.
//
// El resto --acciones, orden canonico, rangos, `equals`, `hashCode`-- es identico al JDK, y esta
// verificado contra el JDK real caso por caso.
//
// @deprecated El Security Manager quedo deprecado para remocion; estos permisos ya no se chequean.
@Deprecated
public final class SocketPermission extends Permission implements java.io.Serializable {

    private static final long serialVersionUID = -7204263841984476862L;

    private static final int CONNECT = 0x1;
    private static final int LISTEN = 0x2;
    private static final int ACCEPT = 0x4;
    private static final int RESOLVE = 0x8;

    // El host en minusculas. Si `comodin` es true, es el SUFIJO que hay que matchear (".ejemplo.org"),
    // o la cadena vacia para el `*` pelado, que matchea todo.
    private final String host;
    private final boolean comodin;
    private final int puertoMin;
    private final int puertoMax;
    private final int mask;
    private final String actions;

    /**
     * El permiso sobre {@code host} para {@code action}.
     *
     * @param host {@code hostname[:puerto|:min-max|:min-|:-max]}; vacio significa "localhost", y
     *     un nombre puede empezar con {@code *.} para cubrir un dominio entero
     * @param action lista separada por comas de connect/listen/accept/resolve, sin distinguir caja
     * @throws NullPointerException si {@code action} es null
     * @throws IllegalArgumentException si {@code action} es vacio o tiene un nombre desconocido
     */
    public SocketPermission(String host, String action) {
        // El nombre que se guarda es el YA normalizado: un host vacio significa "localhost", y
        // `getName()` tiene que devolver eso y no la cadena vacia con la que se escribio. La cuenta
        // va inline porque `super(...)` tiene que ser la primera sentencia.
        super(host == null || host.length() == 0 ? "localhost" : host);
        String h = host == null || host.length() == 0 ? "localhost" : host;
        int corte = puntoDeCorte(h);
        String nombre;
        String puertos;
        if (corte == -1) {
            nombre = h;
            puertos = null;
        } else {
            nombre = h.substring(0, corte);
            puertos = h.substring(corte + 1);
        }
        nombre = nombre.toLowerCase();
        if (nombre.equals("*")) {
            this.comodin = true;
            this.host = "";
        } else if (nombre.startsWith("*.")) {
            this.comodin = true;
            this.host = nombre.substring(1);
        } else {
            this.comodin = false;
            this.host = nombre;
        }
        int[] rango = parsearPuertos(puertos);
        this.puertoMin = rango[0];
        this.puertoMax = rango[1];
        this.mask = parsearAcciones(action);
        this.actions = armarAcciones(this.mask);
    }

    // El ':' que separa el host de los puertos. Es el ULTIMO, y no el primero, porque una direccion
    // IPv6 literal viene llena de ':' -- pero entre corchetes, asi que si hay un ']' el corte tiene
    // que buscarse despues de el.
    private static int puntoDeCorte(String h) {
        int corchete = h.lastIndexOf(']');
        return h.indexOf(':', corchete + 1) == -1 ? -1 : h.lastIndexOf(':');
    }

    // "80" -> [80,80]; "80-90" -> [80,90]; "1024-" -> [1024,65535]; "-100" -> [0,100];
    // ausente -> todo el rango, que es lo que hace que `*` con "connect" implique cualquier puerto.
    private static int[] parsearPuertos(String p) {
        if (p == null || p.length() == 0) {
            return new int[] {0, 65535};
        }
        int guion = p.indexOf('-');
        if (guion == -1) {
            int v = entero(p, -1);
            if (v < 0) {
                throw new IllegalArgumentException("invalid port range: " + p);
            }
            return new int[] {v, v};
        }
        String izq = p.substring(0, guion);
        String der = p.substring(guion + 1);
        int lo = izq.length() == 0 ? 0 : entero(izq, -1);
        int hi = der.length() == 0 ? 65535 : entero(der, -1);
        if (lo < 0 || hi < 0) {
            throw new IllegalArgumentException("invalid port range: " + p);
        }
        return new int[] {lo, hi};
    }

    private static int entero(String s, int siFalla) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return siFalla;
        }
    }

    // "resolve" se agrega solo cuando hay cualquier otra accion: conectarse a un nombre implica
    // haberlo podido resolver, asi que separarlos seria dar un permiso inutil.
    private static int parsearAcciones(String action) {
        if (action == null) {
            throw new NullPointerException("action can't be null");
        }
        if (action.length() == 0) {
            throw new IllegalArgumentException("action can't be empty");
        }
        int m = 0;
        int start = 0;
        while (start <= action.length()) {
            int coma = action.indexOf(',', start);
            String tok;
            if (coma == -1) {
                tok = action.substring(start);
                start = action.length() + 1;
            } else {
                tok = action.substring(start, coma);
                start = coma + 1;
            }
            tok = tok.trim().toLowerCase();
            if (tok.length() == 0) {
                continue;
            }
            if (tok.equals("connect")) {
                m = m | CONNECT;
            } else if (tok.equals("listen")) {
                m = m | LISTEN;
            } else if (tok.equals("accept")) {
                m = m | ACCEPT;
            } else if (tok.equals("resolve")) {
                m = m | RESOLVE;
            } else {
                throw new IllegalArgumentException("invalid permission: " + tok);
            }
        }
        if ((m & (CONNECT | LISTEN | ACCEPT)) != 0) {
            m = m | RESOLVE;
        }
        return m;
    }

    // El orden es fijo --connect, listen, accept, resolve-- y no el de escritura: asi dos permisos
    // equivalentes escritos distinto dan la misma cadena.
    private static String armarAcciones(int m) {
        StringBuilder b = new StringBuilder();
        String sep = "";
        if ((m & CONNECT) != 0) {
            b.append(sep).append("connect");
            sep = ",";
        }
        if ((m & LISTEN) != 0) {
            b.append(sep).append("listen");
            sep = ",";
        }
        if ((m & ACCEPT) != 0) {
            b.append(sep).append("accept");
            sep = ",";
        }
        if ((m & RESOLVE) != 0) {
            b.append(sep).append("resolve");
        }
        return b.toString();
    }

    /**
     * Si este permiso cubre a {@code p}.
     *
     * <p>Tres condiciones, todas necesarias: las acciones de {@code p} tienen que estar entre las
     * de este, su rango de puertos tiene que caer entero adentro de este, y su host tiene que
     * matchear.
     *
     * <p>Sobre la comparacion de hosts, ver la cabecera del archivo: es textual, sin DNS.
     */
    @Override
    public boolean implies(Permission p) {
        if (!(p instanceof SocketPermission)) {
            return false;
        }
        SocketPermission that = (SocketPermission) p;
        if ((this.mask & that.mask) != that.mask) {
            return false;
        }
        if (that.puertoMin < this.puertoMin || that.puertoMax > this.puertoMax) {
            return false;
        }
        return this.matcheaHost(that);
    }

    private boolean matcheaHost(SocketPermission that) {
        if (this.comodin) {
            // `*` pelado cubre todo; `*.dominio` cubre los subdominios, NO el dominio pelado
            // (`*.ejemplo.org` no implica `ejemplo.org`, que es lo que hace el JDK).
            return this.host.length() == 0 || that.host.endsWith(this.host);
        }
        if (that.comodin) {
            return false;
        }
        return this.host.equals(that.host);
    }

    /** Las acciones en orden canonico. */
    @Override
    public String getActions() {
        return this.actions;
    }

    /**
     * Igual host, igual rango y iguales acciones.
     *
     * <p>Comparar el host **canonizado** y no el nombre crudo es lo que hace que
     * {@code HOST.com:80} y {@code host.com:80} sean el mismo permiso.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof SocketPermission)) {
            return false;
        }
        SocketPermission that = (SocketPermission) obj;
        return this.mask == that.mask
                && this.comodin == that.comodin
                && this.puertoMin == that.puertoMin
                && this.puertoMax == that.puertoMax
                && this.host.equals(that.host);
    }

    /**
     * El hash del NOMBRE, no de las acciones.
     *
     * <p>Es lo que hace el JDK, y no es un descuido: dos permisos sobre el mismo host con acciones
     * distintas caen en el mismo balde a proposito, porque las consultas van siempre por host.
     */
    @Override
    public int hashCode() {
        return this.getName().hashCode();
    }

    /**
     * Una coleccion que sabe responder {@code implies} sobre el conjunto entero.
     *
     * <p>Hace falta una propia porque un conjunto de `SocketPermission` puede implicar algo que
     * ningun miembro implica solo: "connect" a un host mas "resolve" al mismo host se suman. La
     * coleccion junta las mascaras de los que matchean el host antes de decidir.
     */
    @Override
    public PermissionCollection newPermissionCollection() {
        return new SocketPermissionCollection();
    }

    // Package-private, como en el JDK: nadie la nombra, se la obtiene por `newPermissionCollection`.
    static final class SocketPermissionCollection extends PermissionCollection {

        private static final long serialVersionUID = 2787186408602930181L;

        private final List<Permission> perms = new ArrayList<Permission>();

        @Override
        public void add(Permission permission) {
            if (!(permission instanceof SocketPermission)) {
                throw new IllegalArgumentException("invalid permission: " + permission);
            }
            if (this.isReadOnly()) {
                throw new SecurityException(
                        "attempt to add a Permission to a readonly PermissionCollection");
            }
            synchronized (this.perms) {
                this.perms.add(0, permission);
            }
        }

        @Override
        public boolean implies(Permission permission) {
            if (!(permission instanceof SocketPermission)) {
                return false;
            }
            SocketPermission np = (SocketPermission) permission;
            int necesita = np.mask;
            int juntado = 0;
            synchronized (this.perms) {
                int i = 0;
                while (i < this.perms.size()) {
                    SocketPermission x = (SocketPermission) this.perms.get(i);
                    // Solo suman los que ya cubren el host y el rango; si no, sus acciones son
                    // sobre otra cosa y no tienen por que valer aca.
                    if ((x.mask & necesita) != 0
                            && np.puertoMin >= x.puertoMin
                            && np.puertoMax <= x.puertoMax
                            && x.matcheaHost(np)) {
                        juntado = juntado | x.mask;
                        if ((juntado & necesita) == necesita) {
                            return true;
                        }
                    }
                    i = i + 1;
                }
            }
            return false;
        }

        @Override
        public Enumeration<Permission> elements() {
            synchronized (this.perms) {
                return Collections.enumeration(new ArrayList<Permission>(this.perms));
            }
        }
    }
}
