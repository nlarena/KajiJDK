package java.nio.file.attribute;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

// Una entrada de una lista de control de acceso: a quien, que tipo, que permisos y como se hereda.
//
// **Es puro valor y por eso esta entera.** No hace falta ningun nativo para construir, comparar ni
// imprimir una `AclEntry`; lo que KajiJDK no puede es **leerla del disco ni escribirla**, y eso vive
// en `AclFileAttributeView`, que queda sin implementacion. La clase sirve igual: quien tenga ACLs de
// otra fuente --un archivo de configuracion, un protocolo-- puede modelarlas con esto.
//
// **Inmutable, y se construye con un `Builder`.** Son cuatro campos de los cuales dos son conjuntos
// opcionales: un constructor de cuatro argumentos obligaria a escribir `Collections.emptySet()` dos
// veces en el caso comun. Los conjuntos se copian al entrar --no se guarda la referencia que dio
// quien llama-- porque si no la "inmutabilidad" duraria hasta que el llamador tocara su propio set.
public final class AclEntry {

    private final AclEntryType type;
    private final UserPrincipal who;
    private final Set<AclEntryPermission> perms;
    private final Set<AclEntryFlag> flags;

    // El hash se calcula una sola vez y se guarda; 0 significa "todavia no". Que el valor legitimo 0
    // se recalcule cada vez es barato y evita un campo booleano extra.
    private volatile int hash;

    private AclEntry(AclEntryType type, UserPrincipal who, Set<AclEntryPermission> perms,
            Set<AclEntryFlag> flags) {
        this.type = type;
        this.who = who;
        this.perms = perms;
        this.flags = flags;
    }

    /** Un constructor vacio, al que hay que darle al menos tipo y principal. */
    public static Builder newBuilder() {
        Set<AclEntryPermission> p = Collections.emptySet();
        Set<AclEntryFlag> f = Collections.emptySet();
        return new Builder(null, null, p, f);
    }

    /** Un constructor precargado con los valores de `entry`, para copiar cambiando una cosa. */
    public static Builder newBuilder(AclEntry entry) {
        if (entry == null) {
            throw new NullPointerException();
        }
        return new Builder(entry.type, entry.who, entry.perms, entry.flags);
    }

    /** Si esta entrada permite, niega, audita o alarma. */
    public AclEntryType type() {
        return this.type;
    }

    /** A quien aplica. */
    public UserPrincipal principal() {
        return this.who;
    }

    /** Los permisos, en una **copia**: modificarla no toca la entrada. */
    public Set<AclEntryPermission> permissions() {
        return new HashSet<AclEntryPermission>(this.perms);
    }

    /** Las banderas de herencia, en una **copia**. */
    public Set<AclEntryFlag> flags() {
        return new HashSet<AclEntryFlag>(this.flags);
    }

    /** Igual si coinciden los cuatro campos. */
    public boolean equals(Object ob) {
        if (ob == this) {
            return true;
        }
        if (!(ob instanceof AclEntry)) {
            return false;
        }
        AclEntry other = (AclEntry) ob;
        if (this.type != other.type) {
            return false;
        }
        if (!this.who.equals(other.who)) {
            return false;
        }
        if (!this.perms.equals(other.perms)) {
            return false;
        }
        return this.flags.equals(other.flags);
    }

    private static int hashCodeOf(Set<?> s) {
        // La suma de los hashes de los elementos: no depende del orden de iteracion, que en un
        // conjunto no esta definido.
        int h = 0;
        Iterator<?> it = s.iterator();
        while (it.hasNext()) {
            Object o = it.next();
            h = h + (o == null ? 0 : o.hashCode());
        }
        return h;
    }

    public int hashCode() {
        int h = this.hash;
        if (h != 0) {
            return h;
        }
        h = this.type.hashCode();
        h = h * 127 + this.who.hashCode();
        h = h * 127 + hashCodeOf(this.perms);
        h = h * 127 + hashCodeOf(this.flags);
        this.hash = h;
        return h;
    }

    /** Algo como `usuario:READ_DATA/WRITE_DATA:FILE_INHERIT:ALLOW`. */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.who.getName());
        sb.append(':');
        Iterator<AclEntryPermission> ip = this.perms.iterator();
        while (ip.hasNext()) {
            sb.append(ip.next().name());
            sb.append('/');
        }
        if (!this.perms.isEmpty()) {
            sb.setLength(sb.length() - 1);
        }
        sb.append(':');
        Iterator<AclEntryFlag> iflag = this.flags.iterator();
        while (iflag.hasNext()) {
            sb.append(iflag.next().name());
            sb.append('/');
        }
        if (!this.flags.isEmpty()) {
            sb.setLength(sb.length() - 1);
        }
        sb.append(':');
        sb.append(this.type.name());
        return sb.toString();
    }

    /**
     * El constructor de `AclEntry`.
     *
     * <p>Los `set*` devuelven `this` para poder encadenarlos, y **mutan** el propio constructor: la
     * inmutabilidad la garantiza `build()`, que copia los conjuntos, no el `Builder`. No es
     * seguro compartir un `Builder` entre hilos.
     */
    public static final class Builder {

        private AclEntryType type;
        private UserPrincipal who;
        private Set<AclEntryPermission> perms;
        private Set<AclEntryFlag> flags;

        private Builder(AclEntryType type, UserPrincipal who, Set<AclEntryPermission> perms,
                Set<AclEntryFlag> flags) {
            this.type = type;
            this.who = who;
            this.perms = perms;
            this.flags = flags;
        }

        /**
         * Arma la entrada.
         *
         * @throws IllegalStateException si falta el tipo o el principal -- los dos unicos campos
         *     que no tienen un valor por omision razonable: un conjunto vacio de permisos significa
         *     "ninguno", pero no hay una entrada sin destinatario
         */
        public AclEntry build() {
            if (this.type == null) {
                throw new IllegalStateException("missing type component");
            }
            if (this.who == null) {
                throw new IllegalStateException("missing who component");
            }
            return new AclEntry(this.type, this.who,
                    new HashSet<AclEntryPermission>(this.perms),
                    new HashSet<AclEntryFlag>(this.flags));
        }

        /** Fija el tipo. */
        public Builder setType(AclEntryType type) {
            if (type == null) {
                throw new NullPointerException();
            }
            this.type = type;
            return this;
        }

        /** Fija a quien aplica. */
        public Builder setPrincipal(UserPrincipal who) {
            if (who == null) {
                throw new NullPointerException();
            }
            this.who = who;
            return this;
        }

        /**
         * Fija los permisos.
         *
         * <p>Se copia y se revisa elemento por elemento en vez de confiar en el tipo estatico:
         * un `Set` crudo puede traer cualquier cosa, y el `ClassCastException` aparecereria mucho
         * mas tarde, al usarlo.
         */
        public Builder setPermissions(Set<AclEntryPermission> perms) {
            if (perms.isEmpty()) {
                this.perms = Collections.emptySet();
                return this;
            }
            Set<AclEntryPermission> copia = new HashSet<AclEntryPermission>();
            Iterator<AclEntryPermission> it = perms.iterator();
            while (it.hasNext()) {
                AclEntryPermission p = it.next();
                if (p == null) {
                    throw new NullPointerException();
                }
                copia.add(p);
            }
            this.perms = copia;
            return this;
        }

        /** Fija los permisos, sueltos. */
        public Builder setPermissions(AclEntryPermission... perms) {
            Set<AclEntryPermission> copia = new HashSet<AclEntryPermission>();
            int i = 0;
            while (i < perms.length) {
                if (perms[i] == null) {
                    throw new NullPointerException();
                }
                copia.add(perms[i]);
                i = i + 1;
            }
            this.perms = copia;
            return this;
        }

        /** Fija las banderas de herencia. */
        public Builder setFlags(Set<AclEntryFlag> flags) {
            if (flags.isEmpty()) {
                this.flags = Collections.emptySet();
                return this;
            }
            Set<AclEntryFlag> copia = new HashSet<AclEntryFlag>();
            Iterator<AclEntryFlag> it = flags.iterator();
            while (it.hasNext()) {
                AclEntryFlag f = it.next();
                if (f == null) {
                    throw new NullPointerException();
                }
                copia.add(f);
            }
            this.flags = copia;
            return this;
        }

        /** Fija las banderas de herencia, sueltas. */
        public Builder setFlags(AclEntryFlag... flags) {
            Set<AclEntryFlag> copia = new HashSet<AclEntryFlag>();
            int i = 0;
            while (i < flags.length) {
                if (flags[i] == null) {
                    throw new NullPointerException();
                }
                copia.add(flags[i]);
                i = i + 1;
            }
            this.flags = copia;
            return this;
        }
    }
}
