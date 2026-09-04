package javax.management.relation;

import java.io.Serializable;

/**
 * La descripcion de un rol dentro de un tipo de relacion: como se llama, que puede ir adentro y
 * cuantos.
 *
 * <h2>Que es un rol</h2>
 *
 * <p>Una relacion de JMX conecta MBeans, y cada punta de la conexion es un <em>rol</em>. En una
 * relacion "dueno/recurso", {@code dueno} y {@code recurso} son roles, y esta clase describe uno de
 * ellos <strong>antes</strong> de que exista ninguna relacion concreta — es el esquema.
 *
 * <h2>Los dos grados, que es lo que hace util al esquema</h2>
 *
 * <p>{@link #getMinDegree} y {@link #getMaxDegree} dicen cuantos MBeans puede tener el rol. Con eso
 * se expresa la cardinalidad: {@code (1,1)} es exactamente uno, {@code (0,1)} opcional,
 * {@code (1, INFINITY)} al menos uno.
 *
 * <p>Es lo que el servicio de relaciones verifica en cada escritura, y lo que hace que una relacion
 * mal formada se rechace en vez de quedar inconsistente.
 *
 * <p>{@link #ROLE_CARDINALITY_INFINITY} es {@code -1} y no {@link Integer#MAX_VALUE}: si fuera un
 * numero grande, comparar seria correcto pero {@code maxDegree + 1} desbordaria. Con {@code -1} la
 * comparacion es un caso aparte y explicito.
 *
 * <h2>Inmutable</h2>
 *
 * <p>No tiene setters, y tiene que ser asi: es el esquema contra el que se valida, y si se pudiera
 * cambiar despues de declarado, las relaciones ya creadas dejarian de cumplirlo sin que nadie las
 * tocara.
 */
public class RoleInfo implements Serializable {

    private static final long serialVersionUID = 2504952983494636987L;

    /** Sin limite superior. Vale {@code -1}; ver la nota de la clase. */
    public static final int ROLE_CARDINALITY_INFINITY = -1;

    private final String name;
    private final boolean isReadable;
    private final boolean isWritable;
    private final String description;
    private final int minDegree;
    private final int maxDegree;
    private final String referencedMBeanClassName;

    /**
     * Con todo.
     *
     * @param roleName el nombre; no puede ser {@code null}
     * @param mbeanClassName la clase que los MBeans referenciados deben ser o extender
     * @param read si el rol se puede leer
     * @param write si el rol se puede escribir
     * @param min cuantos MBeans como minimo
     * @param max cuantos como maximo, o {@link #ROLE_CARDINALITY_INFINITY}
     * @param descr una descripcion para mostrar
     * @throws IllegalArgumentException si falta el nombre o la clase
     * @throws InvalidRoleInfoException si el minimo es mayor que el maximo — un rol asi no se puede
     *     cumplir nunca, y rechazarlo aca evita descubrirlo recien al crear la relacion
     */
    public RoleInfo(String roleName, String mbeanClassName, boolean read, boolean write,
            int min, int max, String descr)
            throws IllegalArgumentException, InvalidRoleInfoException {
        if (roleName == null) {
            throw new IllegalArgumentException("falta el nombre del rol");
        }
        if (mbeanClassName == null) {
            throw new IllegalArgumentException("falta la clase de los MBeans referenciados");
        }
        int mn = min;
        int mx = max;
        if (mn == ROLE_CARDINALITY_INFINITY) {
            mn = Integer.MAX_VALUE;
        }
        if (mx == ROLE_CARDINALITY_INFINITY) {
            mx = Integer.MAX_VALUE;
        }
        if (mn > mx) {
            throw new InvalidRoleInfoException(
                    "el grado minimo es mayor que el maximo en el rol " + roleName);
        }
        this.name = roleName;
        this.referencedMBeanClassName = mbeanClassName;
        this.isReadable = read;
        this.isWritable = write;
        this.minDegree = mn;
        this.maxDegree = mx;
        this.description = descr;
    }

    /** Sin descripcion, con cardinalidad {@code (1,1)}. */
    public RoleInfo(String roleName, String mbeanClassName, boolean read, boolean write)
            throws IllegalArgumentException {
        this(roleName, mbeanClassName, read, write, 1, 1, null, true);
    }

    /** Legible, escribible, exactamente uno, sin descripcion. */
    public RoleInfo(String roleName, String mbeanClassName) throws IllegalArgumentException {
        this(roleName, mbeanClassName, true, true, 1, 1, null, true);
    }

    /**
     * Una copia.
     *
     * @throws IllegalArgumentException si {@code roleInfo} es {@code null}
     */
    public RoleInfo(RoleInfo roleInfo) throws IllegalArgumentException {
        if (roleInfo == null) {
            throw new IllegalArgumentException("no hay nada que copiar");
        }
        this.name = roleInfo.getName();
        this.referencedMBeanClassName = roleInfo.getRefMBeanClassName();
        this.isReadable = roleInfo.isReadable();
        this.isWritable = roleInfo.isWritable();
        this.minDegree = roleInfo.getMinDegree();
        this.maxDegree = roleInfo.getMaxDegree();
        this.description = roleInfo.getDescription();
    }

    // Los constructores cortos no pueden tirar `InvalidRoleInfoException` —no esta en su firma— y
    // con (1,1) es imposible que pase. Este privado existe solo para que el compilador lo sepa.
    private RoleInfo(String roleName, String mbeanClassName, boolean read, boolean write,
            int min, int max, String descr, boolean interno) throws IllegalArgumentException {
        if (roleName == null) {
            throw new IllegalArgumentException("falta el nombre del rol");
        }
        if (mbeanClassName == null) {
            throw new IllegalArgumentException("falta la clase de los MBeans referenciados");
        }
        this.name = roleName;
        this.referencedMBeanClassName = mbeanClassName;
        this.isReadable = read;
        this.isWritable = write;
        this.minDegree = min;
        this.maxDegree = max;
        this.description = descr;
    }

    /** El nombre del rol. */
    public String getName() {
        return this.name;
    }

    /** Si el rol se puede leer. */
    public boolean isReadable() {
        return this.isReadable;
    }

    /** Si el rol se puede escribir. */
    public boolean isWritable() {
        return this.isWritable;
    }

    /** La descripcion, o {@code null}. */
    public String getDescription() {
        return this.description;
    }

    /** Cuantos MBeans como minimo. */
    public int getMinDegree() {
        return this.minDegree;
    }

    /** Cuantos como maximo. */
    public int getMaxDegree() {
        return this.maxDegree;
    }

    /** La clase que los MBeans referenciados deben ser o extender. */
    public String getRefMBeanClassName() {
        return this.referencedMBeanClassName;
    }

    /** Si {@code value} llega al minimo. */
    public boolean checkMinDegree(int value) {
        return value >= this.minDegree;
    }

    /** Si {@code value} no pasa del maximo. */
    public boolean checkMaxDegree(int value) {
        return value <= this.maxDegree;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("role info name: ").append(this.name);
        sb.append("; isReadable: ").append(String.valueOf(this.isReadable));
        sb.append("; isWritable: ").append(String.valueOf(this.isWritable));
        sb.append("; description: ").append(String.valueOf(this.description));
        sb.append("; minimum degree: ").append(String.valueOf(this.minDegree));
        sb.append("; maximum degree: ").append(String.valueOf(this.maxDegree));
        sb.append("; ObjectName class: ").append(this.referencedMBeanClassName);
        return sb.toString();
    }
}
