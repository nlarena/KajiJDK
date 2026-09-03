package java.sql;

/**
 * KajiLibrary's java.sql.DriverManager -- el registro de drivers, y la puerta clasica a una conexion.
 *
 * <p>Funciona por **preguntas en orden**: se le da una URL, y le pregunta a cada driver registrado si
 * la entiende hasta que uno dice que si. Eso es lo que permite que el codigo no nombre a ningun
 * driver, y es tambien por lo que el orden de registro importa cuando dos podrian atender la misma
 * URL.
 *
 * <p>Hoy conviene {@link javax.sql.DataSource} en su lugar: el gestor no agrupa conexiones, no se
 * puede configurar desde afuera y es estatico, o sea global. Se mantiene porque medio mundo lo usa.
 *
 * <p>Esta implementacion **registra y busca de verdad**; lo que no hace es descubrir drivers por
 * `META-INF/services`, porque esta biblioteca no lee ese directorio. Un driver hay que registrarlo
 * llamando a {@link #registerDriver}.
 */
public class DriverManager {

    // Registrados, en orden. Una lista y no un mapa porque la busqueda **es** secuencial: la pregunta
    // no es "cual se llama asi" sino "cual acepta esta URL", y solo el driver sabe contestarla.
    private static final java.util.ArrayList<Registrado> DRIVERS =
            new java.util.ArrayList<Registrado>();

    private static java.io.PrintWriter logWriter = null;
    private static int loginTimeout = 0;

    private DriverManager() {
    }

    // El driver y lo que hay que hacer al darlo de baja. Un par y no dos listas paralelas.
    private static class Registrado {
        final Driver driver;
        final DriverAction action;

        Registrado(Driver driver, DriverAction action) {
            this.driver = driver;
            this.action = action;
        }
    }

    /** Registra ese driver. */
    public static void registerDriver(Driver driver) throws SQLException {
        registerDriver(driver, null);
    }

    /** Registra ese driver, con lo que hay que hacer al darlo de baja. */
    public static void registerDriver(Driver driver, DriverAction da) throws SQLException {
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
            DRIVERS.add(new Registrado(driver, da));
        }
    }

    /** Da de baja ese driver, y le avisa si habia dejado un {@link DriverAction}. */
    public static void deregisterDriver(Driver driver) throws SQLException {
        if (driver == null) {
            return;
        }
        DriverAction aviso = null;
        synchronized (DRIVERS) {
            int i = 0;
            while (i < DRIVERS.size()) {
                if (DRIVERS.get(i).driver == driver) {
                    aviso = DRIVERS.get(i).action;
                    DRIVERS.remove(i);
                    break;
                }
                i = i + 1;
            }
        }
        // Fuera del `synchronized`: el aviso es codigo del driver y puede hacer cualquier cosa,
        // incluso volver a llamar aca. Llamarlo con el candado tomado seria pedir un abrazo mortal.
        if (aviso != null) {
            aviso.deregister();
        }
    }

    /** Los drivers registrados. */
    public static java.util.Enumeration<Driver> getDrivers() {
        return java.util.Collections.enumeration(listaDeDrivers());
    }

    /** Los mismos, como flujo. */
    public static java.util.stream.Stream<Driver> drivers() {
        return listaDeDrivers().stream();
    }

    private static java.util.List<Driver> listaDeDrivers() {
        java.util.ArrayList<Driver> salida = new java.util.ArrayList<Driver>();
        synchronized (DRIVERS) {
            int i = 0;
            while (i < DRIVERS.size()) {
                salida.add(DRIVERS.get(i).driver);
                i = i + 1;
            }
        }
        return salida;
    }

    /**
     * El primer driver registrado que acepta esa URL.
     *
     * @throws SQLException si ninguno la acepta
     */
    public static Driver getDriver(String url) throws SQLException {
        java.util.List<Driver> todos = listaDeDrivers();
        int i = 0;
        while (i < todos.size()) {
            if (todos.get(i).acceptsURL(url)) {
                return todos.get(i);
            }
            i = i + 1;
        }
        throw new SQLException("No suitable driver", "08001");
    }

    /** Una conexion a esa URL. */
    public static Connection getConnection(String url) throws SQLException {
        return conectar(url, new java.util.Properties());
    }

    /** Una conexion a esa URL con esas credenciales. */
    public static Connection getConnection(String url, String user, String password)
            throws SQLException {
        java.util.Properties info = new java.util.Properties();
        if (user != null) {
            info.put("user", user);
        }
        if (password != null) {
            info.put("password", password);
        }
        return conectar(url, info);
    }

    /** Una conexion a esa URL con esas propiedades. */
    public static Connection getConnection(String url, java.util.Properties info)
            throws SQLException {
        return conectar(url, info == null ? new java.util.Properties() : info);
    }

    // Se le pregunta a cada uno hasta que alguno devuelva algo. Se **acumulan** los fallos en vez de
    // lanzar el primero: si tres drivers dijeron que la URL era suya y los tres fallaron, las tres
    // razones importan, y la cadena de `SQLException` esta para eso.
    private static Connection conectar(String url, java.util.Properties info) throws SQLException {
        if (url == null) {
            throw new SQLException("The url cannot be null", "08001");
        }
        java.util.List<Driver> todos = listaDeDrivers();
        SQLException fallos = null;
        int i = 0;
        while (i < todos.size()) {
            try {
                Connection c = todos.get(i).connect(url, info);
                if (c != null) {
                    return c;
                }
            } catch (SQLException e) {
                if (fallos == null) {
                    fallos = e;
                } else {
                    fallos.setNextException(e);
                }
            }
            i = i + 1;
        }
        if (fallos != null) {
            throw fallos;
        }
        throw new SQLException("No suitable driver found for " + url, "08001");
    }

    /** Segundos a esperar al conectar; cero para el limite del sistema. */
    public static void setLoginTimeout(int seconds) {
        loginTimeout = seconds;
    }

    public static int getLoginTimeout() {
        return loginTimeout;
    }

    /** A donde van los mensajes del gestor y de los drivers. */
    public static java.io.PrintWriter getLogWriter() {
        return logWriter;
    }

    public static void setLogWriter(java.io.PrintWriter out) {
        logWriter = out;
    }

    /**
     * @deprecated usar {@link #getLogWriter}
     */
    @Deprecated
    public static java.io.PrintStream getLogStream() {
        return null;
    }

    /**
     * @deprecated usar {@link #setLogWriter}
     */
    @Deprecated
    public static void setLogStream(java.io.PrintStream out) {
        logWriter = out == null ? null : new java.io.PrintWriter(out);
    }

    /** Escribe una linea en el destino de mensajes, si hay alguno. */
    public static void println(String message) {
        java.io.PrintWriter w = logWriter;
        if (w != null) {
            w.println(message);
            w.flush();
        }
    }
}
