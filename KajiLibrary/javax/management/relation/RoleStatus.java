package javax.management.relation;

/**
 * Los codigos que dicen <strong>por que</strong> un rol no se pudo leer o escribir.
 *
 * <h2>Por que codigos y no excepciones</h2>
 *
 * <p>Porque una operacion sobre varios roles puede fallar en algunos y andar en otros. Con
 * excepciones habria que elegir: cortar en el primer problema —perdiendo los que si funcionaron— o
 * tragarselos. Los codigos permiten devolver las dos listas, que es lo que hace {@link RoleResult}.
 *
 * <p>Cada rol que fallo llega en un {@link RoleUnresolved} con uno de estos numeros adentro.
 */
public class RoleStatus {

    /** La relacion no tiene un rol con ese nombre. */
    public static final int NO_ROLE_WITH_NAME = 1;

    /** El rol existe pero su descripcion no lo declara legible. */
    public static final int ROLE_NOT_READABLE = 2;

    /** El rol existe pero su descripcion no lo declara escribible. */
    public static final int ROLE_NOT_WRITABLE = 3;

    /** Menos MBeans que el minimo que exige {@link RoleInfo}. */
    public static final int LESS_THAN_MIN_ROLE_DEGREE = 4;

    /** Mas MBeans que el maximo. */
    public static final int MORE_THAN_MAX_ROLE_DEGREE = 5;

    /** Un MBean referenciado no es de la clase que el rol exige. */
    public static final int REF_MBEAN_OF_INCORRECT_CLASS = 6;

    /**
     * Un MBean referenciado no esta registrado en el servidor.
     *
     * <p>Distinto de {@link #REF_MBEAN_OF_INCORRECT_CLASS}: alli el objeto existe y es del tipo
     * equivocado; aca no existe. Confundirlos manda a revisar el lugar equivocado.
     */
    public static final int REF_MBEAN_NOT_REGISTERED = 7;

    public RoleStatus() {
    }

    /** Si {@code status} es uno de los siete codigos definidos. */
    public static boolean isRoleStatus(int status) {
        return status >= NO_ROLE_WITH_NAME && status <= REF_MBEAN_NOT_REGISTERED;
    }
}
