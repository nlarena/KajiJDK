package java.io;

// Same-package import works around the frozen javac's finder (finding #4).
import java.io.Writer;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Locale;

// KajiLibrary's java.io.PrintWriter — the decorator that makes a Writer convenient.
//
// Everything here could be written by the caller: print(int) is write(Integer.toString(i)),
// println() is a '\n'. Wrapping it up matters for two reasons. First, it is a decorator,
// so the convenience attaches to ANY Writer — a file, a StringWriter in a test, a socket —
// rather than being a method on one particular sink. Second, it changes the error contract:
// a PrintWriter never lets an I/O failure out. In the JDK the wrapped Writer's IOException
// is swallowed and latched in a flag you read with checkError(). The reasoning is that
// print/println are used in the places least willing to handle failure — logging, tracing,
// a usage message on the way to exit — and there the least bad answer to a broken sink is
// to carry on quietly.
//
// KajiLibrary's java.io is throws-free (see IOException), so nothing underneath can throw
// yet; the one error the flag really does record is writing to a PrintWriter that has been
// closed. The API is the point: this is where a stream stops reporting failures upward and
// starts recording them.
//
// autoFlush is the other half of the convenience. A buffer that is never flushed is a
// message that never arrives, and a program that prints a prompt and then blocks reading
// the answer will hang forever if the prompt is still in a buffer. With autoFlush on,
// every println pushes the line out.
public class PrintWriter extends Writer {

    // The wrapped sink. Protected because the JDK exposes it and subclasses print through
    // it; null after close(), which is how a use-after-close is detected.
    protected Writer out;

    private boolean autoFlush;

    // Latched, never cleared by accident: once something has gone wrong the caller must
    // ask (checkError) or explicitly clear it. A flag that reset itself would be useless
    // to a caller who only checks at the end.
    private boolean trouble;

    public PrintWriter(Writer out) {
        this.out = out;
        this.autoFlush = false;
        this.trouble = false;
    }

    public PrintWriter(Writer out, boolean autoFlush) {
        this.out = out;
        this.autoFlush = autoFlush;
        this.trouble = false;
    }

    // Over a byte stream: encode through an OutputStreamWriter, buffer it, and print through that.
    public PrintWriter(OutputStream out) {
        this(new BufferedWriter(new OutputStreamWriter(out)), false);
    }

    public PrintWriter(OutputStream out, boolean autoFlush) {
        this(new BufferedWriter(new OutputStreamWriter(out)), autoFlush);
    }

    public PrintWriter(OutputStream out, boolean autoFlush, Charset charset) {
        this(new BufferedWriter(new OutputStreamWriter(out, charset)), autoFlush);
    }

    // Over a named file: KajiJDK has no filesystem to open, so these fail honestly. The implicit
    // super() runs before the throw; the half-built object is discarded.
    public PrintWriter(String fileName) throws FileNotFoundException {
        throw new FileNotFoundException("KajiJDK has no filesystem to write: " + fileName);
    }

    public PrintWriter(String fileName, String csn)
            throws FileNotFoundException, UnsupportedEncodingException {
        throw new FileNotFoundException("KajiJDK has no filesystem to write: " + fileName);
    }

    public PrintWriter(String fileName, Charset charset) throws IOException {
        throw new FileNotFoundException("KajiJDK has no filesystem to write: " + fileName);
    }

    public PrintWriter(File file) throws FileNotFoundException {
        throw new FileNotFoundException("KajiJDK has no filesystem to write");
    }

    public PrintWriter(File file, String csn)
            throws FileNotFoundException, UnsupportedEncodingException {
        throw new FileNotFoundException("KajiJDK has no filesystem to write");
    }

    public PrintWriter(File file, Charset charset) throws IOException {
        throw new FileNotFoundException("KajiJDK has no filesystem to write");
    }

    // The single place a failure is turned into a flag instead of an exception. Every
    // write goes through it, so there is exactly one gate to reason about.
    private boolean open() {
        if (this.out == null) {
            this.trouble = true;
            return false;
        }
        return true;
    }

    public void flush() {
        if (this.open()) {
            this.out.flush();
        }
    }

    public void close() {
        if (this.out != null) {
            this.out.flush();
            this.out.close();
            this.out = null;
        }
    }

    // Flushes first: a caller asking "did this work?" wants the answer for everything it
    // has written, including whatever is still sitting in a buffer downstream.
    public boolean checkError() {
        if (this.out != null) {
            this.flush();
        }
        return this.trouble;
    }

    // Protected, for a subclass that writes through `out` directly and hits a problem this
    // class never sees.
    protected void setError() {
        this.trouble = true;
    }

    protected void clearError() {
        this.trouble = false;
    }

    // --- the Writer contract, routed through the gate ---

    public void write(int c) {
        if (this.open()) {
            this.out.write(c);
        }
    }

    public void write(char[] buf, int off, int len) {
        if (this.open()) {
            this.out.write(buf, off, len);
        }
    }

    public void write(char[] buf) {
        this.write(buf, 0, buf.length);
    }

    public void write(String s, int off, int len) {
        if (this.open()) {
            this.out.write(s, off, len);
        }
    }

    // Handed to the sink whole rather than through Writer's char[]-copying default: a
    // print writer's traffic is almost entirely Strings.
    public void write(String s) {
        if (this.open()) {
            this.out.write(s);
        }
    }

    // --- print: value in, characters out ---
    //
    // Nine overloads that all end at write(String). They exist because Java has no
    // universal "render this" for primitives — a printed `int` and a printed `Object` need
    // different code — and because overloading keeps that invisible at the call site.

    public void print(boolean b) {
        if (b) {
            this.write("true");
        } else {
            this.write("false");
        }
    }

    public void print(char c) {
        this.write(c);
    }

    public void print(int i) {
        this.write(Integer.toString(i));
    }

    public void print(long l) {
        this.write(Long.toString(l));
    }

    public void print(float f) {
        this.write(Float.toString(f));
    }

    public void print(double d) {
        this.write(Double.toString(d));
    }

    public void print(char[] s) {
        this.write(s);
    }

    public void print(String s) {
        if (s == null) {
            this.write("null");
        } else {
            this.write(s);
        }
    }

    // Prints "null" for a null reference rather than throwing: consistent with print(String)
    // and with the spirit of the class — a trace statement must not be what kills a program.
    public void print(Object obj) {
        this.write(String.valueOf(obj));
    }

    // --- println: print, terminate the line, and (if asked) push it out ---

    public void println() {
        this.newLine();
    }

    public void println(boolean x) {
        this.print(x);
        this.newLine();
    }

    public void println(char x) {
        this.print(x);
        this.newLine();
    }

    public void println(int x) {
        this.print(x);
        this.newLine();
    }

    public void println(long x) {
        this.print(x);
        this.newLine();
    }

    public void println(float x) {
        this.print(x);
        this.newLine();
    }

    public void println(double x) {
        this.print(x);
        this.newLine();
    }

    public void println(char[] x) {
        this.print(x);
        this.newLine();
    }

    public void println(String x) {
        this.print(x);
        this.newLine();
    }

    public void println(Object x) {
        this.print(x);
        this.newLine();
    }

    // The line ending is '\n' rather than the platform's: KajiLibrary has no
    // System.lineSeparator() yet, and a `static final` String on another class is not a
    // workaround either (finding #110 compiles that read to a getfield that traps).
    //
    // This is where autoFlush earns its keep — the flush is tied to the END OF A LINE, not
    // to every write, because a line is the unit a reader on the other side waits for.
    private void newLine() {
        this.write('\n');
        if (this.autoFlush) {
            this.flush();
        }
    }

    // --- formatted output ---
    //
    // Returns `this`, so calls chain. The Locale-taking overloads the JDK also has are
    // left out for now: KajiLibrary's subset stays a subset, and the locale-free forms are
    // the ones the pattern is about.
    //
    // Note both bodies pass `args` on as the array it already is, never as a spread
    // argument list. That is load-bearing today: our javac emits no ACC_VARARGS, and a
    // spread call to a method loaded from the classpath is silently dropped rather than
    // rejected (see the report accompanying this work). Passing the array through is the
    // shape that compiles correctly.

    public PrintWriter printf(String format, Object... args) {
        return this.format(format, args);
    }

    public PrintWriter format(String format, Object... args) {
        String s = String.format(format, args);
        this.write(s);
        if (this.autoFlush) {
            this.flush();
        }
        return this;
    }

    public PrintWriter printf(Locale l, String format, Object... args) {
        return this.format(l, format, args);
    }

    public PrintWriter format(Locale l, String format, Object... args) {
        String s = String.format(l, format, args);
        this.write(s);
        if (this.autoFlush) {
            this.flush();
        }
        return this;
    }

    // --- appendable ---
    //
    // Returns `this` (a PrintWriter), covariantly narrowing Writer/Appendable — the compiler
    // synthesises those bridges. A null CharSequence prints as "null", per the contract.

    public PrintWriter append(CharSequence csq) {
        this.write(csq == null ? "null" : csq.toString());
        return this;
    }

    public PrintWriter append(CharSequence csq, int start, int end) {
        CharSequence cs = csq == null ? "null" : csq;
        this.write(cs.subSequence(start, end).toString());
        return this;
    }

    public PrintWriter append(char c) {
        this.write(c);
        return this;
    }
}
