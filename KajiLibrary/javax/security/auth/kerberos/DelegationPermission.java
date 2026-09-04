package javax.security.auth.kerberos;

import java.io.Serializable;
import java.security.BasicPermission;
import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Objects;

/**
 * KajiLibrary's javax.security.auth.kerberos.DelegationPermission -- permiso para delegar
 * credenciales.
 *
 * <p>Es el permiso de que un servicio use el ticket de un cliente para hablar con <b>otro</b>
 * servicio en su nombre. El nombre lleva los dos principales entre comillas, separados por espacio:
 * {@code "\"host/web@REINO\" \"krbtgt/REINO@REINO\""} es "el servicio web puede pedir tickets
 * en nombre del cliente". El primero es el subordinado --quien delega-- y el segundo el destino.
 *
 * <p>El formato es estricto: las dos partes van entre comillas, con al menos un espacio entre ellas y
 * nada alrededor. Cada error tiene su mensaje --{@code improperly quoted}, {@code not enough input},
 * {@code extra input}--, porque el nombre viene de un archivo de politica escrito a mano y el que lo
 * escribio tiene que poder ver que le falto.
 *
 * <p>No hay acciones. Dos permisos son iguales si sus dos principales coinciden; el espacio entre
 * las comillas no cuenta.
 *
 * @deprecated el JDK lo marca para remocion junto con el gestor de seguridad; sigue aca porque el
 *     codigo que lo instancia tiene que poder compilar y correr
 */
@Deprecated(since = "17", forRemoval = true)
public final class DelegationPermission extends BasicPermission implements Serializable {

    private static final long serialVersionUID = 883133252142523922L;

    /** Quien delega. */
    private transient String subordinate;

    /** A quien. */
    private transient String service;

    /**
     * Con ese nombre. Ver la nota de la clase sobre el formato.
     *
     * @throws NullPointerException si es null
     * @throws IllegalArgumentException si esta vacio o mal formado
     */
    public DelegationPermission(String principals) {
        super(principals);
        init(principals);
    }

    /** Idem; las acciones se ignoran. */
    public DelegationPermission(String principals, String actions) {
        super(principals, actions);
        init(principals);
    }

    /** Separa los dos principales. Ver la nota de la clase. */
    private void init(String target) {
        if (target == null) {
            throw new NullPointerException("name can't be null");
        }
        if (target.isEmpty()) {
            throw new IllegalArgumentException("name can't be empty");
        }
        if (!target.startsWith("\"")) {
            throw new IllegalArgumentException("Illegal input [" + target + "]: improperly quoted");
        }
        int subordinateEnd = target.indexOf('"', 1);
        if (subordinateEnd < 0) {
            throw new IllegalArgumentException("Illegal input [" + target + "]: improperly quoted");
        }
        String rest = target.substring(subordinateEnd + 1);
        if (rest.isEmpty()) {
            throw new IllegalArgumentException("Illegal input [" + target + "]: not enough input");
        }
        int at = 0;
        while (at < rest.length() && Character.isWhitespace(rest.charAt(at))) {
            at = at + 1;
        }
        if (at == 0) {
            throw new IllegalArgumentException("Illegal input [" + target
                + "]: improperly separated");
        }
        rest = rest.substring(at);
        if (rest.isEmpty()) {
            throw new IllegalArgumentException("Illegal input [" + target + "]: not enough input");
        }
        if (!rest.startsWith("\"")) {
            throw new IllegalArgumentException("Illegal input [" + target + "]: improperly quoted");
        }
        int serviceEnd = rest.indexOf('"', 1);
        if (serviceEnd < 0) {
            throw new IllegalArgumentException("Illegal input [" + target + "]: improperly quoted");
        }
        if (serviceEnd + 1 != rest.length()) {
            throw new IllegalArgumentException("Illegal input [" + target + "]: extra input");
        }
        this.subordinate = target.substring(1, subordinateEnd);
        this.service = rest.substring(1, serviceEnd);
        if (this.subordinate.isEmpty()) {
            throw new IllegalArgumentException("Illegal input [" + target
                + "]: bad subordinate name");
        }
        if (this.service.isEmpty()) {
            throw new IllegalArgumentException("Illegal input [" + target + "]: bad service name");
        }
    }

    /** Si es el mismo par de principales. No hay comodines. */
    @Override
    public boolean implies(Permission p) {
        if (!(p instanceof DelegationPermission)) {
            return false;
        }
        DelegationPermission that = (DelegationPermission) p;
        return this.subordinate.equals(that.subordinate) && this.service.equals(that.service);
    }

    /** Iguales si sus dos principales coinciden. */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof DelegationPermission)) {
            return false;
        }
        return implies((DelegationPermission) obj);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.subordinate, this.service);
    }

    /** Una coleccion de estos. */
    @Override
    public PermissionCollection newPermissionCollection() {
        return new KrbDelegationPermissionCollection();
    }

    /** Al leerse de un flujo se vuelven a separar los principales: no se serializan. */
    private void readObject(java.io.ObjectInputStream in)
            throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();
        init(getName());
    }
}
