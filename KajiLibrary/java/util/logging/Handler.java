package java.util.logging;

/**
 * KajiLibrary's java.util.logging.Handler -- a donde va un mensaje.
 *
 * <p>La separacion entre {@link Logger} y esto es lo que hace util a la biblioteca: el logger dice
 * **que** se registra y el manejador **donde** termina, y cambiar uno no toca al otro. Un mismo
 * logger puede tener varios, con niveles distintos -- todo a un archivo y solo lo grave a la consola.
 *
 * <p>Su nivel es un segundo filtro **despues** del nivel del logger, no en vez de el: un mensaje que
 * el logger descarto no llega nunca.
 */
public abstract class Handler {

    private Level level = Level.ALL;
    private Formatter formatter;
    private Filter filter;
    private String encoding;
    private ErrorManager errorManager = new ErrorManager();

    protected Handler() {
    }

    /** Escribe ese registro. */
    public abstract void publish(LogRecord record);

    /** Vacia lo que este pendiente. */
    public abstract void flush();

    /** Cierra y suelta los recursos. */
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

    /** Le pasa el fallo al {@link ErrorManager}. */
    protected void reportError(String msg, Exception ex, int code) {
        this.errorManager.error(msg, ex, code);
    }

    /** Si este manejador acepta ese registro: por nivel y despues por filtro. */
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
