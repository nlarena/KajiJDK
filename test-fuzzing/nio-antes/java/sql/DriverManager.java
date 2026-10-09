package java.sql;

/**
 * KajiLibrary's java.sql.DriverManager -- the registry of drivers, and the classic door to a
 * connection.
 *
 * <p>It works by **asking in order**: it is given a URL, and it asks each registered driver whether
 * it understands it until one says yes. That is what allows the code to name no driver, and it is
 * also why the registration order matters when two could serve the same URL.
 *
 * <p>Today {@link javax.sql.DataSource} is preferable in its place: the manager does not pool
 * connections, cannot be configured from outside and is static, that is, global. It is kept because
 * half the world uses it.
 *
 * <p>This implementation **registers and looks up for real**; what it does not do is discover
 * drivers through `META-INF/services`, because this library does not read that directory. A driver
 * has to be registered by calling {@link #registerDriver}.
 */
public class DriverManager {

    // Registered, in order. A list and not a map because the lookup **is** sequential: the question
    // is not "which one is called that" but "which one accepts this URL", and only the driver can
    // answer it.
    private static final java.util.ArrayList<Registered> DRIVERS =
            new java.util.ArrayList<Registered>();

    private static java.io.PrintWriter logWriter = null;
    private static int loginTimeout = 0;

    private DriverManager() {
    }

    // The driver and what to do on deregistering it. A pair and not two parallel lists.
    private static class Registered {
        final Driver driver;
        final DriverAction action;

        Registered(Driver driver, DriverAction action) {
            this.driver = driver;
            this.action = action;
        }
    }

    /** It registers that driver. */
    public static void registerDriver(Driver driver) throws SQLException {
        registerDriver(driver, null);
    }

    /** It registers that driver, with what to do on deregistering it. */
    public static void registerDriver(Driver driver, DriverAction action) throws SQLException {
        if (driver == null) {
            throw new NullPointerException("driver");
        }
        synchronized (DRIVERS) {
            int i = 0;
            while (i < DRIVERS.size()) {
                if (DRIVERS.get(i).driver == driver) {
                    return;
                }
                i = i + 1;
            }
            DRIVERS.add(new Registered(driver, action));
        }
    }

    /** It deregisters that driver, and tells it if it had left a {@link DriverAction}. */
    public static void deregisterDriver(Driver driver) throws SQLException {
        if (driver == null) {
            return;
        }
        DriverAction action = null;
        synchronized (DRIVERS) {
            int i = 0;
            while (i < DRIVERS.size()) {
                if (DRIVERS.get(i).driver == driver) {
                    action = DRIVERS.get(i).action;
                    DRIVERS.remove(i);
                    break;
                }
                i = i + 1;
            }
        }
        // Outside the `synchronized`: the notice is the driver's code and can do anything,
        // including calling back in here. Calling it with the lock held would be asking for a
        // deadlock.
        if (action != null) {
            action.deregister();
        }
    }

    /** The registered drivers. */
    public static java.util.Enumeration<Driver> getDrivers() {
        return java.util.Collections.enumeration(driverList());
    }

    /** The same ones, as a stream. */
    public static java.util.stream.Stream<Driver> drivers() {
        return driverList().stream();
    }

    private static java.util.List<Driver> driverList() {
        java.util.ArrayList<Driver> copy = new java.util.ArrayList<Driver>();
        synchronized (DRIVERS) {
            int i = 0;
            while (i < DRIVERS.size()) {
                copy.add(DRIVERS.get(i).driver);
                i = i + 1;
            }
        }
        return copy;
    }

    /**
     * The first registered driver that accepts that URL.
     *
     * @throws SQLException if none accepts it
     */
    public static Driver getDriver(String url) throws SQLException {
        java.util.List<Driver> all = driverList();
        int i = 0;
        while (i < all.size()) {
            if (all.get(i).acceptsURL(url)) {
                return all.get(i);
            }
            i = i + 1;
        }
        throw new SQLException("No suitable driver", "08001");
    }

    /** A connection to that URL. */
    public static Connection getConnection(String url) throws SQLException {
        return connect(url, new java.util.Properties());
    }

    /** A connection to that URL with those credentials. */
    public static Connection getConnection(String url, String user, String password)
            throws SQLException {
        java.util.Properties info = new java.util.Properties();
        if (user != null) {
            info.put("user", user);
        }
        if (password != null) {
            info.put("password", password);
        }
        return connect(url, info);
    }

    /** A connection to that URL with those properties. */
    public static Connection getConnection(String url, java.util.Properties info)
            throws SQLException {
        return connect(url, info == null ? new java.util.Properties() : info);
    }

    // Each one is asked until one returns something. The failures are **accumulated** rather than
    // the first thrown: if three drivers said the URL was theirs and all three failed, all three
    // reasons matter, and `SQLException`'s chain is there for that.
    private static Connection connect(String url, java.util.Properties info) throws SQLException {
        if (url == null) {
            throw new SQLException("The url cannot be null", "08001");
        }
        java.util.List<Driver> all = driverList();
        SQLException failures = null;
        int i = 0;
        while (i < all.size()) {
            try {
                Connection c = all.get(i).connect(url, info);
                if (c != null) {
                    return c;
                }
            } catch (SQLException e) {
                if (failures == null) {
                    failures = e;
                } else {
                    failures.setNextException(e);
                }
            }
            i = i + 1;
        }
        if (failures != null) {
            throw failures;
        }
        throw new SQLException("No suitable driver found for " + url, "08001");
    }

    /** Seconds to wait when connecting; zero for the system's limit. */
    public static void setLoginTimeout(int seconds) {
        loginTimeout = seconds;
    }

    public static int getLoginTimeout() {
        return loginTimeout;
    }

    /** Where the manager's and the drivers' messages go. */
    public static java.io.PrintWriter getLogWriter() {
        return logWriter;
    }

    public static void setLogWriter(java.io.PrintWriter out) {
        logWriter = out;
    }

    /**
     * @deprecated use {@link #getLogWriter}
     */
    @Deprecated
    public static java.io.PrintStream getLogStream() {
        return null;
    }

    /**
     * @deprecated use {@link #setLogWriter}
     */
    @Deprecated
    public static void setLogStream(java.io.PrintStream out) {
        logWriter = out == null ? null : new java.io.PrintWriter(out);
    }

    /** It writes a line to the message destination, if there is one. */
    public static void println(String message) {
        java.io.PrintWriter w = logWriter;
        if (w != null) {
            w.println(message);
            w.flush();
        }
    }
}
