package jdk.jshell;

import java.io.IOError;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.Charset;

/**
 * The console the code written in `jshell` sees.
 *
 * <p>When a snippet calls {@link java.lang.System#console()}, what reaches it is not the process's
 * console: it is this one, which may live on the other side of a connection --the user's code runs
 * on **another VM**-- or inside an IDE's window.
 *
 * <p>That is why {@link #charset()} is here: whoever types and whoever executes may be on machines
 * with different encodings, and the user's code has a right to know which one it is reading in.
 *
 * @since 22
 */
public interface JShellConsole {

    /** Where the user's code writes. */
    PrintWriter writer();

    /** Where the user's code reads from. */
    Reader reader();

    /**
     * Reads a line, showing that text first.
     *
     * @param prompt what is shown before reading, or `null` for nothing
     * @return the line without the break, or `null` if the input ran out
     * @throws IOError if the read fails
     */
    String readLine(String prompt) throws IOError;

    /**
     * Reads a line without showing what is typed.
     *
     * <p>It returns `char[]` and not `String` for the usual reason with passwords: an array can be
     * wiped there and then, and a string stays in the heap until the collector feels like it.
     *
     * @param prompt what is shown before reading, or `null` for nothing
     * @return the characters, or `null` if the input ran out
     * @throws IOError if the read fails
     */
    char[] readPassword(String prompt) throws IOError;

    /** Empties whatever is pending to be written. */
    void flush();

    /** The encoding this console reads and writes in. */
    Charset charset();
}
