package java.util.logging;

/**
 * KajiLibrary's java.util.logging.Filter -- the second criterion, after the level.
 *
 * <p>The level decides by **importance** and this one by whatever: the class's name, the message's
 * content, the time. It is applied after the level on purpose, because comparing two integers is
 * much cheaper than calling the user's code.
 */
public interface Filter {

    /** Whether that record should be published. */
    boolean isLoggable(LogRecord record);
}
