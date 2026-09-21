package java.util.logging;

/**
 * KajiLibrary's java.util.logging.Handler -- where a message goes.
 *
 * <p>The separation between {@link Logger} and this is what makes the library useful: the logger
 * says **what** is recorded and the handler **where** it ends up, and changing one does not touch
 * the other. One logger can have several, at different levels -- everything to a file and only the
 * serious to the console.
 *
 * <p>Its level is a second filter **after** the logger's level, not instead of it: a message the
 * logger discarded never arrives.
 */
public abstract class Handler {

    private Level level = Level.ALL;
    private Formatter formatter;
    private Filter filter;
    private String encoding;
    private ErrorManager errorManager = new ErrorManager();

    protected Handler() {
    }

    /** It writes that record. */
    public abstract void publish(LogRecord record);

    /** It flushes whatever is pending. */
    public abstract void flush();

    /** It closes and releases the resources. */
    public abstract void close() throws SecurityException;

    public synchronized void setFormatter(Formatter newFormatter) throws SecurityException {
        if (newFormatter == null) {
            throw new NullPointerException("newFormatter");
        }
        this.formatter = newFormatter;
    }

    public Formatter getFormatter() {
        return this.formatter;
    }

    public synchronized void setFilter(Filter newFilter) throws SecurityException {
        this.filter = newFilter;
    }

    public Filter getFilter() {
        return this.filter;
    }

    public synchronized void setLevel(Level newLevel) throws SecurityException {
        if (newLevel == null) {
            throw new NullPointerException("newLevel");
        }
        this.level = newLevel;
    }

    public Level getLevel() {
        return this.level;
    }

    public synchronized void setEncoding(String encoding)
            throws SecurityException, java.io.UnsupportedEncodingException {
        this.encoding = encoding;
    }

    public String getEncoding() {
        return this.encoding;
    }

    public synchronized void setErrorManager(ErrorManager em) {
        if (em == null) {
            throw new NullPointerException("em");
        }
        this.errorManager = em;
    }

    public ErrorManager getErrorManager() {
        return this.errorManager;
    }

    /** It hands the failure to the {@link ErrorManager}. */
    protected void reportError(String msg, Exception ex, int code) {
        this.errorManager.error(msg, ex, code);
    }

    /** Whether this handler accepts that record: by level and then by filter. */
    public boolean isLoggable(LogRecord record) {
        if (record == null) {
            return false;
        }
        int n = record.getLevel().intValue();
        if (n < this.level.intValue() || this.level.intValue() == Level.OFF.intValue()) {
            return false;
        }
        Filter f = this.filter;
        return f == null || f.isLoggable(record);
    }
}
