package java.util.logging;

/**
 * KajiLibrary's java.util.logging.Logger -- por donde se emiten los mensajes.
 *
 * <p>Los loggers forman un **arbol por el punto del nombre**: `com.acme.db` es hijo de `com.acme`,
 * que es hijo de la raiz. De ahi salen las dos cosas que hay que entender.
 *
 * <p>La primera es que el nivel se **hereda**: un logger sin nivel propio usa el del padre, asi que
 * poner `com.acme` en `FINE` afecta a todos sus descendientes sin nombrarlos. La segunda es que los
 * mensajes **suben**: un registro que pasa el filtro se publica en los manejadores de este logger y
 * despues en los del padre, y asi hasta la raiz, salvo que alguno corte con
 * {@link #setUseParentHandlers}. Por eso alcanza con poner **un** manejador en la raiz para ver todo.
 *
 * <p>Las variantes con {@link java.util.function.Supplier} existen por el costo: armar el mensaje
 * cuesta aunque despues se descarte, y un `log(FINE, () -> caro())` no evalua nada si `FINE` no esta
 * habilitado. Es la unica manera de tener traza fina sin pagarla cuando esta apagada.
 *
 * <p>La localizacion tambien se hereda por el arbol, y con una vuelta de tuerca: el catalogo que se
 * usa al emitir es el del ancestro mas cercano que tenga uno, pero {@link #getResourceBundle}
 * devuelve **solo el propio**. No es una inconsistencia -- una cosa es que catalogo se aplica y otra
 * cual configuro este logger, y confundirlas haria imposible saber si hay que configurarlo.
 */
public class Logger {

    /** El nombre del logger que registra las llamadas al sistema global. */
    public static final String GLOBAL_LOGGER_NAME = "global";

    /**
     * El logger global. Existe para ejemplos y para codigo desechable, no para una aplicacion.
     *
     * @deprecated Un campo `static final` publico se inicializa cuando la clase se inicializa, y eso
     *             pasa mas temprano de lo que uno cree: leerlo desde el arranque de la propia
     *             infraestructura de traza podia dar `null` a mitad de la inicializacion. Por eso el
     *             JDK agrego {@link #getGlobal()}, que es un metodo y por lo tanto no tiene ese
     *             problema.
     */
    @Deprecated
    public static final Logger global = Logger.getLogger(GLOBAL_LOGGER_NAME);

    /** El logger global, sin el problema de inicializacion del campo {@link #global}. */
    public static final Logger getGlobal() {
        return global;
    }

    private final String name;
    private volatile String resourceBundleName;
    private volatile java.util.ResourceBundle bundle;
    private volatile Level level;
    private volatile Logger parent;
    private volatile boolean useParentHandlers = true;
    private volatile Filter filter;
    private final java.util.ArrayList<Handler> handlers = new java.util.ArrayList<Handler>();

    protected Logger(String name, String resourceBundleName) {
        this.name = name;
        this.resourceBundleName = resourceBundleName;
    }

    // ---- obtenerlos ------------------------------------------------------------------------------------

    /**
     * El logger de ese nombre, creandolo si no existe.
     *
     * <p>Devuelve **el mismo** objeto para el mismo nombre, que es lo que permite configurarlo en un
     * lado y usarlo en otro.
     */
    public static Logger getLogger(String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        LogManager m = LogManager.getLogManager();
        Logger ya = m.getLogger(name);
        if (ya != null) {
            return ya;
        }
        Logger nuevo = new Logger(name, null);
        if (!name.isEmpty()) {
            // Se fuerza a que la raiz exista **antes** de registrar: `addLogger` cuelga al recien
            // llegado de su ancestro mas cercano, y sin raiz no habria de que colgarlo.
            raiz();
        }
        if (!m.addLogger(nuevo)) {
            // Otro hilo gano la carrera: vale el suyo, para que la identidad por nombre se mantenga.
            return m.getLogger(name);
        }
        return nuevo;
    }

    /**
     * El de arriba, con el nombre del catalogo con el que se traducen los mensajes.
     *
     * <p>Si el logger ya existia **sin** catalogo, se le pone este. Si ya tenia **otro**, es un error:
     * dos partes del programa que pidieron el mismo logger con catalogos distintos no se pueden
     * conformar las dos, y elegir en silencio le rompe la traduccion a una de ellas sin avisarle.
     *
     * @throws java.util.MissingResourceException si el catalogo no se encuentra -- se busca al pedirlo y no al
     *         emitir, que es cuando todavia se puede hacer algo al respecto
     * @throws IllegalArgumentException si el logger ya tenia otro catalogo
     */
    public static Logger getLogger(String name, String resourceBundleName) {
        Logger l = getLogger(name);
        if (resourceBundleName != null) {
            String ya = l.resourceBundleName;
            if (ya == null) {
                // Se carga primero: si no existe, el logger queda como estaba y no a medio configurar.
                l.bundle = java.util.ResourceBundle.getBundle(resourceBundleName);
                l.resourceBundleName = resourceBundleName;
            } else if (!ya.equals(resourceBundleName)) {
                throw new IllegalArgumentException(ya + " != " + resourceBundleName);
            }
        }
        return l;
    }

    /**
     * Un logger **sin nombre**, que no se registra en ningun lado.
     *
     * <p>Sirve justamente para lo contrario que {@link #getLogger}: como nadie mas lo puede
     * encontrar, nadie mas lo puede reconfigurar. Es lo que corresponde para una biblioteca que no
     * quiere que su traza dependa de la configuracion global.
     */
    public static Logger getAnonymousLogger() {
        return getAnonymousLogger(null);
    }

    /**
     * El de arriba, con catalogo.
     *
     * @throws java.util.MissingResourceException si el catalogo no se encuentra
     */
    public static Logger getAnonymousLogger(String resourceBundleName) {
        Logger l = new Logger(null, resourceBundleName);
        if (resourceBundleName != null) {
            l.bundle = java.util.ResourceBundle.getBundle(resourceBundleName);
        }
        l.parent = raiz();
        return l;
    }

    private static Logger raiz() {
        LogManager m = LogManager.getLogManager();
        Logger r = m.getLogger("");
        if (r != null) {
            return r;
        }
        Logger nueva = new Logger("", null);
        // Un nivel de arranque por si la configuracion no dice nada: la raiz es la unica que no tiene
        // de quien heredar. Sus manejadores y su nivel definitivo salen de la configuracion, que
        // `addLogger` le aplica.
        nueva.level = Level.INFO;
        if (!m.addLogger(nueva)) {
            return m.getLogger("");
        }
        return nueva;
    }

    // ---- configuracion ----------------------------------------------------------------------------------

    /** El nombre, o `null` si es anonimo. */
    public String getName() {
        return this.name;
    }

    public String getResourceBundleName() {
        return this.resourceBundleName;
    }

    /**
     * El catalogo **propio**, o `null` si no tiene uno.
     *
     * <p>`null` no significa que los mensajes no se traduzcan: si un ancestro tiene catalogo, es el
     * que se usa. Lo que este metodo contesta es si **este** logger fue configurado, que es otra
     * pregunta.
     */
    public java.util.ResourceBundle getResourceBundle() {
        return this.bundle;
    }

    /**
     * Fija el catalogo propio.
     *
     * <p>El catalogo tiene que tener nombre base porque el nombre es lo que viaja: un registro
     * serializado lleva el nombre y no el objeto, y un catalogo anonimo daria un registro que del otro
     * lado no se puede traducir.
     *
     * @throws NullPointerException si `bundle` es `null`
     * @throws IllegalArgumentException si el catalogo no tiene nombre base, o si este logger ya tenia
     *         otro catalogo -- por lo mismo que {@link #getLogger(String, String)}
     */
    public void setResourceBundle(java.util.ResourceBundle bundle) {
        if (bundle == null) {
            throw new NullPointerException("bundle");
        }
        String base = bundle.getBaseBundleName();
        if (base == null || base.isEmpty()) {
            throw new IllegalArgumentException("resource bundle must have a name");
        }
        String ya = this.resourceBundleName;
        if (ya != null && !ya.equals(base)) {
            throw new IllegalArgumentException("can't replace resource bundle");
        }
        this.bundle = bundle;
        this.resourceBundleName = base;
    }

    /** El nivel propio, o `null` si hereda el del padre. */
    public Level getLevel() {
        return this.level;
    }

    /** Fija el nivel; `null` para volver a heredar. */
    public void setLevel(Level newLevel) throws SecurityException {
        this.level = newLevel;
    }

    public Logger getParent() {
        return this.parent;
    }

    public void setParent(Logger parent) {
        if (parent == null) {
            throw new NullPointerException("parent");
        }
        this.parent = parent;
    }

    /** Si los mensajes tambien van a los manejadores del padre. */
    public boolean getUseParentHandlers() {
        return this.useParentHandlers;
    }

    public void setUseParentHandlers(boolean useParentHandlers) throws SecurityException {
        this.useParentHandlers = useParentHandlers;
    }

    public Filter getFilter() {
        return this.filter;
    }

    public void setFilter(Filter newFilter) throws SecurityException {
        this.filter = newFilter;
    }

    public void addHandler(Handler handler) throws SecurityException {
        if (handler == null) {
            throw new NullPointerException("handler");
        }
        synchronized (this.handlers) {
            this.handlers.add(handler);
        }
    }

    public void removeHandler(Handler handler) throws SecurityException {
        if (handler == null) {
            return;
        }
        synchronized (this.handlers) {
            this.handlers.remove(handler);
        }
    }

    public Handler[] getHandlers() {
        synchronized (this.handlers) {
            return this.handlers.toArray(new Handler[this.handlers.size()]);
        }
    }

    /**
     * Si un mensaje de ese nivel se registraria.
     *
     * <p>Vale la pena preguntarlo antes de armar un mensaje caro -- o usar la variante con
     * {@link java.util.function.Supplier}, que lo hace sola.
     */
    public boolean isLoggable(Level level) {
        int propio = this.nivelEfectivo().intValue();
        if (propio == Level.OFF.intValue()) {
            return false;
        }
        return level.intValue() >= propio;
    }

    // El primer nivel propio subiendo por el arbol; `INFO` si no hay ninguno.
    private Level nivelEfectivo() {
        Logger l = this;
        while (l != null) {
            Level n = l.level;
            if (n != null) {
                return n;
            }
            l = l.parent;
        }
        return Level.INFO;
    }

    // ---- emitir -----------------------------------------------------------------------------------------

    /**
     * Publica ese registro: en los manejadores propios y, si corresponde, en los del padre.
     *
     * <p>Todos los demas metodos terminan aca -- pero **no** al reves: este toma el registro tal como
     * viene y no le pone ni el nombre del logger ni el catalogo. Es lo que corresponde para el unico
     * metodo al que se le entrega un {@link LogRecord} ya armado: quien lo armo decidio que dice.
     */
    public void log(LogRecord record) {
        if (record == null || !this.isLoggable(record.getLevel())) {
            return;
        }
        Filter f = this.filter;
        if (f != null && !f.isLoggable(record)) {
            return;
        }
        Logger l = this;
        while (l != null) {
            Handler[] hs = l.getHandlers();
            int i = 0;
            while (i < hs.length) {
                hs[i].publish(record);
                i = i + 1;
            }
            if (!l.useParentHandlers) {
                return;
            }
            l = l.parent;
        }
    }

    /**
     * Le pone al registro lo que sale de **este** logger y despues lo publica.
     *
     * <p>Es por donde pasan todos los metodos de conveniencia, y la razon de que exista es que
     * {@link #log(LogRecord)} no debe hacer esto: un registro que el llamador armo ya dice lo que
     * tiene que decir.
     */
    private void doLog(LogRecord record) {
        record.setLoggerName(this.name);
        java.util.ResourceBundle rb = this.catalogoEfectivo();
        if (rb != null) {
            record.setResourceBundle(rb);
            record.setResourceBundleName(rb.getBaseBundleName());
        }
        this.log(record);
    }

    // El de arriba con un catalogo dicho por nombre, para los `logrb` deprecados. Un nombre que no
    // resuelve deja el registro con el nombre puesto y sin catalogo: es la verdad --se pidio esa
    // traduccion y no se encontro-- y el formateador cae al mensaje crudo, que es lo unico que queda.
    private void doLog(LogRecord record, String rbname) {
        record.setLoggerName(this.name);
        if (rbname != null) {
            record.setResourceBundleName(rbname);
            record.setResourceBundle(porNombre(rbname));
        }
        this.log(record);
    }

    private void doLog(LogRecord record, java.util.ResourceBundle rb) {
        record.setLoggerName(this.name);
        if (rb != null) {
            record.setResourceBundle(rb);
            record.setResourceBundleName(rb.getBaseBundleName());
        }
        this.log(record);
    }

    // El catalogo del ancestro mas cercano que tenga uno, incluido este.
    private java.util.ResourceBundle catalogoEfectivo() {
        Logger l = this;
        while (l != null) {
            java.util.ResourceBundle rb = l.bundle;
            if (rb != null) {
                return rb;
            }
            l = l.parent;
        }
        return null;
    }

    private static java.util.ResourceBundle porNombre(String rbname) {
        try {
            return java.util.ResourceBundle.getBundle(rbname);
        } catch (java.util.MissingResourceException e) {
            return null;
        }
    }

    public void log(Level level, String msg) {
        if (!this.isLoggable(level)) {
            return;
        }
        this.doLog(new LogRecord(level, msg));
    }

    /** El mensaje se arma **solo si** el nivel esta habilitado. */
    public void log(Level level, java.util.function.Supplier<String> msgSupplier) {
        if (!this.isLoggable(level)) {
            return;
        }
        this.doLog(new LogRecord(level, msgSupplier.get()));
    }

    public void log(Level level, String msg, Object param1) {
        if (!this.isLoggable(level)) {
            return;
        }
        LogRecord r = new LogRecord(level, msg);
        r.setParameters(new Object[] {param1});
        this.doLog(r);
    }

    public void log(Level level, String msg, Object[] params) {
        if (!this.isLoggable(level)) {
            return;
        }
        LogRecord r = new LogRecord(level, msg);
        r.setParameters(params);
        this.doLog(r);
    }

    public void log(Level level, String msg, Throwable thrown) {
        if (!this.isLoggable(level)) {
            return;
        }
        LogRecord r = new LogRecord(level, msg);
        r.setThrown(thrown);
        this.doLog(r);
    }

    public void log(Level level, Throwable thrown, java.util.function.Supplier<String> msgSupplier) {
        if (!this.isLoggable(level)) {
            return;
        }
        LogRecord r = new LogRecord(level, msgSupplier.get());
        r.setThrown(thrown);
        this.doLog(r);
    }

    // ---- con origen explicito ----------------------------------------------------------------------------
    //
    // Los `logp` reciben la clase y el metodo en vez de deducirlos. Existen para el codigo que
    // registra **en nombre de otro** -- un envoltorio, un marco de trabajo-- donde el origen deducido
    // seria el envoltorio y no lo que al lector le interesa.

    public void logp(Level level, String sourceClass, String sourceMethod, String msg) {
        if (!this.isLoggable(level)) {
            return;
        }
        this.doLog(conOrigen(level, msg, sourceClass, sourceMethod));
    }

    public void logp(Level level, String sourceClass, String sourceMethod,
            java.util.function.Supplier<String> msgSupplier) {
        if (!this.isLoggable(level)) {
            return;
        }
        this.doLog(conOrigen(level, msgSupplier.get(), sourceClass, sourceMethod));
    }

    public void logp(Level level, String sourceClass, String sourceMethod, String msg,
            Object param1) {
        if (!this.isLoggable(level)) {
            return;
        }
        LogRecord r = conOrigen(level, msg, sourceClass, sourceMethod);
        r.setParameters(new Object[] {param1});
        this.doLog(r);
    }

    public void logp(Level level, String sourceClass, String sourceMethod, String msg,
            Object[] params) {
        if (!this.isLoggable(level)) {
            return;
        }
        LogRecord r = conOrigen(level, msg, sourceClass, sourceMethod);
        r.setParameters(params);
        this.doLog(r);
    }

    public void logp(Level level, String sourceClass, String sourceMethod, String msg,
            Throwable thrown) {
        if (!this.isLoggable(level)) {
            return;
        }
        LogRecord r = conOrigen(level, msg, sourceClass, sourceMethod);
        r.setThrown(thrown);
        this.doLog(r);
    }

    public void logp(Level level, String sourceClass, String sourceMethod, Throwable thrown,
            java.util.function.Supplier<String> msgSupplier) {
        if (!this.isLoggable(level)) {
            return;
        }
        LogRecord r = conOrigen(level, msgSupplier.get(), sourceClass, sourceMethod);
        r.setThrown(thrown);
        this.doLog(r);
    }

    // ---- con catalogo explicito ---------------------------------------------------------------------------
    //
    // Los `logrb` traducen con **este** catalogo en vez de con el del logger. Es lo que necesita una
    // biblioteca que emite por un logger de la aplicacion --para que la configuracion de la
    // aplicacion la alcance-- pero cuyos mensajes estan en su propio catalogo, no en el de ella.
    //
    // Las cuatro formas que reciben el catalogo por **nombre** estan deprecadas, y con razon: un
    // nombre se resuelve contra un cargador de clases que en el momento de emitir puede no ser el que
    // uno cree. Pasar el objeto no tiene esa ambiguedad.

    /** @deprecated Usar la forma que recibe el {@link java.util.ResourceBundle}. */
    @Deprecated
    public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName,
            String msg) {
        if (!this.isLoggable(level)) {
            return;
        }
        this.doLog(conOrigen(level, msg, sourceClass, sourceMethod), bundleName);
    }

    /** @deprecated Usar la forma que recibe el {@link java.util.ResourceBundle}. */
    @Deprecated
    public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName,
            String msg, Object param1) {
        if (!this.isLoggable(level)) {
            return;
        }
        LogRecord r = conOrigen(level, msg, sourceClass, sourceMethod);
        r.setParameters(new Object[] {param1});
        this.doLog(r, bundleName);
    }

    /** @deprecated Usar la forma que recibe el {@link java.util.ResourceBundle}. */
    @Deprecated
    public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName,
            String msg, Object[] params) {
        if (!this.isLoggable(level)) {
            return;
        }
        LogRecord r = conOrigen(level, msg, sourceClass, sourceMethod);
        r.setParameters(params);
        this.doLog(r, bundleName);
    }

    /** @deprecated Usar la forma que recibe el {@link java.util.ResourceBundle}. */
    @Deprecated
    public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName,
            String msg, Throwable thrown) {
        if (!this.isLoggable(level)) {
            return;
        }
        LogRecord r = conOrigen(level, msg, sourceClass, sourceMethod);
        r.setThrown(thrown);
        this.doLog(r, bundleName);
    }

    public void logrb(Level level, String sourceClass, String sourceMethod,
            java.util.ResourceBundle bundle, String msg, Object... params) {
        if (!this.isLoggable(level)) {
            return;
        }
        LogRecord r = conOrigen(level, msg, sourceClass, sourceMethod);
        if (params != null && params.length != 0) {
            r.setParameters(params);
        }
        this.doLog(r, bundle);
    }

    public void logrb(Level level, String sourceClass, String sourceMethod,
            java.util.ResourceBundle bundle, String msg, Throwable thrown) {
        if (!this.isLoggable(level)) {
            return;
        }
        LogRecord r = conOrigen(level, msg, sourceClass, sourceMethod);
        r.setThrown(thrown);
        this.doLog(r, bundle);
    }

    public void logrb(Level level, java.util.ResourceBundle bundle, String msg, Object... params) {
        if (!this.isLoggable(level)) {
            return;
        }
        LogRecord r = new LogRecord(level, msg);
        if (params != null && params.length != 0) {
            r.setParameters(params);
        }
        this.doLog(r, bundle);
    }

    public void logrb(Level level, java.util.ResourceBundle bundle, String msg, Throwable thrown) {
        if (!this.isLoggable(level)) {
            return;
        }
        LogRecord r = new LogRecord(level, msg);
        r.setThrown(thrown);
        this.doLog(r, bundle);
    }

    private static LogRecord conOrigen(Level level, String msg, String clase, String metodo) {
        LogRecord r = new LogRecord(level, msg);
        r.setSourceClassName(clase);
        r.setSourceMethodName(metodo);
        return r;
    }

    // ---- entrada y salida de metodo ----------------------------------------------------------------------
    //
    // Todos en `FINER`, y con mensajes fijos (`ENTRY`, `RETURN`, `THROW`) para que una herramienta
    // pueda reconocerlos sin parsear.

    public void entering(String sourceClass, String sourceMethod) {
        this.logp(Level.FINER, sourceClass, sourceMethod, "ENTRY");
    }

    public void entering(String sourceClass, String sourceMethod, Object param1) {
        this.logp(Level.FINER, sourceClass, sourceMethod, "ENTRY {0}", param1);
    }

    public void entering(String sourceClass, String sourceMethod, Object[] params) {
        if (!this.isLoggable(Level.FINER)) {
            return;
        }
        StringBuilder sb = new StringBuilder("ENTRY");
        int i = 0;
        while (i < (params == null ? 0 : params.length)) {
            sb.append(" {");
            sb.append(i);
            sb.append('}');
            i = i + 1;
        }
        this.logp(Level.FINER, sourceClass, sourceMethod, sb.toString(), params);
    }

    public void exiting(String sourceClass, String sourceMethod) {
        this.logp(Level.FINER, sourceClass, sourceMethod, "RETURN");
    }

    public void exiting(String sourceClass, String sourceMethod, Object result) {
        this.logp(Level.FINER, sourceClass, sourceMethod, "RETURN {0}", result);
    }

    /**
     * Registra que el metodo salio lanzando.
     *
     * <p>En `FINER` y no en `SEVERE`, aunque haya una excepcion: no es un fallo del programa sino la
     * traza de un metodo que termino asi, y quien atrape la excepcion decidira si es grave.
     */
    public void throwing(String sourceClass, String sourceMethod, Throwable thrown) {
        if (!this.isLoggable(Level.FINER)) {
            return;
        }
        LogRecord r = conOrigen(Level.FINER, "THROW", sourceClass, sourceMethod);
        r.setThrown(thrown);
        this.doLog(r);
    }

    // ---- atajos por nivel ---------------------------------------------------------------------------------

    public void severe(String msg) {
        this.log(Level.SEVERE, msg);
    }

    public void warning(String msg) {
        this.log(Level.WARNING, msg);
    }

    public void info(String msg) {
        this.log(Level.INFO, msg);
    }

    public void config(String msg) {
        this.log(Level.CONFIG, msg);
    }

    public void fine(String msg) {
        this.log(Level.FINE, msg);
    }

    public void finer(String msg) {
        this.log(Level.FINER, msg);
    }

    public void finest(String msg) {
        this.log(Level.FINEST, msg);
    }

    public void severe(java.util.function.Supplier<String> msgSupplier) {
        this.log(Level.SEVERE, msgSupplier);
    }

    public void warning(java.util.function.Supplier<String> msgSupplier) {
        this.log(Level.WARNING, msgSupplier);
    }

    public void info(java.util.function.Supplier<String> msgSupplier) {
        this.log(Level.INFO, msgSupplier);
    }

    public void config(java.util.function.Supplier<String> msgSupplier) {
        this.log(Level.CONFIG, msgSupplier);
    }

    public void fine(java.util.function.Supplier<String> msgSupplier) {
        this.log(Level.FINE, msgSupplier);
    }

    public void finer(java.util.function.Supplier<String> msgSupplier) {
        this.log(Level.FINER, msgSupplier);
    }

    public void finest(java.util.function.Supplier<String> msgSupplier) {
        this.log(Level.FINEST, msgSupplier);
    }
}
