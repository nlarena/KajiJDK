package jdk.jshell.tool;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Locale;
import java.util.Map;
import java.util.prefs.Preferences;

/**
 * Builds and runs the `jshell` tool from inside a program.
 *
 * <p>It is the only supported way of embedding the console: `jshell` as a command is a thin wrapper
 * over this. It serves an IDE that wants an integrated console, or a tutorial that starts a session
 * with its own classes already loaded.
 *
 * <h2>Why there are so many streams</h2>
 *
 * <p>`jshell` mixes **three** conversations that in an ordinary program would be one: what the tool
 * says to the user, what the user's code prints, and the diagnostics. That is why
 * {@link #out(PrintStream, PrintStream, PrintStream)} takes three and not one: an IDE wants to paint
 * each of them differently, and with a single stream it cannot tell them apart.
 *
 * <p>{@link #in(InputStream, InputStream)} takes two for the same reason the other way round: what
 * the user types into the console and what the **user's code** reads from `System.in` are not the
 * same input.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>{@link #builder()} throws {@link UnsupportedOperationException}. It is not a missing piece:
 * `jshell` is an incremental compiler plus a remote VM plus a protocol between the two, and none of
 * that is in this library.
 *
 * <p>The interface is complete all the same --with the two {@code default} methods implemented--
 * because it is what a program compiles against. Returning a builder that accepted the whole
 * configuration and failed only at {@code run()} would be worse: the program would believe the
 * console was there and would find out it was not at the moment when it can no longer do anything
 * about it.
 *
 * @since 9
 */
public interface JavaShellToolBuilder {

    /**
     * A new builder.
     *
     * <p><b>Not implemented in this library.</b> See the interface's note.
     *
     * @throws UnsupportedOperationException always, in this library
     */
    static JavaShellToolBuilder builder() {
        throw new UnsupportedOperationException(
                "no jshell tool in this library: it needs an incremental compiler, a remote "
                + "execution VM and the protocol between them, none of which are present");
    }

    /**
     * The two input streams: the one for the commands and the one the user's code sees.
     *
     * @param cmdIn where the commands and the snippets typed in come from
     * @param userIn what the user's code reads from `System.in`, or `null` for it to be the same as
     *     `cmdIn`
     */
    JavaShellToolBuilder in(InputStream cmdIn, InputStream userIn);

    /**
     * A single output stream for all three conversations.
     *
     * @param output where everything that comes out goes
     */
    JavaShellToolBuilder out(PrintStream output);

    /**
     * The three output streams separately. See the interface's note.
     *
     * @param cmdOut what the tool says to the user
     * @param console the console's echo
     * @param userOut what the user's code prints
     */
    JavaShellToolBuilder out(PrintStream cmdOut, PrintStream console, PrintStream userOut);

    /**
     * A single error stream.
     *
     * @param error where the diagnostics and the user code's errors go
     */
    JavaShellToolBuilder err(PrintStream error);

    /**
     * The two error streams separately.
     *
     * @param cmdErr the tool's diagnostics
     * @param userErr what the user's code writes to `System.err`
     */
    JavaShellToolBuilder err(PrintStream cmdErr, PrintStream userErr);

    /**
     * Where the history and the options are kept between sessions.
     *
     * @param prefs the preferences node
     */
    JavaShellToolBuilder persistence(Preferences prefs);

    /**
     * The same, in an in-memory map.
     *
     * <p>It serves a session that is to leave no trace, and the tests.
     */
    JavaShellToolBuilder persistence(Map<String, String> prefsMap);

    /**
     * The environment variables the user's code sees.
     *
     * @param env the environment, or `null` for the process's own
     */
    JavaShellToolBuilder env(Map<String, String> env);

    /** The language of the tool's messages. */
    JavaShellToolBuilder locale(Locale locale);

    /**
     * Whether the prompt and the echo have to come out through the output stream.
     *
     * <p>With `true` the whole session can be transcribed by reading a single stream, which is what
     * an automated test needs. It is `false` by default, because in a terminal the echo is the
     * terminal's job and it would come out twice.
     */
    JavaShellToolBuilder promptCapture(boolean capture);

    /**
     * Whether the input is to be treated as an interactive terminal.
     *
     * <p>By default it changes nothing: the tool detects it on its own. This method is for forcing it
     * when the detection cannot get it right --a redirected input that wants line editing anyway.
     */
    default JavaShellToolBuilder interactiveTerminal(boolean interactiveTerminal) {
        return this;
    }

    /**
     * The window's size, for the tools that have no terminal to find it out from.
     *
     * <p>By default it changes nothing, like {@link #interactiveTerminal}.
     */
    default JavaShellToolBuilder windowSize(int columns, int rows) {
        return this;
    }

    /**
     * Runs the tool with those command-line arguments.
     *
     * <p>It blocks until the session ends.
     *
     * @throws Exception whatever fails while running it
     */
    void run(String... arguments) throws Exception;

    /**
     * Runs the tool and returns its exit code.
     *
     * <p>By default it is {@link #run} and a zero: {@code run} reports failures by throwing, so if it
     * came back it went well. An implementation that can tell degrees of failure apart redefines it.
     *
     * @throws Exception whatever fails while running it
     */
    default int start(String... arguments) throws Exception {
        run(arguments);
        return 0;
    }
}
