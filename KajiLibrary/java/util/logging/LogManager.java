package java.util.logging;

/**
 * KajiLibrary's java.util.logging.LogManager -- el registro de los {@link Logger} por nombre, y la
 * configuracion.
 *
 * <p>Es lo que hace que `Logger.getLogger("a.b.c")` devuelva **el mismo** objeto dos veces, que es lo
 * que permite configurar un logger en un lado y usarlo en otro.
 *
 * <p>La otra mitad es la configuracion por propiedades, y ahi lo importante es **cuando** se aplica:
 * un `a.b.level=FINE` en el archivo alcanza a un logger que todavia no existe, porque el nivel se le
 * pone al crearlo. Sin eso la configuracion solo serviria para los loggers que ya se hubieran creado,
 * o sea para ninguno, porque el archivo se lee antes que nada.
 *
 * <p><strong>De donde sale la configuracion por omision.</strong> El JDK la lee de
 * `$JAVA_HOME/conf/logging.properties`. En este arbol no hay directorio `conf` --ni
 * `System.getProperty("java.home")` que lo encuentre--, asi que **el mismo contenido** que el JDK
 * distribuye esta escrito aca adentro. Es la unica diferencia y es de donde salen los bytes, no de
 * que dicen: `getProperty("handlers")` contesta lo mismo en los dos. Lo que si es de verdad es
 * `java.util.logging.config.file`: si esa propiedad del sistema apunta a un archivo, se lee ese.
 */
public class LogManager {

    // El contenido exacto de `conf/logging.properties` del JDK. Ver la nota de arriba.
    private static final String POR_OMISION =
            "handlers= java.util.logging.ConsoleHandler\n"
            + ".level= INFO\n"
            + "java.util.logging.FileHandler.pattern = %h/java%u.log\n"
            + "java.util.logging.FileHandler.limit = 50000\n"
            + "java.util.logging.FileHandler.count = 1\n"
            + "java.util.logging.FileHandler.maxLocks = 100\n"
            + "java.util.logging.FileHandler.formatter = java.util.logging.XMLFormatter\n"
            + "java.util.logging.ConsoleHandler.level = INFO\n"
            + "java.util.logging.ConsoleHandler.formatter = java.util.logging.SimpleFormatter\n";

    private static final LogManager UNICO = new LogManager();

    private final java.util.HashMap<String, Logger> loggers = new java.util.HashMap<String, Logger>();

    private final java.util.Properties props = new java.util.Properties();

    private final java.util.ArrayList<Runnable> oyentes = new java.util.ArrayList<Runnable>();

    protected LogManager() {
        this.cargarPorOmision();
    }

    private void cargarPorOmision() {
        try {
            this.props.load(new java.io.StringReader(POR_OMISION));
        } catch (java.io.IOException e) {
            // Leer de una cadena en memoria no puede fallar; si fallara, quedar sin configuracion es
            // preferible a no poder ni construir el gestor.
        }
    }

    /** El gestor unico. */
    public static LogManager getLogManager() {
        return UNICO;
    }

    /**
     * Registra ese logger si no habia otro con su nombre.
     *
     * @return `false` si ya habia uno -- y entonces el que se agrego se descarta
     */
    public boolean addLogger(Logger logger) {
        if (logger == null) {
            throw new NullPointerException("logger");
        }
        String nombre = logger.getName();
        if (nombre == null) {
            return false;
        }
        synchronized (this.loggers) {
            if (this.loggers.containsKey(nombre)) {
                return false;
            }
            this.loggers.put(nombre, logger);
            this.acomodarEnElArbol(logger, nombre);
        }
        this.configurar(logger, nombre);
        return true;
    }

    /**
     * Cuelga al recien llegado de su ancestro mas cercano, y le pasa los descendientes que le tocan.
     *
     * <p>La segunda mitad es la que se olvida y la que importa: los loggers no llegan en orden, asi
     * que `com.acme.db` puede existir antes que `com.acme`. Cuando aparece el intermedio hay que
     * **recolgar** a los que estaban colgados mas arriba, o el `setLevel` sobre el intermedio no
     * afectaria a nadie -- que es justo lo que uno espera que haga.
     */
    private void acomodarEnElArbol(Logger nuevo, String nombre) {
        // El ancestro mas cercano: se van sacando segmentos desde la derecha hasta encontrar uno.
        String padre = nombre;
        while (true) {
            int punto = padre.lastIndexOf('.');
            if (punto < 0) {
                break;
            }
            padre = padre.substring(0, punto);
            Logger cand = this.loggers.get(padre);
            if (cand != null) {
                nuevo.setParent(cand);
                break;
            }
        }
        if (nuevo.getParent() == null && !nombre.isEmpty()) {
            Logger raiz = this.loggers.get("");
            if (raiz != null) {
                nuevo.setParent(raiz);
            }
        }
        if (nombre.isEmpty()) {
            return;
        }
        // Y los que ahora tienen un ancestro mas cercano que el que tenian.
        String prefijo = nombre + ".";
        for (Logger otro : this.loggers.values()) {
            String n = otro.getName();
            if (n == null || otro == nuevo || !n.startsWith(prefijo)) {
                continue;
            }
            Logger p = otro.getParent();
            String actual = p == null ? "" : p.getName();
            if (actual == null) {
                actual = "";
            }
            if (actual.length() < nombre.length()) {
                otro.setParent(nuevo);
            }
        }
    }

    // Le aplica al logger recien creado lo que la configuracion diga de el. La raiz usa las claves
    // sin nombre --`.level`, `handlers`-- y los demas las suyas con el prefijo.
    private void configurar(Logger logger, String nombre) {
        String pref = nombre.isEmpty() ? "" : nombre;
        Level nivel = this.getLevelProperty(pref + ".level", null);
        if (nivel != null) {
            logger.setLevel(nivel);
        }
        String hs = this.getProperty(nombre.isEmpty() ? "handlers" : nombre + ".handlers");
        if (hs != null) {
            this.ponerManejadores(logger, hs);
        }
        String uph = this.getProperty(pref + ".useParentHandlers");
        if (uph != null) {
            logger.setUseParentHandlers(Boolean.parseBoolean(uph.trim()));
        }
    }

    private void ponerManejadores(Logger logger, String lista) {
        String[] nombres = partir(lista);
        int i = 0;
        while (i < nombres.length) {
            try {
                logger.addHandler((Handler) crear(nombres[i]));
            } catch (Exception e) {
                // Un manejador que no se puede crear no debe impedir que se creen los otros ni que el
                // programa arranque: el resto de la traza sigue funcionando sin el.
                System.err.println("Can't load log handler \"" + nombres[i] + "\": " + e);
            }
            i = i + 1;
        }
    }

    /** El logger de ese nombre, o `null`. */
    public Logger getLogger(String name) {
        synchronized (this.loggers) {
            return this.loggers.get(name);
        }
    }

    /** Los nombres registrados. */
    public java.util.Enumeration<String> getLoggerNames() {
        synchronized (this.loggers) {
            return java.util.Collections.enumeration(
                    new java.util.ArrayList<String>(this.loggers.keySet()));
        }
    }

    // ---- la vista de administracion --------------------------------------------------------------

    /**
     * El nombre con el que se registra el {@link LoggingMXBean} en un servidor MBean.
     *
     * <p>Es una cadena, no un `ObjectName`: el tipo que la sabe interpretar vive en JMX y aca no
     * esta. Que no haya donde registrarlo no cambia cual es el nombre, asi que la constante dice lo
     * mismo que en el JDK.
     */
    public static final String LOGGING_MXBEAN_NAME = "java.util.logging:type=Logging";

    // Uno solo, y perezoso: el JDK devuelve siempre la misma instancia y hay codigo que compara con
    // `==` para saber si ya lo tenia.
    private static LoggingMXBean bean;

    /**
     * La vista de administracion del arbol de loggers.
     *
     * <p>Lo que el JDK deja para JMX es **publicar** este objeto; responder sus cuatro preguntas es
     * cosa de este registro y de nadie mas, asi que se responden. Ver {@link LoggingMXBean} para por
     * que la interfaz se puede traer sin `java.lang.management`.
     *
     * @deprecated Junto con {@link LoggingMXBean}.
     */
    @Deprecated(since = "9")
    public static synchronized LoggingMXBean getLoggingMXBean() {
        if (bean == null) {
            bean = new Administracion();
        }
        return bean;
    }

    /**
     * La implementacion de {@link LoggingMXBean}: toda pregunta se contesta mirando el registro.
     *
     * <p>El unico cuidado esta en no crear loggers sin querer. Las cuatro operaciones usan
     * {@link #getLogger} --que devuelve `null` si no esta-- y nunca `Logger.getLogger`, que lo
     * crearia. Preguntar por el nivel de algo que no existe tiene que poder contestar "no existe"; si
     * la pregunta lo creara, la respuesta nunca seria esa.
     */
    private static final class Administracion implements LoggingMXBean {

        @Override
        public java.util.List<String> getLoggerNames() {
            return java.util.Collections.list(LogManager.getLogManager().getLoggerNames());
        }

        @Override
        public String getLoggerLevel(String loggerName) {
            Logger l = LogManager.getLogManager().getLogger(loggerName);
            if (l == null) {
                return null;
            }
            Level n = l.getLevel();
            // Vacio y no `null`: `null` ya significa "no hay tal logger" y son dos cosas distintas.
            return n == null ? "" : n.getName();
        }

        @Override
        public void setLoggerLevel(String loggerName, String levelName) {
            Logger l = LogManager.getLogManager().getLogger(loggerName);
            if (l == null) {
                throw new IllegalArgumentException("logger desconocido: " + loggerName);
            }
            // `null` no es un error sino la forma de sacarle el nivel propio y volver a heredar; por
            // eso se pasa tal cual en vez de rechazarlo. Un nombre que no es un nivel si es error, y
            // `Level.parse` ya tira `IllegalArgumentException`.
            l.setLevel(levelName == null ? null : Level.parse(levelName));
        }

        @Override
        public String getParentLoggerName(String loggerName) {
            Logger l = LogManager.getLogManager().getLogger(loggerName);
            if (l == null) {
                return null;
            }
            Logger p = l.getParent();
            // La raiz no tiene padre y contesta `""`, otra vez para dejarle `null` al caso de arriba.
            return p == null ? "" : p.getName();
        }
    }

    /**
     * Deja el registro sin configuracion: sin propiedades, sin manejadores y sin niveles propios.
     *
     * <p>La raiz es la excepcion y queda en {@link Level#INFO} en vez de en `null`. Tiene que quedar
     * en algo: es la unica que no tiene de quien heredar, y dejarla en `null` haria que el nivel
     * efectivo saliera de un valor por omision escondido en vez de de un nivel que se puede leer.
     */
    public void reset() {
        synchronized (this.props) {
            this.props.clear();
        }
        synchronized (this.loggers) {
            for (Logger l : this.loggers.values()) {
                Handler[] hs = l.getHandlers();
                int i = 0;
                while (i < hs.length) {
                    try {
                        hs[i].close();
                    } catch (Exception e) {
                        // Cerrar un manejador no debe impedir cerrar los demas.
                    }
                    l.removeHandler(hs[i]);
                    i = i + 1;
                }
                String n = l.getName();
                l.setLevel(n != null && n.isEmpty() ? Level.INFO : null);
            }
        }
    }

    /** El valor de esa propiedad de configuracion, o `null` si no esta. */
    public String getProperty(String name) {
        synchronized (this.props) {
            return this.props.getProperty(name);
        }
    }

    /**
     * Antes verificaba el permiso {@link LoggingPermission}; ahora no hace nada.
     *
     * <p>No es una omision: lo que verificaba era el gestor de seguridad, que ya no puede existir.
     * Sin gestor no hay a quien preguntarle, y "no hay quien lo prohiba" es exactamente pasar.
     *
     * @deprecated Junto con el gestor de seguridad.
     */
    @Deprecated(since = "17", forRemoval = true)
    public void checkAccess() throws SecurityException {
    }

    // ---- la configuracion ------------------------------------------------------------------------

    /**
     * Relee la configuracion de donde salga por omision, tirando la que hubiera.
     *
     * <p>Con `java.util.logging.config.class`, esa clase se instancia y se hace responsable de
     * configurar; con `java.util.logging.config.file`, se lee ese archivo; sin ninguna de las dos, la
     * configuracion incorporada.
     */
    public void readConfiguration() throws java.io.IOException, SecurityException {
        String clase = System.getProperty("java.util.logging.config.class");
        if (clase != null) {
            try {
                // Instanciarla ES la configuracion: se espera que su constructor llame a
                // `readConfiguration(InputStream)` con lo que sea que ella sepa leer.
                crear(clase);
                return;
            } catch (Exception e) {
                System.err.println("Logging configuration class \"" + clase + "\" failed: " + e);
            }
        }
        String archivo = System.getProperty("java.util.logging.config.file");
        if (archivo != null) {
            java.io.InputStream in = new java.io.FileInputStream(archivo);
            try {
                this.readConfiguration(in);
            } finally {
                in.close();
            }
            return;
        }
        this.reset();
        synchronized (this.props) {
            this.cargarPorOmision();
        }
        this.aplicar();
        this.avisar();
    }

    /**
     * Relee la configuracion de ese flujo, tirando la que hubiera.
     *
     * <p>El {@link #reset} va primero y es lo que hace que esto sea "releer" y no "agregar": una
     * configuracion nueva que dejara vivos los manejadores de la anterior duplicaria cada mensaje.
     */
    public void readConfiguration(java.io.InputStream ins)
            throws java.io.IOException, SecurityException {
        if (ins == null) {
            throw new NullPointerException("ins");
        }
        java.util.Properties nuevas = new java.util.Properties();
        nuevas.load(ins);
        this.reset();
        synchronized (this.props) {
            for (String k : nuevas.stringPropertyNames()) {
                this.props.setProperty(k, nuevas.getProperty(k));
            }
        }
        this.aplicar();
        this.avisar();
    }

    // Le pasa la configuracion actual a todos los loggers que ya existen, y corre las clases de
    // `config`. Los que todavia no existen la reciben al crearse, en `addLogger`.
    private void aplicar() {
        String cfg = this.getProperty("config");
        if (cfg != null) {
            String[] clases = partir(cfg);
            int i = 0;
            while (i < clases.length) {
                try {
                    crear(clases[i]);
                } catch (Exception e) {
                    System.err.println("Can't load config class \"" + clases[i] + "\": " + e);
                }
                i = i + 1;
            }
        }
        java.util.ArrayList<Logger> copia;
        synchronized (this.loggers) {
            copia = new java.util.ArrayList<Logger>(this.loggers.values());
        }
        for (Logger l : copia) {
            String n = l.getName();
            if (n != null) {
                this.configurar(l, n);
            }
        }
    }

    /**
     * Mezcla esa configuracion con la que hay, decidiendo clave por clave con `mapper`.
     *
     * <p>La diferencia con {@link #readConfiguration(java.io.InputStream)} es que aca **no** hay
     * `reset`: lo que la configuracion nueva no menciona sigue como estaba. Es lo que hace falta para
     * cambiar el nivel de un logger en un programa que ya esta corriendo sin tirarle abajo los
     * manejadores, que es cuando uno quiere subir el detalle para mirar algo.
     *
     * <p>`mapper` recibe el nombre de cada propiedad --de la union de la vieja y la nueva-- y devuelve
     * una funcion de (valor viejo, valor nuevo) al valor que queda; `null` borra la propiedad. Un
     * `mapper` nulo equivale a quedarse con el nuevo.
     */
    public void updateConfiguration(java.util.function.Function<String,
            java.util.function.BiFunction<String, String, String>> mapper)
            throws java.io.IOException {
        this.updateConfiguration(null, mapper);
    }

    /** El de arriba, con la configuracion nueva leida de ese flujo. */
    public void updateConfiguration(java.io.InputStream ins,
            java.util.function.Function<String,
                    java.util.function.BiFunction<String, String, String>> mapper)
            throws java.io.IOException {
        java.util.Properties nuevas = new java.util.Properties();
        if (ins != null) {
            nuevas.load(ins);
        } else {
            String archivo = System.getProperty("java.util.logging.config.file");
            if (archivo != null) {
                java.io.InputStream in = new java.io.FileInputStream(archivo);
                try {
                    nuevas.load(in);
                } finally {
                    in.close();
                }
            } else {
                nuevas.load(new java.io.StringReader(POR_OMISION));
            }
        }

        java.util.HashSet<String> claves = new java.util.HashSet<String>();
        java.util.Properties viejas;
        synchronized (this.props) {
            viejas = new java.util.Properties();
            for (String k : this.props.stringPropertyNames()) {
                viejas.setProperty(k, this.props.getProperty(k));
                claves.add(k);
            }
        }
        for (String k : nuevas.stringPropertyNames()) {
            claves.add(k);
        }

        java.util.Properties resultado = new java.util.Properties();
        for (String k : claves) {
            String viejo = viejas.getProperty(k);
            String nuevo = nuevas.getProperty(k);
            String queda = nuevo;
            if (mapper != null) {
                java.util.function.BiFunction<String, String, String> f = mapper.apply(k);
                queda = f == null ? nuevo : f.apply(viejo, nuevo);
            }
            if (queda != null) {
                resultado.setProperty(k, queda);
            }
        }

        synchronized (this.props) {
            this.props.clear();
            for (String k : resultado.stringPropertyNames()) {
                this.props.setProperty(k, resultado.getProperty(k));
            }
        }

        // Solo se toca lo que **cambio**. Un logger cuyo `.level` no aparecio en ningun lado se queda
        // con el que tenia, que es el sentido entero de que esto no sea un `readConfiguration`.
        java.util.ArrayList<Logger> copia;
        synchronized (this.loggers) {
            copia = new java.util.ArrayList<Logger>(this.loggers.values());
        }
        for (Logger l : copia) {
            String n = l.getName();
            if (n == null) {
                continue;
            }
            // El nivel se toca **solo si** la configuracion nueva lo dice. Que una propiedad
            // desaparezca no significa "volve a heredar": el nivel puede haberlo puesto el programa
            // por codigo, y una actualizacion que no habla del asunto no tiene por que pisarlo.
            String claveNivel = n + ".level";
            String nivelNuevo = resultado.getProperty(claveNivel);
            if (nivelNuevo != null && !igual(viejas.getProperty(claveNivel), nivelNuevo)) {
                Level lv = this.getLevelProperty(claveNivel, null);
                if (lv != null) {
                    l.setLevel(lv);
                }
            }
            String claveH = n.isEmpty() ? "handlers" : n + ".handlers";
            if (!igual(viejas.getProperty(claveH), resultado.getProperty(claveH))) {
                Handler[] hs = l.getHandlers();
                int i = 0;
                while (i < hs.length) {
                    try {
                        hs[i].close();
                    } catch (Exception e) {
                        // Ver `reset`.
                    }
                    l.removeHandler(hs[i]);
                    i = i + 1;
                }
                String lista = resultado.getProperty(claveH);
                if (lista != null) {
                    this.ponerManejadores(l, lista);
                }
            }
            String claveU = n + ".useParentHandlers";
            if (!igual(viejas.getProperty(claveU), resultado.getProperty(claveU))) {
                String v = resultado.getProperty(claveU);
                l.setUseParentHandlers(v == null || Boolean.parseBoolean(v.trim()));
            }
        }
        this.avisar();
    }

    private static boolean igual(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    /**
     * Agrega un oyente que corre cada vez que la configuracion se relee o se actualiza.
     *
     * <p>Es lo que necesita el codigo que **deriva** algo de la configuracion --un `boolean` cacheado
     * de si la traza fina esta prendida-- para enterarse de que ese algo quedo viejo.
     *
     * @return este mismo gestor, para encadenar
     * @throws NullPointerException si `listener` es `null`
     */
    public LogManager addConfigurationListener(Runnable listener) {
        if (listener == null) {
            throw new NullPointerException("listener");
        }
        synchronized (this.oyentes) {
            this.oyentes.add(listener);
        }
        return this;
    }

    /** Saca un oyente; si no estaba, no pasa nada. */
    public void removeConfigurationListener(Runnable listener) {
        if (listener == null) {
            throw new NullPointerException("listener");
        }
        synchronized (this.oyentes) {
            this.oyentes.remove(listener);
        }
    }

    private void avisar() {
        java.util.ArrayList<Runnable> copia;
        synchronized (this.oyentes) {
            copia = new java.util.ArrayList<Runnable>(this.oyentes);
        }
        for (Runnable r : copia) {
            try {
                r.run();
            } catch (Exception e) {
                // Un oyente que falla no puede impedir que corran los demas ni invalidar la
                // configuracion, que ya esta aplicada.
            }
        }
    }

    // ---- lo que leen los manejadores -------------------------------------------------------------
    //
    // No son API publica --en el JDK tampoco--: son los accesos con conversion y valor por omision
    // que cada manejador usa para leer su propia configuracion. Todos comparten la misma regla: si la
    // propiedad falta o no se puede convertir, vale el valor por omision. Un `.level=CUALQUIERA` mal
    // escrito no puede tumbar el arranque del programa.

    Level getLevelProperty(String name, Level porOmision) {
        String v = this.getProperty(name);
        if (v == null) {
            return porOmision;
        }
        try {
            return Level.parse(v.trim());
        } catch (Exception e) {
            return porOmision;
        }
    }

    int getIntProperty(String name, int porOmision) {
        String v = this.getProperty(name);
        if (v == null) {
            return porOmision;
        }
        try {
            return Integer.parseInt(v.trim());
        } catch (Exception e) {
            return porOmision;
        }
    }

    long getLongProperty(String name, long porOmision) {
        String v = this.getProperty(name);
        if (v == null) {
            return porOmision;
        }
        try {
            return Long.parseLong(v.trim());
        } catch (Exception e) {
            return porOmision;
        }
    }

    boolean getBooleanProperty(String name, boolean porOmision) {
        String v = this.getProperty(name);
        if (v == null) {
            return porOmision;
        }
        v = v.toLowerCase().trim();
        if (v.equals("true") || v.equals("1")) {
            return true;
        }
        if (v.equals("false") || v.equals("0")) {
            return false;
        }
        return porOmision;
    }

    String getStringProperty(String name, String porOmision) {
        String v = this.getProperty(name);
        return v == null ? porOmision : v.trim();
    }

    Filter getFilterProperty(String name, Filter porOmision) {
        String v = this.getProperty(name);
        if (v == null) {
            return porOmision;
        }
        try {
            return (Filter) crear(v.trim());
        } catch (Exception e) {
            return porOmision;
        }
    }

    Formatter getFormatterProperty(String name, Formatter porOmision) {
        String v = this.getProperty(name);
        if (v == null) {
            return porOmision;
        }
        try {
            return (Formatter) crear(v.trim());
        } catch (Exception e) {
            return porOmision;
        }
    }

    // Una instancia de esa clase por su constructor sin argumentos.
    static Object crear(String nombreClase) throws Exception {
        Class<?> c = Class.forName(nombreClase);
        return c.getDeclaredConstructor().newInstance();
    }

    // La configuracion separa las listas por espacios o por comas, indistintamente.
    private static String[] partir(String lista) {
        java.util.ArrayList<String> salida = new java.util.ArrayList<String>();
        int i = 0;
        StringBuilder actual = new StringBuilder();
        while (i < lista.length()) {
            char c = lista.charAt(i);
            if (c == ',' || c == ' ' || c == '\t') {
                if (actual.length() > 0) {
                    salida.add(actual.toString());
                    actual.setLength(0);
                }
            } else {
                actual.append(c);
            }
            i = i + 1;
        }
        if (actual.length() > 0) {
            salida.add(actual.toString());
        }
        return salida.toArray(new String[salida.size()]);
    }
}
