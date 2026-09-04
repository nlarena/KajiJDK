package javax.management;

import java.security.Permission;

/**
 * Permiso sobre <b>una operacion concreta contra un MBean concreto</b>.
 *
 * <p>El nombre tiene tres partes, {@code clase#miembro[nombreDeObjeto]}, y cualquiera puede faltar
 * o ser `*`, que significa "cualquiera". Las tres se comparan distinto, y esa es toda la clase:
 *
 * <ul>
 *   <li><b>la clase</b> admite comodin de sufijo --{@code com.foo.*} cubre todo el paquete--,
 *       porque los nombres de clase son jerarquicos por prefijo;
 *   <li><b>el miembro</b> no: un atributo `Count` y otro `CountTotal` no tienen relacion, asi que
 *       solo hay igualdad o `*`;
 *   <li><b>el nombre de objeto</b> se compara con {@link ObjectName#apply}, que ya sabe de
 *       comodines de dominio y de propiedades. Reimplementarlo aca seria tener dos definiciones de
 *       lo mismo.
 * </ul>
 *
 * <p>Las acciones van en mascara, y una implica a otra: {@code queryMBeans} implica
 * {@code queryNames}, porque quien puede traerse las instancias ya vio los nombres. Igual que en
 * {@link MBeanServerPermission}, esa implicacion se cierra al construir la mascara y no en
 * `implies`.
 *
 * <p>Un detalle que rompe la intuicion y esta en la especificacion: un permiso <b>otorgado</b> con
 * la parte vacia significa "cualquiera", pero el permiso que se <b>chequea</b> con la parte vacia
 * significa "no se de cual", y entonces solo lo cubre un otorgado que tambien acepte cualquiera.
 * Es la asimetria correcta: en la duda, no alcanza.
 */
public class MBeanPermission extends Permission {

    private static final long serialVersionUID = -2416928705275160661L;

    /** Las acciones que existen, en el orden en que se reconstruye la cadena canonica. */
    private static final String[] ACCIONES = {
        "addNotificationListener", "getAttribute", "getClassLoader", "getClassLoaderFor",
        "getClassLoaderRepository", "getDomains", "getMBeanInfo", "getObjectInstance",
        "instantiate", "invoke", "isInstanceOf", "queryMBeans", "queryNames", "registerMBean",
        "removeNotificationListener", "setAttribute", "unregisterMBean"
    };

    private static final int BIT_QUERY_MBEANS = 1 << 11;
    private static final int BIT_QUERY_NAMES = 1 << 12;
    private static final int TODAS = (1 << ACCIONES.length) - 1;

    /** Todo lo de abajo se deriva del nombre y de las acciones, que es lo unico que se serializa. */
    private transient String patronClase;
    private transient boolean claseComodinDeSufijo;
    private transient String patronMiembro;
    private transient ObjectName patronNombre;
    private transient int mascara;

    /**
     * @param name {@code clase#miembro[nombreDeObjeto]}, o `*`
     * @param actions lista separada por comas, o `*`
     * @throws IllegalArgumentException si el nombre o las acciones no parsean
     */
    public MBeanPermission(String name, String actions) {
        super(name);
        parsearNombre(name);
        this.mascara = mascaraDe(actions);
    }

    /**
     * Arma el nombre a partir de las partes, para no obligar a concatenar a mano.
     *
     * @param className `null` significa "cualquiera"
     * @param member `null` significa "cualquiera"
     * @param objectName `null` significa "cualquiera"
     */
    public MBeanPermission(String className, String member, ObjectName objectName, String actions) {
        this(armarNombre(className, member, objectName), actions);
    }

    private static String armarNombre(String className, String member, ObjectName objectName) {
        // El guion, y no el asterisco, es como la especificacion escribe "cualquiera" en una parte
        // que se armo desde `null`. Importa para el nombre del objeto, donde `*` no es un
        // `ObjectName` legal y seria un nombre invalido.
        StringBuilder sb = new StringBuilder();
        sb.append(className == null ? "-" : className);
        sb.append('#');
        sb.append(member == null ? "-" : member);
        sb.append('[');
        sb.append(objectName == null ? "-" : objectName.getCanonicalName());
        sb.append(']');
        return sb.toString();
    }

    /** Las tres formas de escribir "cualquiera" en una parte del nombre. */
    private static boolean cualquiera(String parte) {
        return parte.length() == 0 || parte.equals("*") || parte.equals("-");
    }

    private void parsearNombre(String name) {
        if (name == null) {
            throw new NullPointerException("El nombre no puede ser null");
        }
        if (name.length() == 0) {
            throw new IllegalArgumentException("El nombre no puede estar vacio");
        }
        if (name.equals("*")) {
            return; // los tres quedan en null, o sea "cualquiera"
        }

        String resto = name;
        // El corchete se busca desde el final: un ObjectName puede traer '#' adentro de un valor
        // citado, pero el '[' que abre la parte del nombre de objeto es el ultimo del texto.
        int abre = resto.indexOf('[');
        if (abre >= 0) {
            if (!resto.endsWith("]")) {
                throw new IllegalArgumentException("Falta cerrar el corchete: " + name);
            }
            String on = resto.substring(abre + 1, resto.length() - 1);
            resto = resto.substring(0, abre);
            // Aca `*` NO significa "cualquiera": la parte se parsea como `ObjectName`, y `*` solo
            // no es uno. Para "cualquiera" van el vacio o el guion. Es asi en la especificacion y
            // conviene respetarlo: `[*]` tiene que fallar, porque quien lo escribio cree estar
            // pidiendo todos los MBeans y en realidad escribio un nombre invalido.
            if (on.length() > 0 && !on.equals("-")) {
                try {
                    patronNombre = new ObjectName(on);
                } catch (MalformedObjectNameException e) {
                    throw new IllegalArgumentException("ObjectName invalido: " + on, e);
                }
            }
        }

        int num = resto.indexOf('#');
        String clase;
        if (num >= 0) {
            clase = resto.substring(0, num);
            String miembro = resto.substring(num + 1);
            if (!cualquiera(miembro)) {
                patronMiembro = miembro;
            }
        } else {
            clase = resto;
        }

        if (cualquiera(clase)) {
            return;
        }
        if (clase.endsWith(".*")) {
            claseComodinDeSufijo = true;
            patronClase = clase.substring(0, clase.length() - 1); // se queda el punto
        } else if (clase.endsWith("*")) {
            claseComodinDeSufijo = true;
            patronClase = clase.substring(0, clase.length() - 1);
        } else {
            patronClase = clase;
        }
    }

    private static int mascaraDe(String actions) {
        if (actions == null) {
            throw new IllegalArgumentException("Las acciones no pueden ser null");
        }
        String a = actions.trim();
        if (a.equals("*")) {
            return TODAS;
        }
        if (a.length() == 0) {
            throw new IllegalArgumentException("Las acciones no pueden estar vacias");
        }
        int m = 0;
        for (String parte : a.split(",", -1)) {
            String p = parte.trim();
            int bit = 0;
            for (int i = 0; i < ACCIONES.length; i++) {
                if (ACCIONES[i].equals(p)) {
                    bit = 1 << i;
                    break;
                }
            }
            if (bit == 0) {
                throw new IllegalArgumentException("Accion invalida: " + p);
            }
            m |= bit;
        }
        if ((m & BIT_QUERY_MBEANS) != 0) {
            m |= BIT_QUERY_NAMES;
        }
        return m;
    }

    /** La lista canonica: mismas acciones, siempre el mismo texto y el mismo orden. */
    public String getActions() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ACCIONES.length; i++) {
            if ((mascara & (1 << i)) != 0) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(ACCIONES[i]);
            }
        }
        return sb.toString();
    }

    public int hashCode() {
        return getName().hashCode() + getActions().hashCode();
    }

    /**
     * Cubre a `p` si sus acciones son un subconjunto de las de este y si las tres partes del nombre
     * coinciden.
     */
    public boolean implies(Permission p) {
        if (!(p instanceof MBeanPermission)) {
            return false;
        }
        MBeanPermission q = (MBeanPermission) p;

        if ((mascara & q.mascara) != q.mascara) {
            return false;
        }

        if (patronClase != null) {
            if (q.patronClase == null) {
                return false; // el pedido no dice de que clase: no alcanza un permiso restringido
            }
            if (claseComodinDeSufijo) {
                if (!q.patronClase.startsWith(patronClase)) {
                    return false;
                }
            } else if (!patronClase.equals(q.patronClase) || q.claseComodinDeSufijo) {
                return false;
            }
        }

        if (patronMiembro != null && !patronMiembro.equals(q.patronMiembro)) {
            return false;
        }

        if (patronNombre != null) {
            if (q.patronNombre == null || !patronNombre.apply(q.patronNombre)) {
                return false;
            }
        }
        return true;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof MBeanPermission)) {
            return false;
        }
        MBeanPermission q = (MBeanPermission) obj;
        return mascara == q.mascara && getName().equals(q.getName());
    }
}
