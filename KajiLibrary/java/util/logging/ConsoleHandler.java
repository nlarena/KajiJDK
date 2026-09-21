package java.util.logging;

/**
 * KajiLibrary's java.util.logging.ConsoleHandler -- it writes to standard **error**.
 *
 * <p>To `System.err` and not to `System.out`, on purpose: the log is not the program's output, and
 * mixing them ruins the program's use in a pipeline.
 *
 * <p>And it flushes after each message. It is slower and it is right for a console: a log that sits
 * in the buffer when the program crashes is no use at all, and the last message before crashing is
 * usually the one that matters.
 */
public class ConsoleHandler extends StreamHandler {

    public ConsoleHandler() {
        // Its own properties override `StreamHandler`'s, which the `super()` already applied.
        this.configure("java.util.logging.ConsoleHandler");
        this.setOutputStream(System.err);
    }

    public void publish(LogRecord record) {
        super.publish(record);
        this.flush();
    }

    /** It flushes but does **not** close: `System.err` is not its own. */
    public void close() {
        this.flush();
    }
}
