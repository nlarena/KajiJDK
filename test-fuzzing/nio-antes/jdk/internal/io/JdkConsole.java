package jdk.internal.io;

import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Locale;

/**
 * KajiLibrary's jdk.internal.io.JdkConsole -- the interface that is **behind**
 * {@link java.io.Console}.
 *
 * <p>The separation exists in the JDK for a concrete reason: the real console needs things the
 * library cannot do on its own --reading a password without it being seen on the screen, knowing
 * whether there is a terminal connected-- so `java.io.Console` delegates to an implementation the
 * runtime provides. This interface is that contract.
 *
 * <p>It is a **pure declaration**: it does not promise behaviour, it describes it. That is why it
 * can be written whole and honestly even though this VM has no terminal. Whoever implements it
 * decides what they can fulfil; {@link JdkConsoleImpl} says so explicitly in its header.
 *
 * <p>The pairs of methods with and without {@link Locale} are not sugar: the one that carries a
 * locale formats the message that is shown before reading, and the one that does not carry it is
 * the bare reading.
 */
public interface JdkConsole {

    /** The writer of this console. */
    PrintWriter writer();

    /** The reader of this console. */
    Reader reader();

    /** It writes the object and a line break. */
    JdkConsole println(Object obj);

    /** It writes the object, with no break. */
    JdkConsole print(Object obj);

    /** It writes a formatted string. */
    JdkConsole format(Locale locale, String format, Object... args);

    /** It shows the formatted message and reads a line. */
    String readLine(Locale locale, String format, Object... args);

    /** It reads a line. */
    String readLine();

    /**
     * It shows the formatted message and reads a password **with no echo**.
     *
     * <p>It returns `char[]` and not `String` on purpose, and it is the only part of this interface
     * where the type carries an intention: an array can be overwritten as soon as it has been used,
     * and a string stays in the pool until the collector picks it up.
     */
    char[] readPassword(Locale locale, String format, Object... args);

    /** It reads a password with no echo. */
    char[] readPassword();

    /** It flushes what is pending to be written. */
    void flush();

    /** The character set of this console. */
    Charset charset();
}
