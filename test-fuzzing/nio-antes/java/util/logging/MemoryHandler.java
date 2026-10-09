package java.util.logging;

/**
 * KajiLibrary's java.util.logging.MemoryHandler -- it keeps the last messages and lets them go when
 * something happens.
 *
 * <p>It resolves the tension between the two things one wants from a log and that contradict each
 * other: having the fine detail when something fails, and not paying the cost of writing it when
 * nothing fails. The way out is not to write: the records pile up in a ring in memory, and **only**
 * when one of level {@link #getPushLevel} or higher arrives is everything accumulated dumped to the
 * target handler. What is left in the file is the minute before the failure, which is exactly the
 * minute one would have wanted to be recording.
 *
 * <p>The buffer is circular and of fixed size, and that is the design too: on filling up it
 * **discards the oldest** instead of growing or refusing to accept. A program that runs a week
 * without failing cannot run out of memory because of its own log, and what piled up on Monday
 * matters to nobody.
 *
 * <p>Its own level and the push level are two different thresholds and are not to be confused: its
 * own --{@link Handler#setLevel}, which by default is {@link Level#ALL}-- decides **what enters the
 * buffer**, and the push level decides **what empties it**. Setting both the same turns this into an
 * ordinary handler with one step too many.
 *
 * <p>{@link #push} can also be called by hand, which is what is needed when the sign that something
 * is wrong is not a message but something else -- a health check that came back bad, a request that
 * took too long.
 */
public class MemoryHandler extends Handler {

    private static final int DEFAULT_SIZE = 1000;

    private Handler target;
    private Level pushLevel;
    private LogRecord[] buffer;

    // The oldest one's index, and how many there are. Those two are enough: the newest is at
    // `(start + howMany - 1) % buffer.length`, and there is no need to tell "empty" from "full" by
    // the position of two pointers, which is the ring's classic mistake.
    private int start;
    private int howMany;

    /**
     * The one that comes out of the configuration.
     *
     * <p>The target has **no default** and it is the only handler of which that is true:
     * `java.util.logging.MemoryHandler.target` has to be there. It is consistent with what this
     * handler is -- it writes nowhere by itself, so one with no target is not a handler with a poor
     * configuration but a hole the whole log falls through. Better to fail on construction.
     *
     * @throws RuntimeException if the configuration does not say which handler to dump to
     */
    public MemoryHandler() {
        LogManager m = LogManager.getLogManager();
        String cname = "java.util.logging.MemoryHandler";
        this.pushLevel = m.getLevelProperty(cname + ".push", Level.SEVERE);
        int tam = m.getIntProperty(cname + ".size", DEFAULT_SIZE);
        if (tam <= 0) {
            tam = DEFAULT_SIZE;
        }
        this.buffer = new LogRecord[tam];
        this.setLevel(m.getLevelProperty(cname + ".level", Level.ALL));
        this.setFilter(m.getFilterProperty(cname + ".filter", null));
        this.setFormatter(m.getFormatterProperty(cname + ".formatter", new SimpleFormatter()));
        String targetName = m.getProperty(cname + ".target");
        if (targetName == null) {
            throw new RuntimeException("The handler " + cname + " does not specify a target");
        }
        try {
            this.target = (Handler) LogManager.build(targetName.trim());
        } catch (Exception e) {
            throw new RuntimeException("MemoryHandler can't load handler target \"" + targetName + "\"");
        }
    }

    /**
     * @throws IllegalArgumentException if `size` is not positive -- a ring of zero keeps nothing and
     *         a negative one means nothing
     * @throws NullPointerException if `target` or `pushLevel` are `null`
     */
    public MemoryHandler(Handler target, int size, Level pushLevel) {
        if (size <= 0) {
            throw new IllegalArgumentException("size: " + size);
        }
        if (target == null) {
            throw new NullPointerException("target");
        }
        if (pushLevel == null) {
            throw new NullPointerException("pushLevel");
        }
        LogManager m = LogManager.getLogManager();
        String cname = "java.util.logging.MemoryHandler";
        this.setLevel(m.getLevelProperty(cname + ".level", Level.ALL));
        this.setFilter(m.getFilterProperty(cname + ".filter", null));
        this.setFormatter(m.getFormatterProperty(cname + ".formatter", new SimpleFormatter()));
        this.target = target;
        this.pushLevel = pushLevel;
        this.buffer = new LogRecord[size];
    }

    /**
     * It stores the record in the ring, and dumps everything if that record reaches the push level.
     *
     * <p>What matters is what it does **not** do: it does not hand the record to the target. The
     * target gets it later, at the dump, and that is why a memory handler costs no I/O while nothing
     * happens.
     */
    public synchronized void publish(LogRecord record) {
        if (!this.isLoggable(record)) {
            return;
        }
        int i = (this.start + this.howMany) % this.buffer.length;
        this.buffer[i] = record;
        if (this.howMany < this.buffer.length) {
            this.howMany = this.howMany + 1;
        } else {
            // It was full: the write overwrote the oldest, so the oldest is the next one.
            this.start = (this.start + 1) % this.buffer.length;
        }
        if (record.getLevel().intValue() < this.pushLevel.intValue()) {
            return;
        }
        this.push();
    }

    /**
     * It dumps what has piled up to the target, oldest to newest, and empties the ring.
     *
     * <p>Emptying it is part of the contract and not a detail: without that, two failures in a row
     * would write the same records twice and the log would lie about how many times each thing
     * happened.
     *
     * <p>Each record goes through the **target's** {@link Handler#isLoggable}, which is where the
     * target's level is applied. This handler's was already applied on the way in.
     */
    public synchronized void push() {
        int i = 0;
        while (i < this.howMany) {
            this.target.publish(this.buffer[(this.start + i) % this.buffer.length]);
            i = i + 1;
        }
        this.start = 0;
        this.howMany = 0;
    }

    /** It flushes the **target**; what is in the ring has not been written yet and needs no flushing. */
    public void flush() {
        this.target.flush();
    }

    /**
     * It closes the target and switches itself off.
     *
     * <p>And it **discards** whatever is left in the ring, which is what the contract says. It sounds
     * like a loss and it is consistent: what is in the ring are messages that never came to interest
     * anybody, and dumping them on close would turn every program's ending into a full dump of fine
     * logging.
     */
    public void close() throws SecurityException {
        this.target.close();
        this.setLevel(Level.OFF);
    }

    /** The one what has piled up is dumped to. */
    public synchronized void setPushLevel(Level newLevel) throws SecurityException {
        if (newLevel == null) {
            throw new NullPointerException("newLevel");
        }
        this.pushLevel = newLevel;
    }

    public synchronized Level getPushLevel() {
        return this.pushLevel;
    }

    /**
     * Whether the record enters the **ring**.
     *
     * <p>Which is different from whether it will be seen: it can enter and then be discarded on close
     * without there ever having been a push.
     */
    public boolean isLoggable(LogRecord record) {
        return super.isLoggable(record);
    }
}
