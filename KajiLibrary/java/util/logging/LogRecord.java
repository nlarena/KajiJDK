package java.util.logging;

/**
 * KajiLibrary's java.util.logging.LogRecord -- un mensaje, con todo lo que se sabe de el.
 *
 * <p>Es un objeto mutable que viaja del `Logger` a cada `Handler`, y ahi esta la unica sutileza:
 * **no se copia**. Un manejador que lo modifique se lo cambia a los que vienen despues, y eso esta
 * documentado como responsabilidad de quien escribe el manejador.
 *
 * <p>El numero de secuencia existe para desempatar: dos mensajes del mismo milisegundo no se pueden
 * ordenar por hora, y el orden importa para leer una traza.
 */
public class LogRecord implements java.io.Serializable {

    private static final java.util.concurrent.atomic.AtomicLong PROXIMO =
            new java.util.concurrent.atomic.AtomicLong(0);

    private Level level;
    private String msg;
    private long sequenceNumber;
    private String sourceClassName;
    private String sourceMethodName;
    private String loggerName;
    private Object[] parameters;
    private Throwable thrown;
    private java.time.Instant instant;
    private long threadID;
    private java.util.ResourceBundle resourceBundle;
    private String resourceBundleName;

    // Los identificadores cortos que se le dieron a los hilos cuyo identificador largo no entra en un
    // `int`. Ver `getThreadID`.
    private static final java.util.HashMap<Long, Integer> CORTOS =
            new java.util.HashMap<Long, Integer>();

    // De donde sale el proximo identificador corto sintetico. Arranca en el negativo mas grande
    // porque los identificadores reales de esta VM son positivos y chicos: empezar por el otro
    // extremo hace que un sintetico no pueda chocar con uno real.
    private static int proximoCorto = Integer.MIN_VALUE;

    public LogRecord(Level level, String msg) {
        if (level == null) {
            throw new NullPointerException("level");
        }
        this.level = level;
        this.msg = msg;
        this.sequenceNumber = PROXIMO.getAndIncrement();
        this.instant = java.time.Instant.now();
        this.threadID = Thread.currentThread().getId();
    }

    public Level getLevel() {
        return this.level;
    }

    public void setLevel(Level level) {
        if (level == null) {
            throw new NullPointerException("level");
        }
        this.level = level;
    }

    /** El mensaje **sin** formatear: puede llevar `{0}`, que resuelve el formateador. */
    public String getMessage() {
        return this.msg;
    }

    public void setMessage(String message) {
        this.msg = message;
    }

    /** Los valores que reemplazan los `{n}` del mensaje. */
    public Object[] getParameters() {
        return this.parameters;
    }

    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }

    public String getLoggerName() {
        return this.loggerName;
    }

    public void setLoggerName(String name) {
        this.loggerName = name;
    }

    /** Quien emitio el mensaje, si se sabe. */
    public String getSourceClassName() {
        return this.sourceClassName;
    }

    public void setSourceClassName(String sourceClassName) {
        this.sourceClassName = sourceClassName;
    }

    public String getSourceMethodName() {
        return this.sourceMethodName;
    }

    public void setSourceMethodName(String sourceMethodName) {
        this.sourceMethodName = sourceMethodName;
    }

    /** La excepcion asociada, si la hay. */
    public Throwable getThrown() {
        return this.thrown;
    }

    public void setThrown(Throwable thrown) {
        this.thrown = thrown;
    }

    /** El orden en que se creo; desempata a los del mismo instante. */
    public long getSequenceNumber() {
        return this.sequenceNumber;
    }

    public void setSequenceNumber(long seq) {
        this.sequenceNumber = seq;
    }

    /** Cuando se creo, al nanosegundo. */
    public java.time.Instant getInstant() {
        return this.instant;
    }

    public void setInstant(java.time.Instant instant) {
        if (instant == null) {
            throw new NullPointerException("instant");
        }
        this.instant = instant;
    }

    /** Cuando se creo, en milisegundos. */
    public long getMillis() {
        return this.instant.toEpochMilli();
    }

    public void setMillis(long millis) {
        this.instant = java.time.Instant.ofEpochMilli(millis);
    }

    /** El hilo que lo emitio. */
    public long getLongThreadID() {
        return this.threadID;
    }

    public LogRecord setLongThreadID(long longThreadID) {
        this.threadID = longThreadID;
        return this;
    }

    /**
     * El hilo que lo emitio, estrechado a `int`.
     *
     * <p>Existe de antes de que los identificadores de hilo fueran de 64 bits, y por eso esta
     * deprecado: para un identificador que no entra en un `int` no hay respuesta correcta, solo
     * respuestas distinguibles. Lo que se garantiza --y es lo unico que el contrato pide, "un
     * identificador"-- es que dos hilos distintos no reciban el mismo numero: los que entran se
     * devuelven tal cual y a los que no se les asigna uno sintetico, estable para ese identificador
     * largo. **Cual** numero sintetico es cosa de la implementacion, aca y en el JDK.
     */
    @Deprecated(since = "16")
    public int getThreadID() {
        long id = this.threadID;
        if (id >= Integer.MIN_VALUE && id <= Integer.MAX_VALUE) {
            return (int) id;
        }
        synchronized (CORTOS) {
            Integer ya = CORTOS.get(Long.valueOf(id));
            if (ya != null) {
                return ya.intValue();
            }
            int nuevo = proximoCorto;
            proximoCorto = proximoCorto + 1;
            CORTOS.put(Long.valueOf(id), Integer.valueOf(nuevo));
            return nuevo;
        }
    }

    /** Fija los dos identificadores: el corto **y** el largo, que no pueden quedar en desacuerdo. */
    @Deprecated(since = "16")
    public void setThreadID(int threadID) {
        this.threadID = threadID;
    }

    /**
     * El catalogo con el que se traduce el mensaje, o `null`.
     *
     * <p>Cuando esta, el mensaje **no es el texto** sino la clave: el formateador busca
     * {@link #getMessage} en el catalogo y usa lo que encuentra. De ahi que un registro localizado
     * lleve `"saludo"` como mensaje y no `"hola {0}"`.
     */
    public java.util.ResourceBundle getResourceBundle() {
        return this.resourceBundle;
    }

    public void setResourceBundle(java.util.ResourceBundle bundle) {
        this.resourceBundle = bundle;
    }

    /**
     * El nombre del catalogo, o `null`.
     *
     * <p>Es independiente de {@link #getResourceBundle}: se fijan por separado y ninguno arrastra al
     * otro. Suena raro y no lo es -- un registro serializado viaja con el **nombre**, porque el
     * catalogo en si no es serializable, y del otro lado se recarga por nombre.
     */
    public String getResourceBundleName() {
        return this.resourceBundleName;
    }

    public void setResourceBundleName(String name) {
        this.resourceBundleName = name;
    }
}
