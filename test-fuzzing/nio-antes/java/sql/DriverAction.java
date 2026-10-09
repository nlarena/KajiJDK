package java.sql;

/**
 * KajiLibrary's java.sql.DriverAction -- what a driver wants to do when it is deregistered.
 *
 * <p>It exists so that the cleanup is not public: if the driver exposed a method for this, anyone
 * could call it. By registering it with {@link DriverManager#registerDriver(Driver, DriverAction)}
 * only the manager holds the reference, and it only uses it when appropriate.
 */
public interface DriverAction {

    /** {@link DriverManager} calls it when deregistering the driver. */
    void deregister();
}
