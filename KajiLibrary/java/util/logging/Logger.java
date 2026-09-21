package java.util.logging;

/**
 * KajiLibrary's java.util.logging.Logger -- where messages are emitted from.
 *
 * <p>Loggers form a **tree by the dots in the name**: `com.acme.db` is a child of `com.acme`, which
 * is a child of the root. Two things follow from that, and they are the ones to understand.
 *
 * <p>The first is that the level is **inherited**: a logger with no level of its own uses its
 * parent's, so putting `com.acme` at `FINE` affects all its descendants without naming them. The
 * second is that messages **go up**: a record that passes the filter is published to this logger's
 * handlers and then to its parent's, and so on to the root, unless one of them cuts it off with
 * {@link #setUseParentHandlers}. That is why putting **one** handler on the root is enough to see
 * everything.
 *
 * <p>The {@link java.util.function.Supplier} variants exist because of cost: building the message
 * costs something even if it is then discarded, and a `log(FINE, () -> expensive())` evaluates
 * nothing if `FINE` is not enabled. It is the only way of having fine logging without paying for it
 * when it is off.
 *
 * <p>Localisation is also inherited down the tree, and with a twist: the bundle used when emitting
 * is the nearest ancestor's that has one, but {@link #getResourceBundle} returns **only its own**.
 * It is not an inconsistency -- which bundle applies is one thing and which one configured this
 * logger is another, and confusing them would make it impossible to know whether it needs
 * configuring.
 */
public class Logger {

    /** The name of the logger that records the global system's calls. */
    public static final String GLOBAL_LOGGER_NAME = "global";

    /**
     * The global logger. It exists for examples and for throwaway code, not for an application.
     *
     * @deprecated A public `static final` field is initialised when the class is initialised, and
     *             that happens earlier than one thinks: reading it from the start-up of the logging
     *             infrastructure itself could give `null` halfway through initialisation. That is
     *             why the JDK added {@link #getGlobal()}, which is a method and therefore does not
     *             have that problem.
     */
    @Deprecated
    public static final Logger global = Logger.getLogger(GLOBAL_LOGGER_NAME);

    /** The global logger, without the {@link #global} field's initialisation problem. */
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

    // ---- obtaining them -------------------------------------------------------------------------

    /**
     * The logger by that name, creating it if it does not exist.
     *
     * <p>It returns **the same** object for the same name, which is what lets it be configured in
     * one place and used in another.
     */
    public static Logger getLogger(String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        LogManager m = LogManager.getLogManager();
        Logger already = m.getLogger(name);
        if (already != null) {
            return already;
        }
        Logger newOne = new Logger(name, null);
        if (!name.isEmpty()) {
            // The root is forced to exist **before** registering: `addLogger` hangs the newcomer
            // off its nearest ancestor, and with no root there would be nothing to hang it off.
            root();
        }
        if (!m.addLogger(newOne)) {
            // Another thread won the race: theirs stands, so that identity by name holds.
            return m.getLogger(name);
        }
        return newOne;
    }

    /**
     * The one above, with the name of the bundle the messages are translated with.
     *
     * <p>If the logger already existed **without** a bundle, this one is set on it. If it already had
     * **another**, that is an error: two parts of the program that asked for the same logger with
     * different bundles cannot both be satisfied, and choosing in silence breaks one of them's
     * translation without telling it.
     *
     * @throws java.util.MissingResourceException if the bundle is not found -- it is looked up when
     *         asked for and not when emitting, which is while something can still be done about it
     * @throws IllegalArgumentException if the logger already had another bundle
     */
    public static Logger getLogger(String name, String resourceBundleName) {
        Logger l = getLogger(name);
        if (resourceBundleName != null) {
            String already = l.resourceBundleName;
            if (already == null) {
                // It is loaded first: if it does not exist, the logger is left as it was and not half configured.
                l.bundle = java.util.ResourceBundle.getBundle(resourceBundleName);
                l.resourceBundleName = resourceBundleName;
            } else if (!already.equals(resourceBundleName)) {
                throw new IllegalArgumentException(already + " != " + resourceBundleName);
            }
        }
        return l;
    }

    /**
     * A logger **with no name**, registered nowhere.
     *
     * <p>It serves exactly the opposite purpose to {@link #getLogger}: since nobody else can find
     * it, nobody else can reconfigure it. It is what suits a library that does not want its logging
     * to depend on the global configuration.
     */
    public static Logger getAnonymousLogger() {
        return getAnonymousLogger(null);
    }

    /**
     * The one above, with a bundle.
     *
     * @throws java.util.MissingResourceException if the bundle is not found
     */
    public static Logger getAnonymousLogger(String resourceBundleName) {
        Logger l = new Logger(null, resourceBundleName);
        if (resourceBundleName != null) {
            l.bundle = java.util.ResourceBundle.getBundle(resourceBundleName);
        }
        l.parent = root();
        return l;
    }

    private static Logger root() {
        LogManager m = LogManager.getLogManager();
        Logger r = m.getLogger("");
        if (r != null) {
            return r;
        }
        Logger fresh = new Logger("", null);
        // A start-up level in case the configuration says nothing: the root is the only one with
        // nobody to inherit from. Its handlers and its final level come from the configuration, which
        // `addLogger` applies to it.
        fresh.level = Level.INFO;
        if (!m.addLogger(fresh)) {
            return m.getLogger("");
        }
        return fresh;
    }

    // ---- configuration --------------------------------------------------------------------------

    /** The name, or `null` if it is anonymous. */
    public String getName() {
        return this.name;
    }

    public String getResourceBundleName() {
        return this.resourceBundleName;
    }

    /**
     * The bundle **of its own**, or `null` if it has none.
     *
     * <p>`null` does not mean the messages are not translated: if an ancestor has a bundle, that is
     * the one used. What this method answers is whether **this** logger was configured, which is
     * another question.
     */
    public java.util.ResourceBundle getResourceBundle() {
        return this.bundle;
    }

    /**
     * It sets the bundle of its own.
     *
     * <p>The bundle has to have a base name because the name is what travels: a serialised record
     * carries the name and not the object, and an anonymous bundle would give a record that cannot be
     * translated on the other side.
     *
     * @throws NullPointerException if `bundle` is `null`
     * @throws IllegalArgumentException if the bundle has no base name, or if this logger already had
     *         another bundle -- for the same reason as {@link #getLogger(String, String)}
     */
    public void setResourceBundle(java.util.ResourceBundle bundle) {
        if (bundle == null) {
            throw new NullPointerException("bundle");
        }
        String base = bundle.getBaseBundleName();
        if (base == null || base.isEmpty()) {
            throw new IllegalArgumentException("resource bundle must have a name");
        }
        String already = this.resourceBundleName;
        if (already != null && !already.equals(base)) {
            throw new IllegalArgumentException("can't replace resource bundle");
        }
        this.bundle = bundle;
        this.resourceBundleName = base;
    }

    /** The level of its own, or `null` if it inherits its parent's. */
    public Level getLevel() {
        return this.level;
    }

    /** It sets the level; `null` to go back to inheriting. */
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

    /** Whether the messages also go to the parent's handlers. */
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
     * Whether a message at that level would be recorded.
     *
     * <p>It is worth asking before building an expensive message -- or using the
     * {@link java.util.function.Supplier} variant, which does it by itself.
     */
    public boolean isLoggable(Level level) {
        int own = this.effectiveLevel().intValue();
        if (own == Level.OFF.intValue()) {
            return false;
        }
        return level.intValue() >= own;
    }

    // The first level of its own going up the tree; `INFO` if there is none.
    private Level effectiveLevel() {
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

    // ---- publishing -----------------------------------------------------------------------------

    /**
     * It publishes that record: to its own handlers and, where appropriate, to its parent's.
     *
     * <p>Every other method ends up here -- but **not** the other way round: this one takes the
     * record as it comes and sets neither the logger's name nor the bundle on it. That is what suits
     * the one method handed an already built {@link LogRecord}: whoever built it decided what it
     * says.
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
     * It sets on the record what comes from **this** logger and then publishes it.
     *
     * <p>It is where every convenience method goes through, and the reason it exists is that
     * {@link #log(LogRecord)} must not do this: a record the caller built already says what it has
     * to say.
     */
    private void doLog(LogRecord record) {
        record.setLoggerName(this.name);
        java.util.ResourceBundle rb = this.effectiveBundle();
        if (rb != null) {
            record.setResourceBundle(rb);
            record.setResourceBundleName(rb.getBaseBundleName());
        }
        this.log(record);
    }

    // The one above with a bundle given by name, for the deprecated `logrb`. A name that does not
    // resolve leaves the record with the name set and no bundle: that is the truth --that translation
    // was asked for and not found-- and the formatter falls back to the raw message, which is all
    // that is left.
    private void doLog(LogRecord record, String rbname) {
        record.setLoggerName(this.name);
        if (rbname != null) {
            record.setResourceBundleName(rbname);
            record.setResourceBundle(byName(rbname));
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

    // The bundle of the nearest ancestor that has one, this one included.
    private java.util.ResourceBundle effectiveBundle() {
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

    private static java.util.ResourceBundle byName(String rbname) {
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

    /** The message is built **only if** the level is enabled. */
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

    // ---- with an explicit source -------------------------------------------------------------------------
    //
    // The `logp` take the class and the method instead of deducing them. They exist for code that
    // logs **on somebody else's behalf** --a wrapper, a framework-- where the deduced source would be
    // the wrapper and not what the reader cares about.

    public void logp(Level level, String sourceClass, String sourceMethod, String msg) {
        if (!this.isLoggable(level)) {
            return;
        }
        this.doLog(withSource(level, msg, sourceClass, sourceMethod));
    }

    public void logp(Level level, String sourceClass, String sourceMethod,
            java.util.function.Supplier<String> msgSupplier) {
        if (!this.isLoggable(level)) {
            return;
        }
        this.doLog(withSource(level, msgSupplier.get(), sourceClass, sourceMethod));
    }

    public void logp(Level level, String sourceClass, String sourceMethod, String msg,
            Object param1) {
        if (!this.isLoggable(level)) {
            return;
        }
        LogRecord r = withSource(level, msg, sourceClass, sourceMethod);
        r.setParameters(new Object[] {param1});
        this.doLog(r);
    }

    public void logp(Level level, String sourceClass, String sourceMethod, String msg,
            Object[] params) {
        if (!this.isLoggable(level)) {
            return;
        }
        LogRecord r = withSource(level, msg, sourceClass, sourceMethod);
        r.setParameters(params);
        this.doLog(r);
    }

    public void logp(Level level, String sourceClass, String sourceMethod, String msg,
            Throwable thrown) {
        if (!this.isLoggable(level)) {
            return;
        }
        LogRecord r = withSource(level, msg, sourceClass, sourceMethod);
        r.setThrown(thrown);
        this.doLog(r);
    }

    public void logp(Level level, String sourceClass, String sourceMethod, Throwable thrown,
            java.util.function.Supplier<String> msgSupplier) {
        if (!this.isLoggable(level)) {
            return;
        }
        LogRecord r = withSource(level, msgSupplier.get(), sourceClass, sourceMethod);
        r.setThrown(thrown);
        this.doLog(r);
    }

    // ---- with an explicit bundle -------------------------------------------------------------------------
    //
    // The `logrb` translate with **this** bundle instead of with the logger's. It is what a library
    // needs when it emits through the application's logger --so that the application's configuration
    // reaches it-- but whose messages are in its own bundle, not in the application's.
    //
    // The four forms that take the bundle by **name** are deprecated, and with reason: a name is
    // resolved against a class loader that at emission time may not be the one one thinks. Passing
    // the object has no such ambiguity.

    /** @deprecated Use the form that takes the {@link java.util.ResourceBundle}. */
    @Deprecated
    public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName,
            String msg) {
        if (!this.isLoggable(level)) {
            return;
        }
        this.doLog(withSource(level, msg, sourceClass, sourceMethod), bundleName);
    }

    /** @deprecated Use the form that takes the {@link java.util.ResourceBundle}. */
    @Deprecated
    public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName,
            String msg, Object param1) {
        if (!this.isLoggable(level)) {
            return;
        }
        LogRecord r = withSource(level, msg, sourceClass, sourceMethod);
        r.setParameters(new Object[] {param1});
        this.doLog(r, bundleName);
    }

    /** @deprecated Use the form that takes the {@link java.util.ResourceBundle}. */
    @Deprecated
    public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName,
            String msg, Object[] params) {
        if (!this.isLoggable(level)) {
            return;
        }
        LogRecord r = withSource(level, msg, sourceClass, sourceMethod);
        r.setParameters(params);
        this.doLog(r, bundleName);
    }

    /** @deprecated Use the form that takes the {@link java.util.ResourceBundle}. */
    @Deprecated
    public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName,
            String msg, Throwable thrown) {
        if (!this.isLoggable(level)) {
            return;
        }
        LogRecord r = withSource(level, msg, sourceClass, sourceMethod);
        r.setThrown(thrown);
        this.doLog(r, bundleName);
    }

    public void logrb(Level level, String sourceClass, String sourceMethod,
            java.util.ResourceBundle bundle, String msg, Object... params) {
        if (!this.isLoggable(level)) {
            return;
        }
        LogRecord r = withSource(level, msg, sourceClass, sourceMethod);
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
        LogRecord r = withSource(level, msg, sourceClass, sourceMethod);
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

    private static LogRecord withSource(Level level, String msg, String cls, String method) {
        LogRecord r = new LogRecord(level, msg);
        r.setSourceClassName(cls);
        r.setSourceMethodName(method);
        return r;
    }

    // ---- method entry and exit ---------------------------------------------------------------------------
    //
    // All at `FINER`, and with fixed messages (`ENTRY`, `RETURN`, `THROW`) so that a tool can
    // recognise them without parsing.

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
     * It records that the method left by throwing.
     *
     * <p>At `FINER` and not at `SEVERE`, even though there is an exception: it is not a failure of
     * the program but the trace of a method that ended that way, and whoever catches the exception
     * will decide whether it is serious.
     */
    public void throwing(String sourceClass, String sourceMethod, Throwable thrown) {
        if (!this.isLoggable(Level.FINER)) {
            return;
        }
        LogRecord r = withSource(Level.FINER, "THROW", sourceClass, sourceMethod);
        r.setThrown(thrown);
        this.doLog(r);
    }

    // ---- the per-level shortcuts -------------------------------------------------------------------------

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
