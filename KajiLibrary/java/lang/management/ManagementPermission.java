package java.lang.management;

import java.security.BasicPermission;

/**
 * KajiLibrary's java.lang.management.ManagementPermission -- permission to look at or touch the
 * virtual machine.
 *
 * <p>Only two names, and anything else is an argument error:
 *
 * <ul>
 *   <li>{@code "monitor"} to read -- thread dumps, memory usage, system properties;
 *   <li>{@code "control"} to modify -- forcing a collection, changing thresholds, switching on
 *       contention tracking.
 * </ul>
 *
 * <p>It admits no wildcards, unlike most {@link BasicPermission}s. With two names they would not be
 * needed, and allowing them would open the door to granting {@code control} by accident.
 *
 * <p>Marked for removal along with the whole {@code SecurityManager} mechanism, which no longer
 * controls anything. It is kept so old code compiles.
 */
@Deprecated(since = "25", forRemoval = true)
public final class ManagementPermission extends BasicPermission {

    private static final long serialVersionUID = 1897496590799378737L;

    /**
     * @param name {@code "monitor"} or {@code "control"}
     * @throws NullPointerException if it is null
     * @throws IllegalArgumentException if it is anything else
     */
    public ManagementPermission(String name) {
        super(name);
        if (!name.equals("control") && !name.equals("monitor")) {
            throw new IllegalArgumentException("name: " + name);
        }
    }

    /**
     * The same; the actions have to be null or empty.
     *
     * @throws IllegalArgumentException if actions are given
     */
    public ManagementPermission(String name, String actions) throws IllegalArgumentException {
        super(name);
        if (!name.equals("control") && !name.equals("monitor")) {
            throw new IllegalArgumentException("name: " + name);
        }
        if (actions != null && actions.length() > 0) {
            throw new IllegalArgumentException("actions: " + actions);
        }
    }
}
