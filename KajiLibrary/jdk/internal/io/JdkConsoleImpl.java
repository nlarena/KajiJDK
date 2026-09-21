package jdk.internal.io;

import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Locale;

/**
 * KajiLibrary's jdk.internal.io.JdkConsoleImpl -- the default console.
 *
 * <p><strong>This VM has no terminal, and this class says so instead of pretending.</strong> The
 * output goes to a sink and the input is at end of file, exactly as {@link java.io.Console} --which
 * is the one that would use it-- already documents from before. `System.console()` returns `null`,
 * so in practice nobody gets this far.
 *
 * <p>It is worth explaining why that does **not** turn it into a member that lies, which is the
 * line this project does not cross. None of these methods promises that there is somebody on the
 * other side:
 *
 * <ul>
 * <li>{@link #readLine()} returns `null` at end of input, and that is what the contract says
 *     happens when the input has run out. An empty input **is** run out from the start.</li>
 * <li>{@link #readPassword()} returns `null` for the same reason.</li>
 * <li>Writing to a console with no terminal has no observable result, so a sink is a correct
 *     implementation and not a simulation.</li>
 * </ul>
 *
 * <p>What would be lying is that `System.console()` returned one of these: the program would
 * believe that there is a user watching. That is why it returns `null`, and that is why this class
 * is reachable only by whoever builds it by hand knowing what they are doing.
 *
 * <p>The only visible difference with the JDK is in `readPassword`: over there the echo is turned
 * off by touching the terminal, and here there is no terminal to touch. There is nothing to turn
 * off and nothing that is shown.
 */
public final class JdkConsoleImpl implements JdkConsole {

    private final Charset inCharset;
    private final Charset outCharset;
    private final PrintWriter writer;
    private final Reader reader;

    /**
     * @param inCharset the character set of the input
     * @param outCharset that of the output
     */
    public JdkConsoleImpl(Charset inCharset, Charset outCharset) {
        this.inCharset = inCharset;
        this.outCharset = outCharset;
        this.writer = new PrintWriter(new Sink(), true);
        this.reader = new AtEndOfFile();
    }

    public PrintWriter writer() {
        return this.writer;
    }

    public Reader reader() {
        return this.reader;
    }

    public JdkConsole println(Object obj) {
        this.writer.println(obj);
        this.writer.flush();
        return this;
    }

    public JdkConsole print(Object obj) {
        this.writer.print(obj);
        this.writer.flush();
        return this;
    }

    public JdkConsole format(Locale locale, String format, Object... args) {
        this.writer.write(String.format(locale, format, args));
        this.writer.flush();
        return this;
    }

    // The message is written all the same before reading: that the reading is not going to give
    // anything does not change the order of the operations, and a caller that looks at the output
    // has to see the request.
    public String readLine(Locale locale, String format, Object... args) {
        this.format(locale, format, args);
        return this.readLine();
    }

    public String readLine() {
        return null;
    }

    public char[] readPassword(Locale locale, String format, Object... args) {
        this.format(locale, format, args);
        return this.readPassword();
    }

    public char[] readPassword() {
        return null;
    }

    public void flush() {
        this.writer.flush();
    }

    /**
     * The character set of the **output**.
     *
     * <p>The constructor receives two and this method returns one: the one of the JDK returns that
     * of the output, which is the one that governs what is written. The one of the input is kept
     * because the constructor declares it and because it is part of the state of the console, not
     * because it is needed in order to answer this.
     */
    public Charset charset() {
        return this.outCharset;
    }

    /** The character set of the input, for whoever needs it inside the package. */
    Charset inputCharset() {
        return this.inCharset;
    }

    // With no terminal, writing has no observable effect. It is discarded instead of accumulated:
    // keeping text nobody is going to read would be a silent loss of memory.
    private static final class Sink extends Writer {
        public void write(char[] buf, int off, int len) {
        }

        public void flush() {
        }

        public void close() {
        }
    }

    // An input that has already run out. `read` returns -1, which is how "there is no more" is
    // said.
    private static final class AtEndOfFile extends Reader {
        public int read(char[] buf, int off, int len) {
            return -1;
        }

        public void close() {
        }
    }
}
