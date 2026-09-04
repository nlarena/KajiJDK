package javax.management;

import java.security.BasicPermission;
import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 * Permiso sobre las operaciones de {@link MBeanServerFactory}.
 *
 * <p>Dos cosas lo separan de un {@link BasicPermission} comun:
 *
 * <ul>
 *   <li><b>el nombre puede ser una lista</b> separada por comas --`"createMBeanServer,findMBeanServer"`--,
 *       cosa que la sintaxis de comodines de `BasicPermission` no sabe hacer;
 *   <li><b>`createMBeanServer` implica `newMBeanServer`.</b> No es una regla arbitraria: crear
 *       registra el agente en la fabrica, y quien puede lo mas --dejar un agente encontrable por
 *       cualquiera-- puede lo menos --armarse uno privado--. Al reves no vale.
 * </ul>
 *
 * <p>Por eso todo se resuelve sobre una mascara de bits y no sobre cadenas: la implicacion entre
 * `create` y `new` es una relacion entre conjuntos, y escrita como comparacion de textos seria un
 * nido de casos especiales.
 */
public class MBeanServerPermission extends BasicPermission {

    private static final long serialVersionUID = -5661980843569388590L;

    private static final int CREAR = 1;
    private static final int BUSCAR = 2;
    private static final int NUEVO = 4;
    private static final int LIBERAR = 8;
    private static final int TODO = CREAR | BUSCAR | NUEVO | LIBERAR;

    /** El orden en que se reconstruye el nombre canonico; fijo, para que `equals` sea estable. */
    private static final String[] NOMBRES = { "createMBeanServer", "findMBeanServer",
                                              "newMBeanServer", "releaseMBeanServer" };
    private static final int[] BITS = { CREAR, BUSCAR, NUEVO, LIBERAR };

    /** Derivada del nombre, que es lo unico que se serializa: no hace falta guardarla. */
    private transient int mascara;

    /** @throws IllegalArgumentException si el nombre esta vacio o trae algo desconocido */
    public MBeanServerPermission(String name) {
        this(name, null);
    }

    /**
     * @param actions tiene que ser `null` o vacio: este permiso no tiene acciones
     * @throws IllegalArgumentException si el nombre no sirve o si vienen acciones
     */
    public MBeanServerPermission(String name, String actions) {
        super(canonico(name), actions);
        this.mascara = mascaraDe(name);
        if (actions != null && actions.length() > 0) {
            throw new IllegalArgumentException("MBeanServerPermission no lleva acciones: " + actions);
        }
    }

    private static int mascaraDe(String name) {
        if (name == null) {
            throw new NullPointerException("El nombre no puede ser null");
        }
        String n = name.trim();
        if (n.equals("*")) {
            return TODO;
        }
        if (n.length() == 0) {
            throw new IllegalArgumentException("El nombre no puede estar vacio");
        }
        int m = 0;
        for (String parte : n.split(",", -1)) {
            String p = parte.trim();
            int bit = 0;
            for (int i = 0; i < NOMBRES.length; i++) {
                if (NOMBRES[i].equals(p)) {
                    bit = BITS[i];
                    break;
                }
            }
            if (bit == 0) {
                throw new IllegalArgumentException("Nombre invalido: " + p);
            }
            m |= bit;
        }
        // Es aca y no en `implies` donde se cierra la implicacion. Meterla en la mascara la vuelve
        // automatica para `equals`, `hashCode` y las colecciones, en vez de un caso suelto.
        if ((m & CREAR) != 0) {
            m |= NUEVO;
        }
        return m;
    }

    /** El nombre normalizado: mismo conjunto de operaciones, siempre el mismo texto. */
    private static String canonico(String name) {
        return deMascara(mascaraDe(name));
    }

    private static String deMascara(int m) {
        if (m == TODO) {
            return "*";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < NOMBRES.length; i++) {
            if ((m & BITS[i]) != 0) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(NOMBRES[i]);
            }
        }
        return sb.toString();
    }

    /** Sobre la mascara: es el conjunto de operaciones lo que decide. */
    public int hashCode() {
        return mascara;
    }

    /** Este permiso cubre a `p` si su mascara contiene entera la de `p`. */
    public boolean implies(Permission p) {
        if (!(p instanceof MBeanServerPermission)) {
            return false;
        }
        MBeanServerPermission otro = (MBeanServerPermission) p;
        return (mascara & otro.mascara) == otro.mascara;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof MBeanServerPermission)) {
            return false;
        }
        return mascara == ((MBeanServerPermission) obj).mascara;
    }

    /**
     * Una coleccion que junta permisos <b>por union de mascaras</b>.
     *
     * <p>Es lo que hace que `{createMBeanServer} + {findMBeanServer}` implique
     * `createMBeanServer,findMBeanServer`, cosa que ninguna coleccion generica puede deducir: la
     * heterogenea pregunta permiso por permiso y ninguno de los dos, solo, alcanza.
     */
    public PermissionCollection newPermissionCollection() {
        return new ColeccionMBeanServerPermission();
    }
}

/**
 * Paquete-privada a proposito: es un detalle de {@link MBeanServerPermission} y el JDK tampoco la
 * expone.
 */
class ColeccionMBeanServerPermission extends PermissionCollection {

    private static final long serialVersionUID = -5661980843569388591L;

    private MBeanServerPermission acumulado = null;

    public synchronized void add(Permission permission) {
        if (!(permission instanceof MBeanServerPermission)) {
            throw new IllegalArgumentException("No es un MBeanServerPermission: " + permission);
        }
        if (isReadOnly()) {
            throw new SecurityException("La coleccion es de solo lectura");
        }
        MBeanServerPermission p = (MBeanServerPermission) permission;
        if (acumulado == null) {
            acumulado = p;
        } else if (!acumulado.implies(p)) {
            // La union se arma por el nombre: el constructor sabe leer la lista con comas, asi que
            // no hace falta exponer la mascara.
            String union = acumulado.getName().equals("*") || p.getName().equals("*")
                    ? "*" : acumulado.getName() + "," + p.getName();
            acumulado = new MBeanServerPermission(union);
        }
    }

    public synchronized boolean implies(Permission permission) {
        return acumulado != null && acumulado.implies(permission);
    }

    public synchronized Enumeration<Permission> elements() {
        final MBeanServerPermission p = acumulado;
        return new Enumeration<Permission>() {
            private boolean entregado = false;

            public boolean hasMoreElements() {
                return p != null && !entregado;
            }

            public Permission nextElement() {
                if (p == null || entregado) {
                    throw new NoSuchElementException();
                }
                entregado = true;
                return p;
            }
        };
    }
}
