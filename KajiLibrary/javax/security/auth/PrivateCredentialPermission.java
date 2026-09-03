package javax.security.auth;

import java.security.Permission;
import java.security.PermissionCollection;
import java.util.ArrayList;
import java.util.List;

/**
 * KajiLibrary's javax.security.auth.PrivateCredentialPermission -- permiso para leer una credencial
 * privada de un {@link Subject}.
 *
 * <p>El nombre del permiso es una gramatica chiquita y vale la pena escribirla porque el parseo es
 * casi toda la clase:
 *
 * <pre>
 *   ClaseDeCredencial ClaseDePrincipal "NombreDePrincipal" [ClaseDePrincipal "NombreDePrincipal"]*
 * </pre>
 *
 * <p>Por ejemplo {@code "java.lang.String javax.security.auth.x500.X500Principal \"cn=juan\""}. Los
 * nombres de principal van entre comillas <b>siempre</b>, incluso el comodin, y tiene que haber al
 * menos un par: una clase de credencial suelta no dice de quien es la credencial, y ese es el dato
 * que decide si se puede leer o no.
 *
 * <h2>Los comodines y la regla que los ata</h2>
 *
 * <p>Tanto la clase de credencial como cada par de principal aceptan {@code *}. Pero hay una
 * combinacion prohibida: una <b>clase</b> de principal comodin con un <b>nombre</b> concreto. Tiene
 * sentido -- "cualquier clase de principal que se llame juan" es una condicion que no se puede
 * evaluar, porque el nombre solo significa algo dentro de un espacio de nombres. El JDK la rechaza
 * en el constructor y aca tambien.
 *
 * <h2>Como se lee implies</h2>
 *
 * <p>{@code a.implies(b)} pregunta si tener {@code a} alcanza para lo que pide {@code b}, y la
 * direccion sorprende: un permiso con <b>menos</b> principals implica a uno con mas, no al reves.
 * La razon es que cada principal es una condicion adicional sobre el mismo Subject -- pedir
 * "credencial de un Subject que es P1 <i>y ademas</i> P2" es pedir menos que "credencial de un
 * Subject que es P1" --, asi que quien tiene el permiso mas laxo tiene tambien el mas estricto.
 *
 * <p>Nota sobre para que sirve hoy: igual que {@link AuthPermission}, ningun chequeo de la
 * biblioteca lo consulta porque el gestor de seguridad ya no se puede habilitar. La clase existe
 * porque su forma -- parseo, {@code implies}, {@code equals} -- es parte del API.
 */
public final class PrivateCredentialPermission extends Permission {

    private static final long serialVersionUID = 5284372143517237068L;

    private static final String WILDCARD = "*";

    private final String credentialClass;
    // Pares (clase, nombre), en el orden en el que aparecieron. Se guarda el orden y no un conjunto
    // porque `getPrincipals()` y `getName()` lo devuelven, aunque `implies` no lo mire.
    private final String[][] principals;
    private final String actions;

    public PrivateCredentialPermission(String name, String actions) {
        super(name);
        // "read" es la unica accion que existe. Cualquier otra cosa --null incluido-- se rechaza en
        // vez de ignorarse: un permiso construido con "write" leeria como si diera permiso de leer.
        if (actions == null || !actions.equalsIgnoreCase("read")) {
            throw new IllegalArgumentException("actions can only be 'read'");
        }
        this.actions = "read";
        if (name == null) {
            throw new NullPointerException("invalid null name");
        }
        if (name.trim().length() == 0) {
            throw new IllegalArgumentException("invalid empty name");
        }
        List<String[]> pairs = new ArrayList<String[]>();
        this.credentialClass = parse(name, pairs);
        this.principals = pairs.toArray(new String[pairs.size()][]);
    }

    // Devuelve la clase de credencial y llena `pairs`. El texto se recorre a mano y no con un
    // separador de espacios porque los nombres van entre comillas y pueden llevar espacios adentro
    // --`"cn=Juan Perez"` es un nombre solo--.
    private static String parse(String name, List<String[]> pairs) {
        int i = 0;
        int n = name.length();
        String cls = null;
        String principalClass = null;
        while (i < n) {
            while (i < n && isBlank(name.charAt(i))) {
                i = i + 1;
            }
            if (i >= n) {
                break;
            }
            if (name.charAt(i) == '"') {
                int close = name.indexOf('"', i + 1);
                if (close < 0) {
                    throw invalid(name, "Principal Name must be surrounded by quotes");
                }
                if (principalClass == null) {
                    throw invalid(name,
                        "Credential Class not followed by a Principal Class and Name");
                }
                String principalName = name.substring(i + 1, close);
                // Ver la nota de la clase: una clase comodin con un nombre concreto no se puede
                // evaluar, asi que se rechaza en vez de aceptarse y no cumplirse nunca.
                if (WILDCARD.equals(principalClass) && !WILDCARD.equals(principalName)) {
                    throw new IllegalArgumentException("PrivateCredentialPermission Principal "
                        + "Class can not be a wildcard (*) value if Principal Name is not a "
                        + "wildcard (*) value");
                }
                pairs.add(new String[] {principalClass, principalName});
                principalClass = null;
                i = close + 1;
                continue;
            }
            int from = i;
            while (i < n && !isBlank(name.charAt(i))) {
                i = i + 1;
            }
            String word = name.substring(from, i);
            if (cls == null) {
                cls = word;
            } else if (principalClass == null) {
                principalClass = word;
            } else {
                // Dos clases seguidas sin nombre en el medio: falta el par.
                throw invalid(name, "Principal Name must be surrounded by quotes");
            }
        }
        if (cls == null) {
            throw new IllegalArgumentException("invalid empty name");
        }
        if (principalClass != null) {
            throw invalid(name, "Principal Name must be surrounded by quotes");
        }
        if (pairs.isEmpty()) {
            throw invalid(name, "Credential Class not followed by a Principal Class and Name");
        }
        return cls;
    }

    private static boolean isBlank(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f';
    }

    private static IllegalArgumentException invalid(String name, String que) {
        return new IllegalArgumentException("permission name [" + name + "] syntax invalid: " + que);
    }

    /** La clase de la credencial, o {@code "*"}. */
    public String getCredentialClass() {
        return this.credentialClass;
    }

    /**
     * Los pares (clase, nombre) del principal. Copia: tocar lo que sale de aca no cambia el permiso.
     */
    public String[][] getPrincipals() {
        String[][] copyOf = new String[this.principals.length][];
        int i = 0;
        while (i < this.principals.length) {
            copyOf[i] = new String[] {this.principals[i][0], this.principals[i][1]};
            i = i + 1;
        }
        return copyOf;
    }

    /** Siempre {@code "read"}. */
    @Override
    public String getActions() {
        return this.actions;
    }

    /**
     * Si tener este permiso alcanza para lo que pide {@code p}. Ver la nota de la clase sobre la
     * direccion, que es al reves de lo que uno espera.
     */
    @Override
    public boolean implies(Permission p) {
        if (!(p instanceof PrivateCredentialPermission)) {
            return false;
        }
        PrivateCredentialPermission other = (PrivateCredentialPermission) p;
        if (!WILDCARD.equals(this.credentialClass)
                && !this.credentialClass.equals(other.credentialClass)) {
            return false;
        }
        // Cada condicion de este permiso tiene que estar cubierta por alguna del otro. Si a este no
        // le queda ninguna condicion sin cubrir, entonces el otro pide al menos lo mismo.
        int i = 0;
        while (i < this.principals.length) {
            boolean cubierta = false;
            int j = 0;
            while (j < other.principals.length) {
                if (covers(this.principals[i], other.principals[j])) {
                    cubierta = true;
                    break;
                }
                j = j + 1;
            }
            if (!cubierta) {
                return false;
            }
            i = i + 1;
        }
        return other.principals.length > 0;
    }

    private static boolean covers(String[] mio, String[] suyo) {
        if (!WILDCARD.equals(mio[0]) && !mio[0].equals(suyo[0])) {
            return false;
        }
        return WILDCARD.equals(mio[1]) || mio[1].equals(suyo[1]);
    }

    /**
     * Dos permisos son el mismo si cada uno implica al otro.
     *
     * <p>Se define asi y no comparando los nombres porque el orden de los pares no significa nada:
     * {@code "C P1 \"a\" P2 \"b\""} y {@code "C P2 \"b\" P1 \"a\""} son el mismo permiso escrito de
     * dos formas.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof PrivateCredentialPermission)) {
            return false;
        }
        PrivateCredentialPermission other = (PrivateCredentialPermission) obj;
        return this.implies(other) && other.implies(this);
    }

    /**
     * Solo la clase de credencial.
     *
     * <p>Es un hash pobre a proposito: tiene que ser consistente con un {@code equals} que ignora el
     * orden de los pares y trata los comodines, y la clase de credencial es lo unico que dos
     * permisos iguales comparten siempre.
     */
    @Override
    public int hashCode() {
        return this.credentialClass.hashCode();
    }

    /**
     * Null, igual que en el JDK: no hay una coleccion especializada para este permiso, asi que quien
     * lo guarde tiene que usar la generica.
     */
    @Override
    public PermissionCollection newPermissionCollection() {
        return null;
    }
}
