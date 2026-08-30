package java.io;

import java.nio.charset.Charset;
import java.util.Locale;

// KajiLibrary's java.io.Console. KajiJDK runs with no controlling terminal, so System.console()
// always returns null and no Console is ever handed out -- this type exists to give that method a
// return type and to mirror the JDK's surface. Its instance methods are written faithfully but are
// unreachable in practice: the internal writer is a discarding sink and the reader is at
// end-of-stream, so the interactive reads answer null as they would with no input available.
public class Console implements Flushable {

    private final PrintWriter pw = new PrintWriter(new SinkWriter(), true);
    private final Reader rd = new EofReader();

    // The VM would construct a Console when a terminal is present; KajiJDK never has one, so this
    // is never called from System.console() (which returns null).
    private Console() {
    }

    /** The {@link PrintWriter} for this console's output. */
    public PrintWriter writer() {
        return this.pw;
    }

    /** The {@link Reader} for this console's input. */
    public Reader reader() {
        return this.rd;
    }

    /** Writes a formatted string to this console's output stream. */
    public Console format(String fmt, Object... args) {
        this.pw.write(String.format(fmt, args));
        this.pw.flush();
        return this;
    }

    /** Writes a locale-formatted string to this console's output stream. */
    public Console format(Locale locale, String fmt, Object... args) {
        this.pw.write(String.format(locale, fmt, args));
        this.pw.flush();
        return this;
    }

    /** A convenience for {@link #format(String, Object...)}. */
    public Console printf(String fmt, Object... args) {
        return this.format(fmt, args);
    }

    /** A convenience for {@link #format(Locale, String, Object...)}. */
    public Console printf(Locale locale, String fmt, Object... args) {
        return this.format(locale, fmt, args);
    }

    /** Reads a line. Always null in KajiJDK: there is no console input. */
    public String readLine() {
        return null;
    }

    /** Prints a prompt, then reads a line. The read is always null (no console input). */
    public String readLine(String fmt, Object... args) {
        this.format(fmt, args);
        return null;
    }

    /** Prints a locale-formatted prompt, then reads a line. Always null (no console input). */
    public String readLine(Locale locale, String fmt, Object... args) {
        this.format(locale, fmt, args);
        return null;
    }

    /** Reads a password with echoing disabled. Always null in KajiJDK: there is no console input. */
    public char[] readPassword() {
        return null;
    }

    /** Prints a prompt, then reads a password. Always null (no console input). */
    public char[] readPassword(String fmt, Object... args) {
        this.format(fmt, args);
        return null;
    }

    /** Prints a locale-formatted prompt, then reads a password. Always null (no console input). */
    public char[] readPassword(Locale locale, String fmt, Object... args) {
        this.format(locale, fmt, args);
        return null;
    }

    /** Flushes this console's output. */
    public void flush() {
        this.pw.flush();
    }

    /** {@return the console's charset} — the platform default in KajiJDK. */
    public Charset charset() {
        return Charset.defaultCharset();
    }

    /** Whether this console is connected to a terminal. Always false in KajiJDK. */
    public boolean isTerminal() {
        return false;
    }

    // A Writer that discards everything: the honest backing for a console that is never real.
    private static final class SinkWriter extends Writer {
        public void write(char[] cbuf, int off, int len) {
        }

        public void flush() {
        }

        public void close() {
        }
    }

    // A Reader permanently at end-of-stream: there is no console input to read.
    private static final class EofReader extends Reader {
        public int read(char[] cbuf, int off, int len) {
            return -1;
        }

        public void close() {
        }
    }
}
