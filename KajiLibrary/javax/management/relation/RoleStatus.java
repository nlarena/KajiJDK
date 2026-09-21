package javax.management.relation;

/**
 * The codes that say <b>why</b> a role could not be read or written.
 *
 * <h2>Why codes and not exceptions</h2>
 *
 * <p>Because an operation over several roles may fail on some and work on others. With exceptions
 * you would have to choose: stop at the first problem --losing the ones that did work-- or swallow
 * them. The codes allow returning both lists, which is what {@link RoleResult} does.
 *
 * <p>Every role that failed arrives in a {@link RoleUnresolved} with one of these numbers inside.
 */
public class RoleStatus {

    /** The relation has no role with that name. */
    public static final int NO_ROLE_WITH_NAME = 1;

    /** The role exists but its description does not declare it readable. */
    public static final int ROLE_NOT_READABLE = 2;

    /** The role exists but its description does not declare it writable. */
    public static final int ROLE_NOT_WRITABLE = 3;

    /** Fewer MBeans than the minimum {@link RoleInfo} requires. */
    public static final int LESS_THAN_MIN_ROLE_DEGREE = 4;

    /** More MBeans than the maximum. */
    public static final int MORE_THAN_MAX_ROLE_DEGREE = 5;

    /** A referenced MBean is not of the class the role requires. */
    public static final int REF_MBEAN_OF_INCORRECT_CLASS = 6;

    /**
     * A referenced MBean is not registered in the server.
     *
     * <p>Different from {@link #REF_MBEAN_OF_INCORRECT_CLASS}: there the object exists and is of
     * the wrong type; here it does not exist. Confusing them sends you to look in the wrong place.
     */
    public static final int REF_MBEAN_NOT_REGISTERED = 7;

    public RoleStatus() {
    }

    /** Whether {@code status} is one of the seven defined codes. */
    public static boolean isRoleStatus(int status) {
        return status >= NO_ROLE_WITH_NAME && status <= REF_MBEAN_NOT_REGISTERED;
    }
}
