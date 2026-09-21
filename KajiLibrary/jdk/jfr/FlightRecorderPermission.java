package jdk.jfr;

import java.security.BasicPermission;

/**
 * The permission that historically guarded the operations of JFR.
 *
 * <p>The names are {@code accessFlightRecorder}, for handling recordings, and
 * {@code registerEvent}, for registering types of event of one's own.
 *
 * <p>It is kept because it is public API, even though the {@code SecurityManager} has been
 * permanently disabled since JDK 24 and the check no longer happens. Building one still works; what
 * no longer happens is that somebody consults it.
 *
 * @since 9
 */
public final class FlightRecorderPermission extends BasicPermission {

    private static final long serialVersionUID = -6989096058590316034L;

    /**
     * A permission with that name.
     *
     * @param name {@code "accessFlightRecorder"} or {@code "registerEvent"}
     * @throws NullPointerException if the name is {@code null}
     * @throws IllegalArgumentException if the name is empty
     */
    public FlightRecorderPermission(String name) {
        super(name);
    }
}
