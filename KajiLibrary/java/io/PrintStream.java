package java.io;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

// KajiLibrary's java.io.PrintStream — the text-oriented output stream behind {@code System.out} and
// {@code System.err}. It adds the {@code print}/{@code println}/{@code printf} family and never
// throws {@code IOException} (it sets an internal error flag instead), exactly as the reference.
//
// Everything funnels through ONE native text seam, {@link #writeString}, which appends a string's
// bytes to the program's stdout without a newline; {@code println(int)} and {@code println(String)}
// keep their own native fast path, and the byte writes encode through the same seam. The underlying
// {@code out} is therefore only decorative for the console (it may be null) — the VM owns the sink.
// A {@code PrintStream} over the console works; one asked to open a FILE fails honestly, because
// KajiJDK has no filesystem to write to, so the file constructors throw {@link FileNotFoundException}.
public class PrintStream extends FilterOutputStream implements Appendable, Closeable {

    private boolean autoFlush;
    private boolean trouble;
    private Charset charset;

    // The single text seam: write the string's bytes to stdout, no newline. Native, like the two
    // `println` overloads below, because the actual console write lives in the VM.
    private native void writeString(String s);

    // ---- constructors ----

    public PrintStream(OutputStream out) {
        this(out, false);
    }

    public PrintStream(OutputStream out, boolean autoFlush) {
        super(out);
        this.autoFlush = autoFlush;
        this.charset = StandardCharsets.UTF_8;
    }

    public PrintStream(OutputStream out, boolean autoFlush, String encoding)
            throws UnsupportedEncodingException {
        super(out);
        this.autoFlush = autoFlush;
        this.charset = lookup(encoding);
    }

    public PrintStream(OutputStream out, boolean autoFlush, Charset charset) {
        super(out);
        this.autoFlush = autoFlush;
        this.charset = charset;
    }

    public PrintStream(String fileName) throws FileNotFoundException {
        super(null);
        throw new FileNotFoundException("KajiJDK has no filesystem to write: " + fileName);
    }

    public PrintStream(String fileName, String csn)
            throws FileNotFoundException, UnsupportedEncodingException {
        super(null);
        lookup(csn);
        throw new FileNotFoundException("KajiJDK has no filesystem to write: " + fileName);
    }

    public PrintStream(String fileName, Charset charset) throws IOException {
        super(null);
        throw new FileNotFoundException("KajiJDK has no filesystem to write: " + fileName);
    }

    public PrintStream(File file) throws FileNotFoundException {
        super(null);
        throw new FileNotFoundException("KajiJDK has no filesystem to write");
    }

    public PrintStream(File file, String csn)
            throws FileNotFoundException, UnsupportedEncodingException {
        super(null);
        lookup(csn);
        throw new FileNotFoundException("KajiJDK has no filesystem to write");
    }

    public PrintStream(File file, Charset charset) throws IOException {
        super(null);
        throw new FileNotFoundException("KajiJDK has no filesystem to write");
    }

    private static Charset lookup(String csn) throws UnsupportedEncodingException {
        try {
            return Charset.forName(csn);
        } catch (RuntimeException e) {
            throw new UnsupportedEncodingException(csn);
        }
    }

    // ---- lifecycle ----

    /**
     * Vacia lo pendiente.
     *
     * <p>No declara `throws IOException`, como en el JDK: un `PrintStream` **no tira** por fallas de
     * E/S, las anota y se las cuenta a quien pregunte por {@link #checkError()}. Es toda la razon de
     * ser de esta clase --que `System.out.println` no obligue a atrapar nada-- y por eso el `catch`
     * de aca no es un descuido sino el contrato.
     */
    public void flush() {
        if (out != null) {
            try {
                out.flush();
            } catch (IOException e) {
                this.setError();
            }
        }
    }

    /** Cierra. Sin `throws`, como el JDK: la falla se anota y se consulta con {@link #checkError()}. */
    public void close() {
        flush();
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                this.setError();
            }
        }
    }

    /** Whether an error has been seen on this stream. Never, here — the console does not fail. */
    /**
     * Marca que este flujo tuvo un problema.
     *
     * <p>Existe porque `PrintStream` **no tira**: los `print` tragan la `IOException` y encienden
     * esta bandera, que es la unica forma de enterarse. Una subclase que haga su propia escritura
     * necesita poder encenderla, o su fallo seria invisible.
     */
    protected void setError() {
        this.trouble = true;
    }

    /**
     * Apaga la bandera de error.
     *
     * <p>Es `protected` a proposito y el javadoc del JDK lo dice de frente: permite que un flujo se
     * "recupere" de un error, y eso **oculta** el fallo a quien llame `checkError` despues. Solo
     * tiene sentido para una subclase que sepa que el problema se resolvio de verdad.
     */
    protected void clearError() {
        this.trouble = false;
    }

    public boolean checkError() {
        return this.trouble;
    }

    /** The charset used to encode text. */
    public Charset charset() {
        return this.charset;
    }

    // ---- raw bytes ----

    public void write(int b) {
        writeString(String.valueOf((char) (b & 0xFF)));
        if (autoFlush) {
            flush();
        }
    }

    public void write(byte[] buf, int off, int len) {
        writeString(new String(buf, off, len));
        if (autoFlush) {
            flush();
        }
    }

    public void write(byte[] buf) throws IOException {
        write(buf, 0, buf.length);
    }

    public void writeBytes(byte[] buf) {
        write(buf, 0, buf.length);
    }

    // ---- print ----

    public void print(boolean b) {
        print(String.valueOf(b));
    }

    public void print(char c) {
        print(String.valueOf(c));
    }

    public void print(int i) {
        print(String.valueOf(i));
    }

    public void print(long l) {
        print(String.valueOf(l));
    }

    public void print(float f) {
        print(String.valueOf(f));
    }

    public void print(double d) {
        print(String.valueOf(d));
    }

    public void print(char[] s) {
        print(String.valueOf(s));
    }

    public void print(String s) {
        writeString(s == null ? "null" : s);
        if (autoFlush) {
            flush();
        }
    }

    public void print(Object obj) {
        print(String.valueOf(obj));
    }

    // ---- println ----

    public void println() {
        writeString(System.lineSeparator());
        if (autoFlush) {
            flush();
        }
    }

    public void println(boolean x) {
        println(String.valueOf(x));
    }

    public void println(char x) {
        println(String.valueOf(x));
    }

    // `println(int)` and `println(String)` have plain Java bodies (matching the reference, which
    // does not declare them native), but the VM still intercepts them with its fast-path intrinsic
    // — intrinsic dispatch keys on (class, name, descriptor) and ignores the `native` flag — so the
    // body is the correct fallback while the VM writes value-plus-newline directly.
    public void println(int x) {
        println(String.valueOf(x));
    }

    public void println(long x) {
        println(String.valueOf(x));
    }

    public void println(float x) {
        println(String.valueOf(x));
    }

    public void println(double x) {
        println(String.valueOf(x));
    }

    public void println(char[] x) {
        println(String.valueOf(x));
    }

    public void println(String x) {
        writeString(x == null ? "null" : x);
        writeString(System.lineSeparator());
        if (autoFlush) {
            flush();
        }
    }

    public void println(Object x) {
        println(String.valueOf(x));
    }

    // ---- formatted ----

    public PrintStream printf(String format, Object... args) {
        return format(format, args);
    }

    public PrintStream printf(Locale l, String format, Object... args) {
        return format(l, format, args);
    }

    public PrintStream format(String format, Object... args) {
        print(String.format(format, args));
        return this;
    }

    public PrintStream format(Locale l, String format, Object... args) {
        print(String.format(l, format, args));
        return this;
    }

    // ---- appendable ----

    public PrintStream append(CharSequence csq) {
        print(csq == null ? "null" : csq.toString());
        return this;
    }

    public PrintStream append(CharSequence csq, int start, int end) {
        CharSequence cs = csq == null ? "null" : csq;
        print(cs.subSequence(start, end).toString());
        return this;
    }

    public PrintStream append(char c) {
        print(c);
        return this;
    }
}
