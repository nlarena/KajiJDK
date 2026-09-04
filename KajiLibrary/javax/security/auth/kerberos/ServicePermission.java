package javax.security.auth.kerberos;

import java.io.Serializable;
import java.security.Permission;
import java.security.PermissionCollection;

/**
 * KajiLibrary's javax.security.auth.kerberos.ServicePermission -- permiso para usar un servicio de
 * Kerberos.
 *
 * <p>El nombre es el principal del servicio, o {@code "*"} para todos. Las acciones son dos:
 * {@code initiate} --pedir un ticket para ese servicio, o sea actuar de cliente-- y {@code accept}
 * --recibir tickets para ese servicio, o sea ser el servicio--. Un servidor necesita {@code accept}
 * sobre su propio principal; un cliente necesita {@code initiate} sobre el del servidor.
 *
 * <p>La forma canonica es {@code "initiate,accept"}, en ese orden, sin espacios y en minusculas; al
 * construir se aceptan mayusculas y espacios alrededor de cada accion.
 *
 * @deprecated el JDK lo marca para remocion junto con el gestor de seguridad; sigue aca porque el
 *     codigo que lo instancia tiene que poder compilar y correr
 */
@Deprecated(since = "17", forRemoval = true)
public final class ServicePermission extends Permission implements Serializable {

    private static final long serialVersionUID = -1227585031618624935L;

    /** Actuar de cliente. */
    private static final int INITIATE = 0x1;

    /** Actuar de servicio. */
    private static final int ACCEPT = 0x2;

    /** Las dos. */
    private static final int ALL = INITIATE | ACCEPT;

    /** Que acciones estan permitidas. */
    private transient int mask;

    /** La forma canonica, armada a pedido. */
    private String actions;

    /**
     * Ese permiso sobre ese servicio.
     *
     * @param servicePrincipal el principal del servicio, o {@code "*"}
     * @param action {@code initiate}, {@code accept} o las dos separadas por coma
     * @throws NullPointerException si cualquiera es null
     * @throws IllegalArgumentException si las acciones estan vacias o alguna no existe
     */
    public ServicePermission(String servicePrincipal, String action) {
        super(servicePrincipal);
        if (servicePrincipal == null) {
            throw new NullPointerException("service principal can't be null");
        }
        init(action);
    }

    /** Con la mascara ya armada; para la coleccion. */
    ServicePermission(String servicePrincipal, int mask) {
        super(servicePrincipal);
        this.mask = mask & ALL;
    }

    /** Interpreta las acciones. */
    private void init(String action) {
        if (action == null) {
            throw new NullPointerException("action can't be null");
        }
        if (action.isEmpty()) {
            throw new IllegalArgumentException("action can't be empty");
        }
        this.mask = getMask(action);
    }

    /**
     * Si este permiso alcanza para lo que el otro pide: el mismo servicio o {@code "*"}, y todas sus
     * acciones.
     */
    @Override
    public boolean implies(Permission p) {
        if (!(p instanceof ServicePermission)) {
            return false;
        }
        ServicePermission that = (ServicePermission) p;
        return (this.mask & that.mask) == that.mask && impliesIgnoreMask(that);
    }

    /** Si el nombre alcanza, sin mirar las acciones. */
    boolean impliesIgnoreMask(ServicePermission p) {
        return getName().equals("*") || getName().equals(p.getName());
    }

    /** Iguales si nombran al mismo servicio y permiten las mismas acciones. */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof ServicePermission)) {
            return false;
        }
        ServicePermission that = (ServicePermission) obj;
        return this.mask == that.mask && getName().equals(that.getName());
    }

    @Override
    public int hashCode() {
        return getName().hashCode() ^ this.mask;
    }

    /** La forma canonica de esos bits. */
    static String getActions(int mask) {
        StringBuilder text = new StringBuilder();
        if ((mask & INITIATE) == INITIATE) {
            text.append("initiate");
        }
        if ((mask & ACCEPT) == ACCEPT) {
            if (text.length() > 0) {
                text.append(',');
            }
            text.append("accept");
        }
        return text.toString();
    }

    /** Las acciones en forma canonica. Ver la nota de la clase. */
    @Override
    public String getActions() {
        if (this.actions == null) {
            this.actions = getActions(this.mask);
        }
        return this.actions;
    }

    /** Una coleccion que junta los permisos del mismo servicio. */
    @Override
    public PermissionCollection newPermissionCollection() {
        return new KrbServicePermissionCollection();
    }

    /** Los bits. */
    int getMask() {
        return this.mask;
    }

    /**
     * Los bits de esa lista de acciones.
     *
     * <p>Cada accion se recorta y se compara sin distinguir mayusculas; una vacia --de una coma de
     * mas-- es tan invalida como una que no existe, y el mensaje repite la lista entera.
     */
    private static int getMask(String action) {
        int mask = 0;
        String[] pieces = action.split(",", -1);
        int i = 0;
        while (i < pieces.length) {
            String piece = pieces[i].trim();
            if (piece.equalsIgnoreCase("initiate")) {
                mask = mask | INITIATE;
            } else if (piece.equalsIgnoreCase("accept")) {
                mask = mask | ACCEPT;
            } else {
                throw new IllegalArgumentException("invalid permission: " + action);
            }
            i = i + 1;
        }
        return mask;
    }

    /** Al leerse de un flujo se vuelve a interpretar la forma canonica: la mascara no se serializa. */
    private void readObject(java.io.ObjectInputStream in)
            throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();
        init(this.actions == null ? "" : this.actions);
    }
}
