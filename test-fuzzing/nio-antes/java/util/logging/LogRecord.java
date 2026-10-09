package java.util.logging;

/**
 * KajiLibrary's java.util.logging.LogRecord -- a message, with everything known about it.
 *
 * <p>It is a mutable object that travels from the `Logger` to each `Handler`, and that is where the
 * one subtlety lies: **it is not copied**. A handler that modifies it changes it for the ones that
 * come after, and that is documented as the responsibility of whoever writes the handler.
 *
 * <p>The sequence number exists to break ties: two messages of the same millisecond cannot be
 * ordered by time, and order matters for reading a log.
 */
public class LogRecord implements java.io.Serializable {

    private static final java.util.concurrent.atomic.AtomicLong NEXT_SHORT_ID =
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

    // The short identifiers given to the threads whose long identifier does not fit in an `int`.
    // See `getThreadID`.
    private static final java.util.HashMap<Long, Integer> SHORT_IDS =
            new java.util.HashMap<Long, Integer>();

    // Where the next synthetic short identifier comes from. It starts at the largest negative
    // because this VM's real identifiers are positive and small: starting from the other end makes a
    // synthetic one unable to collide with a real one.
    private static int nextShortId = Integer.MIN_VALUE;

    public LogRecord(Level level, String msg) {
        if (level == null) {
            throw new NullPointerException("level");
        }
        this.level = level;
        this.msg = msg;
        this.sequenceNumber = NEXT_SHORT_ID.getAndIncrement();
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

    /** The **unformatted** message: it may carry `{0}`, which the formatter resolves. */
    public String getMessage() {
        return this.msg;
    }

    public void setMessage(String message) {
        this.msg = message;
    }

    /** The values that replace the message's `{n}`. */
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

    /** Who emitted the message, if it is known. */
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

    /** The associated exception, if there is one. */
    public Throwable getThrown() {
        return this.thrown;
    }

    public void setThrown(Throwable thrown) {
        this.thrown = thrown;
    }

    /** The order it was created in; it breaks ties among those of the same instant. */
    public long getSequenceNumber() {
        return this.sequenceNumber;
    }

    public void setSequenceNumber(long seq) {
        this.sequenceNumber = seq;
    }

    /** When it was created, to the nanosecond. */
    public java.time.Instant getInstant() {
        return this.instant;
    }

    public void setInstant(java.time.Instant instant) {
        if (instant == null) {
            throw new NullPointerException("instant");
        }
        this.instant = instant;
    }

    /** When it was created, in milliseconds. */
    public long getMillis() {
        return this.instant.toEpochMilli();
    }

    public void setMillis(long millis) {
        this.instant = java.time.Instant.ofEpochMilli(millis);
    }

    /** The thread that emitted it. */
    public long getLongThreadID() {
        return this.threadID;
    }

    public LogRecord setLongThreadID(long longThreadID) {
        this.threadID = longThreadID;
        return this;
    }

    /**
     * The thread that emitted it, narrowed to an `int`.
     *
     * <p>It dates from before thread identifiers were 64-bit, and that is why it is deprecated: for
     * an identifier that does not fit in an `int` there is no right answer, only distinguishable
     * ones. What is guaranteed --and it is all the contract asks for, "an identifier"-- is that two
     * different threads do not get the same number: the ones that fit are returned as they are and
     * the ones that do not are assigned a synthetic one, stable for that long identifier. **Which**
     * synthetic number is the implementation's business, here and in the JDK.
     */
    @Deprecated(since = "16")
    public int getThreadID() {
        long id = this.threadID;
        if (id >= Integer.MIN_VALUE && id <= Integer.MAX_VALUE) {
            return (int) id;
        }
        synchronized (SHORT_IDS) {
            Integer already = SHORT_IDS.get(Long.valueOf(id));
            if (already != null) {
                return already.intValue();
            }
            int newOne = nextShortId;
            nextShortId = nextShortId + 1;
            SHORT_IDS.put(Long.valueOf(id), Integer.valueOf(newOne));
            return newOne;
        }
    }

    /** It sets both identifiers: the short one **and** the long one, which cannot be left disagreeing. */
    @Deprecated(since = "16")
    public void setThreadID(int threadID) {
        this.threadID = threadID;
    }

    /**
     * The bundle the message is translated with, or `null`.
     *
     * <p>When it is there, the message **is not the text** but the key: the formatter looks
     * {@link #getMessage} up in the bundle and uses what it finds. Hence a localised record carries
     * `"greeting"` as its message and not `"hello {0}"`.
     */
    public java.util.ResourceBundle getResourceBundle() {
        return this.resourceBundle;
    }

    public void setResourceBundle(java.util.ResourceBundle bundle) {
        this.resourceBundle = bundle;
    }

    /**
     * The bundle's name, or `null`.
     *
     * <p>It is independent of {@link #getResourceBundle}: they are set separately and neither drags
     * the other. It sounds odd and it is not -- a serialised record travels with the **name**,
     * because the bundle itself is not serialisable, and on the other side it is reloaded by name.
     */
    public String getResourceBundleName() {
        return this.resourceBundleName;
    }

    public void setResourceBundleName(String name) {
        this.resourceBundleName = name;
    }
}
