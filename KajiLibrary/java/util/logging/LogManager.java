package java.util.logging;

/**
 * KajiLibrary's java.util.logging.LogManager -- the registry of {@link Logger}s by name, and the
 * configuration.
 *
 * <p>It is what makes `Logger.getLogger("a.b.c")` return **the same** object twice, which is what
 * lets a logger be configured in one place and used in another.
 *
 * <p>The other half is configuration by properties, and there what matters is **when** it is
 * applied: an `a.b.level=FINE` in the file reaches a logger that does not exist yet, because the
 * level is set on it as it is created. Without that the configuration would serve only the loggers
 * already created, which is to say none, because the file is read before anything.
 *
 * <p><strong>Where the default configuration comes from.</strong> The JDK reads it from
 * `$JAVA_HOME/conf/logging.properties`. In this tree there is no `conf` directory --nor a
 * `System.getProperty("java.home")` that would find it-- so **the same content** the JDK ships is
 * written in here. That is the only difference and it is about where the bytes come from, not about
 * what they say: `getProperty("handlers")` answers the same in both. What IS real is
 * `java.util.logging.config.file`: if that system property points at a file, that file is read.
 */
public class LogManager {

    // The exact content of the JDK's `conf/logging.properties`. See the note above.
    private static final String DEFAULT_CONFIG =
            "handlers= java.util.logging.ConsoleHandler\n"
            + ".level= INFO\n"
            + "java.util.logging.FileHandler.pattern = %h/java%u.log\n"
            + "java.util.logging.FileHandler.limit = 50000\n"
            + "java.util.logging.FileHandler.count = 1\n"
            + "java.util.logging.FileHandler.maxLocks = 100\n"
            + "java.util.logging.FileHandler.formatter = java.util.logging.XMLFormatter\n"
            + "java.util.logging.ConsoleHandler.level = INFO\n"
            + "java.util.logging.ConsoleHandler.formatter = java.util.logging.SimpleFormatter\n";

    private static final LogManager THE_ONE = new LogManager();

    private final java.util.HashMap<String, Logger> loggers = new java.util.HashMap<String, Logger>();

    private final java.util.Properties props = new java.util.Properties();

    private final java.util.ArrayList<Runnable> listeners = new java.util.ArrayList<Runnable>();

    protected LogManager() {
        this.loadDefault();
    }

    private void loadDefault() {
        try {
            this.props.load(new java.io.StringReader(DEFAULT_CONFIG));
        } catch (java.io.IOException e) {
            // Reading from an in-memory string cannot fail; if it did, being left with no
            // configuration is preferable to not even being able to build the manager.
        }
    }

    /** The one manager. */
    public static LogManager getLogManager() {
        return THE_ONE;
    }

    /**
     * It registers that logger if there was no other by its name.
     *
     * @return `false` if there already was one -- and then the one added is discarded
     */
    public boolean addLogger(Logger logger) {
        if (logger == null) {
            throw new NullPointerException("logger");
        }
        String name = logger.getName();
        if (name == null) {
            return false;
        }
        synchronized (this.loggers) {
            if (this.loggers.containsKey(name)) {
                return false;
            }
            this.loggers.put(name, logger);
            this.placeInTree(logger, name);
        }
        this.configure(logger, name);
        return true;
    }

    /**
     * It hangs the newcomer off its nearest ancestor, and hands it the descendants that are its.
     *
     * <p>The second half is the one that gets forgotten and the one that matters: loggers do not
     * arrive in order, so `com.acme.db` can exist before `com.acme`. When the intermediate appears,
     * those hanging further up have to be **rehung**, or a `setLevel` on the intermediate would
     * affect nobody -- which is exactly what one expects it to do.
     */
    private void placeInTree(Logger newOne, String name) {
        // The nearest ancestor: segments are dropped from the right until one is found.
        String parent = name;
        while (true) {
            int dot = parent.lastIndexOf('.');
            if (dot < 0) {
                break;
            }
            parent = parent.substring(0, dot);
            Logger cand = this.loggers.get(parent);
            if (cand != null) {
                newOne.setParent(cand);
                break;
            }
        }
        if (newOne.getParent() == null && !name.isEmpty()) {
            Logger root = this.loggers.get("");
            if (root != null) {
                newOne.setParent(root);
            }
        }
        if (name.isEmpty()) {
            return;
        }
        // And those that now have a nearer ancestor than the one they had.
        String prefix = name + ".";
        for (Logger other : this.loggers.values()) {
            String n = other.getName();
            if (n == null || other == newOne || !n.startsWith(prefix)) {
                continue;
            }
            Logger p = other.getParent();
            String current = p == null ? "" : p.getName();
            if (current == null) {
                current = "";
            }
            if (current.length() < name.length()) {
                other.setParent(newOne);
            }
        }
    }

    // It applies to the newly created logger whatever the configuration says about it. The root uses
    // the unnamed keys --`.level`, `handlers`-- and the rest their own, with the prefix.
    private void configure(Logger logger, String name) {
        String pref = name.isEmpty() ? "" : name;
        Level level = this.getLevelProperty(pref + ".level", null);
        if (level != null) {
            logger.setLevel(level);
        }
        String hs = this.getProperty(name.isEmpty() ? "handlers" : name + ".handlers");
        if (hs != null) {
            this.installHandlers(logger, hs);
        }
        String uph = this.getProperty(pref + ".useParentHandlers");
        if (uph != null) {
            logger.setUseParentHandlers(Boolean.parseBoolean(uph.trim()));
        }
    }

    private void installHandlers(Logger logger, String list) {
        String[] names = splitList(list);
        int i = 0;
        while (i < names.length) {
            try {
                logger.addHandler((Handler) build(names[i]));
            } catch (Exception e) {
                // A handler that cannot be created must not stop the others being created nor the
                // program starting: the rest of the logging goes on working without it.
                System.err.println("Can't load log handler \"" + names[i] + "\": " + e);
            }
            i = i + 1;
        }
    }

    /** The logger by that name, or `null`. */
    public Logger getLogger(String name) {
        synchronized (this.loggers) {
            return this.loggers.get(name);
        }
    }

    /** The registered names. */
    public java.util.Enumeration<String> getLoggerNames() {
        synchronized (this.loggers) {
            return java.util.Collections.enumeration(
                    new java.util.ArrayList<String>(this.loggers.keySet()));
        }
    }

    // ---- the management view ---------------------------------------------------------------------

    /**
     * The name the {@link LoggingMXBean} is registered under in an MBean server.
     *
     * <p>It is a string, not an `ObjectName`: the type that knows how to read it lives in JMX and is
     * not here. That there is nowhere to register it does not change what the name is, so the
     * constant says the same as in the JDK.
     */
    public static final String LOGGING_MXBEAN_NAME = "java.util.logging:type=Logging";

    // A single one, and lazy: the JDK always returns the same instance and there is code that
    // compares with `==` to know whether it already had it.
    private static LoggingMXBean bean;

    /**
     * The management view of the logger tree.
     *
     * <p>What the JDK leaves to JMX is **publishing** this object; answering its four questions is
     * this registry's business and nobody else's, so they are answered. See {@link LoggingMXBean} for
     * why the interface can be brought in without `java.lang.management`.
     *
     * @deprecated Along with {@link LoggingMXBean}.
     */
    @Deprecated(since = "9")
    public static synchronized LoggingMXBean getLoggingMXBean() {
        if (bean == null) {
            bean = new Management();
        }
        return bean;
    }

    /**
     * {@link LoggingMXBean}'s implementation: every question is answered by looking at the registry.
     *
     * <p>The one care needed is not to create loggers by accident. All four operations use
     * {@link #getLogger} --which returns `null` if it is not there-- and never `Logger.getLogger`,
     * which would create it. Asking for the level of something that does not exist has to be able to
     * answer "it does not exist"; if the question created it, the answer never would be.
     */
    private static final class Management implements LoggingMXBean {

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
            // Empty and not `null`: `null` already means "there is no such logger" and they are two
            // different things.
            return n == null ? "" : n.getName();
        }

        @Override
        public void setLoggerLevel(String loggerName, String levelName) {
            Logger l = LogManager.getLogManager().getLogger(loggerName);
            if (l == null) {
                throw new IllegalArgumentException("logger desconocido: " + loggerName);
            }
            // `null` is not an error but the way of taking its own level away and going back to
            // inheriting; that is why it is passed through instead of rejected. A name that is not a
            // level IS an error, and `Level.parse` already throws `IllegalArgumentException`.
            l.setLevel(levelName == null ? null : Level.parse(levelName));
        }

        @Override
        public String getParentLoggerName(String loggerName) {
            Logger l = LogManager.getLogManager().getLogger(loggerName);
            if (l == null) {
                return null;
            }
            Logger p = l.getParent();
            // The root has no parent and answers `""`, again to leave `null` to the case above.
            return p == null ? "" : p.getName();
        }
    }

    /**
     * It leaves the registry with no configuration: no properties, no handlers and no levels of
     * their own.
     *
     * <p>The root is the exception and is left at {@link Level#INFO} instead of at `null`. It has to
     * be left at something: it is the only one with nobody to inherit from, and leaving it `null`
     * would make the effective level come out of a hidden default instead of a level that can be
     * read.
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
                        // Closing one handler must not stop the others being closed.
                    }
                    l.removeHandler(hs[i]);
                    i = i + 1;
                }
                String n = l.getName();
                l.setLevel(n != null && n.isEmpty() ? Level.INFO : null);
            }
        }
    }

    /** That configuration property's value, or `null` if it is not there. */
    public String getProperty(String name) {
        synchronized (this.props) {
            return this.props.getProperty(name);
        }
    }

    /**
     * It used to check the {@link LoggingPermission}; now it does nothing.
     *
     * <p>It is not an omission: what checked was the security manager, which can no longer exist.
     * With no manager there is nobody to ask, and "there is nobody to forbid it" is exactly passing.
     *
     * @deprecated Along with the security manager.
     */
    @Deprecated(since = "17", forRemoval = true)
    public void checkAccess() throws SecurityException {
    }

    // ---- the configuration -----------------------------------------------------------------------

    /**
     * It rereads the configuration from wherever it comes from by default, throwing away whatever
     * there was.
     *
     * <p>With `java.util.logging.config.class`, that class is instantiated and made responsible for
     * configuring; with `java.util.logging.config.file`, that file is read; with neither, the
     * built-in configuration.
     */
    public void readConfiguration() throws java.io.IOException, SecurityException {
        String cls = System.getProperty("java.util.logging.config.class");
        if (cls != null) {
            try {
                // Instantiating it IS the configuration: its constructor is expected to call
                // `readConfiguration(InputStream)` with whatever it knows how to read.
                build(cls);
                return;
            } catch (Exception e) {
                System.err.println("Logging configuration class \"" + cls + "\" failed: " + e);
            }
        }
        String file = System.getProperty("java.util.logging.config.file");
        if (file != null) {
            java.io.InputStream in = new java.io.FileInputStream(file);
            try {
                this.readConfiguration(in);
            } finally {
                in.close();
            }
            return;
        }
        this.reset();
        synchronized (this.props) {
            this.loadDefault();
        }
        this.applyTo();
        this.notifyListeners();
    }

    /**
     * It rereads the configuration from that stream, throwing away whatever there was.
     *
     * <p>The {@link #reset} goes first and is what makes this "reread" and not "add": a new
     * configuration that left the previous one's handlers alive would duplicate every message.
     */
    public void readConfiguration(java.io.InputStream ins)
            throws java.io.IOException, SecurityException {
        if (ins == null) {
            throw new NullPointerException("ins");
        }
        java.util.Properties newProps = new java.util.Properties();
        newProps.load(ins);
        this.reset();
        synchronized (this.props) {
            for (String k : newProps.stringPropertyNames()) {
                this.props.setProperty(k, newProps.getProperty(k));
            }
        }
        this.applyTo();
        this.notifyListeners();
    }

    // It hands the current configuration to every logger that already exists, and runs `config`'s
    // classes. Those that do not exist yet receive it as they are created, in `addLogger`.
    private void applyTo() {
        String cfg = this.getProperty("config");
        if (cfg != null) {
            String[] classes = splitList(cfg);
            int i = 0;
            while (i < classes.length) {
                try {
                    build(classes[i]);
                } catch (Exception e) {
                    System.err.println("Can't load config class \"" + classes[i] + "\": " + e);
                }
                i = i + 1;
            }
        }
        java.util.ArrayList<Logger> copy;
        synchronized (this.loggers) {
            copy = new java.util.ArrayList<Logger>(this.loggers.values());
        }
        for (Logger l : copy) {
            String n = l.getName();
            if (n != null) {
                this.configure(l, n);
            }
        }
    }

    /**
     * It merges that configuration with the one there is, deciding key by key with `mapper`.
     *
     * <p>The difference from {@link #readConfiguration(java.io.InputStream)} is that here there is
     * **no** `reset`: what the new configuration does not mention stays as it was. It is what is
     * needed for changing a logger's level in a program that is already running without tearing down
     * its handlers, which is when one wants to raise the detail to look at something.
     *
     * <p>`mapper` receives each property's name --from the union of the old and the new-- and returns
     * a function from (old value, new value) to the value that remains; `null` deletes the property.
     * A null `mapper` amounts to keeping the new one.
     */
    public void updateConfiguration(java.util.function.Function<String,
            java.util.function.BiFunction<String, String, String>> mapper)
            throws java.io.IOException {
        this.updateConfiguration(null, mapper);
    }

    /** The one above, with the new configuration read from that stream. */
    public void updateConfiguration(java.io.InputStream ins,
            java.util.function.Function<String,
                    java.util.function.BiFunction<String, String, String>> mapper)
            throws java.io.IOException {
        java.util.Properties newProps = new java.util.Properties();
        if (ins != null) {
            newProps.load(ins);
        } else {
            String file = System.getProperty("java.util.logging.config.file");
            if (file != null) {
                java.io.InputStream in = new java.io.FileInputStream(file);
                try {
                    newProps.load(in);
                } finally {
                    in.close();
                }
            } else {
                newProps.load(new java.io.StringReader(DEFAULT_CONFIG));
            }
        }

        java.util.HashSet<String> keys = new java.util.HashSet<String>();
        java.util.Properties oldProps;
        synchronized (this.props) {
            oldProps = new java.util.Properties();
            for (String k : this.props.stringPropertyNames()) {
                oldProps.setProperty(k, this.props.getProperty(k));
                keys.add(k);
            }
        }
        for (String k : newProps.stringPropertyNames()) {
            keys.add(k);
        }

        java.util.Properties result = new java.util.Properties();
        for (String k : keys) {
            String old = oldProps.getProperty(k);
            String newOne = newProps.getProperty(k);
            String left = newOne;
            if (mapper != null) {
                java.util.function.BiFunction<String, String, String> f = mapper.apply(k);
                left = f == null ? newOne : f.apply(old, newOne);
            }
            if (left != null) {
                result.setProperty(k, left);
            }
        }

        synchronized (this.props) {
            this.props.clear();
            for (String k : result.stringPropertyNames()) {
                this.props.setProperty(k, result.getProperty(k));
            }
        }

        // Only what **changed** is touched. A logger whose `.level` appeared nowhere keeps the one
        // it had, which is the whole point of this not being a `readConfiguration`.
        java.util.ArrayList<Logger> copy;
        synchronized (this.loggers) {
            copy = new java.util.ArrayList<Logger>(this.loggers.values());
        }
        for (Logger l : copy) {
            String n = l.getName();
            if (n == null) {
                continue;
            }
            // The level is touched **only if** the new configuration says so. That a property
            // disappears does not mean "go back to inheriting": the level may have been set by the
            // program in code, and an update that does not speak of the matter has no business
            // overwriting it.
            String levelKey = n + ".level";
            String newLevel = result.getProperty(levelKey);
            if (newLevel != null && !same(oldProps.getProperty(levelKey), newLevel)) {
                Level lv = this.getLevelProperty(levelKey, null);
                if (lv != null) {
                    l.setLevel(lv);
                }
            }
            String handlersKey = n.isEmpty() ? "handlers" : n + ".handlers";
            if (!same(oldProps.getProperty(handlersKey), result.getProperty(handlersKey))) {
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
                String list = result.getProperty(handlersKey);
                if (list != null) {
                    this.installHandlers(l, list);
                }
            }
            String useParentKey = n + ".useParentHandlers";
            if (!same(oldProps.getProperty(useParentKey), result.getProperty(useParentKey))) {
                String v = result.getProperty(useParentKey);
                l.setUseParentHandlers(v == null || Boolean.parseBoolean(v.trim()));
            }
        }
        this.notifyListeners();
    }

    private static boolean same(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    /**
     * It adds a listener that runs every time the configuration is reread or updated.
     *
     * <p>It is what code that **derives** something from the configuration --a cached `boolean` of
     * whether fine logging is on-- needs in order to find out that that something has gone stale.
     *
     * @return this same manager, for chaining
     * @throws NullPointerException if `listener` is `null`
     */
    public LogManager addConfigurationListener(Runnable listener) {
        if (listener == null) {
            throw new NullPointerException("listener");
        }
        synchronized (this.listeners) {
            this.listeners.add(listener);
        }
        return this;
    }

    /** It removes a listener; if it was not there, nothing happens. */
    public void removeConfigurationListener(Runnable listener) {
        if (listener == null) {
            throw new NullPointerException("listener");
        }
        synchronized (this.listeners) {
            this.listeners.remove(listener);
        }
    }

    private void notifyListeners() {
        java.util.ArrayList<Runnable> copy;
        synchronized (this.listeners) {
            copy = new java.util.ArrayList<Runnable>(this.listeners);
        }
        for (Runnable r : copy) {
            try {
                r.run();
            } catch (Exception e) {
                // A listener that fails cannot stop the others running nor invalidate the
                // configuration, which is already applied.
            }
        }
    }

    // ---- what the handlers read ------------------------------------------------------------------
    //
    // They are not public API --they are not in the JDK either--: they are the converting,
    // defaulting accessors each handler uses to read its own configuration. They all share the same
    // rule: if the property is missing or cannot be converted, the default applies. A misspelled
    // `.level=WHATEVER` cannot bring down the program's start-up.

    Level getLevelProperty(String name, Level byDefault) {
        String v = this.getProperty(name);
        if (v == null) {
            return byDefault;
        }
        try {
            return Level.parse(v.trim());
        } catch (Exception e) {
            return byDefault;
        }
    }

    int getIntProperty(String name, int byDefault) {
        String v = this.getProperty(name);
        if (v == null) {
            return byDefault;
        }
        try {
            return Integer.parseInt(v.trim());
        } catch (Exception e) {
            return byDefault;
        }
    }

    long getLongProperty(String name, long byDefault) {
        String v = this.getProperty(name);
        if (v == null) {
            return byDefault;
        }
        try {
            return Long.parseLong(v.trim());
        } catch (Exception e) {
            return byDefault;
        }
    }

    boolean getBooleanProperty(String name, boolean byDefault) {
        String v = this.getProperty(name);
        if (v == null) {
            return byDefault;
        }
        v = v.toLowerCase().trim();
        if (v.equals("true") || v.equals("1")) {
            return true;
        }
        if (v.equals("false") || v.equals("0")) {
            return false;
        }
        return byDefault;
    }

    String getStringProperty(String name, String byDefault) {
        String v = this.getProperty(name);
        return v == null ? byDefault : v.trim();
    }

    Filter getFilterProperty(String name, Filter byDefault) {
        String v = this.getProperty(name);
        if (v == null) {
            return byDefault;
        }
        try {
            return (Filter) build(v.trim());
        } catch (Exception e) {
            return byDefault;
        }
    }

    Formatter getFormatterProperty(String name, Formatter byDefault) {
        String v = this.getProperty(name);
        if (v == null) {
            return byDefault;
        }
        try {
            return (Formatter) build(v.trim());
        } catch (Exception e) {
            return byDefault;
        }
    }

    // An instance of that class through its no-argument constructor.
    static Object build(String className) throws Exception {
        Class<?> c = Class.forName(className);
        return c.getDeclaredConstructor().newInstance();
    }

    // The configuration separates lists by spaces or by commas, indifferently.
    private static String[] splitList(String list) {
        java.util.ArrayList<String> out = new java.util.ArrayList<String>();
        int i = 0;
        StringBuilder current = new StringBuilder();
        while (i < list.length()) {
            char c = list.charAt(i);
            if (c == ',' || c == ' ' || c == '\t') {
                if (current.length() > 0) {
                    out.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
            i = i + 1;
        }
        if (current.length() > 0) {
            out.add(current.toString());
        }
        return out.toArray(new String[out.size()]);
    }
}
