package java.lang;

import java.io.PrintStream;
import java.io.PrintWriter;

// KajiLibrary's java.lang.Throwable — the superclass of all errors and exceptions. Carries a detail
// message and an optional cause (another Throwable), with the JDK's `cause == this` sentinel meaning
// "not yet initialised". The message/cause plumbing is pure Java. Stack-trace **capture** needs the
// VM (there is no native `fillInStackTrace`), so the trace is empty unless one is set with
// `setStackTrace`; the suppressed-exception list (try-with-resources) is modelled in full.
public class Throwable {

    private String message;
    private Throwable cause;
    // El stack trace: `null` significa "sin capturar" (KajiJDK no captura de forma nativa), y se
    // reporta como un array vacío. `setStackTrace` puede fijar uno.
    private StackTraceElement[] stackTrace;
    // Las excepciones **suprimidas** (§14.20.3.1, try-with-resources): crece de a una.
    private Throwable[] suppressed;

    public Throwable() {
        this.message = null;
        this.cause = this;
    }

    public Throwable(String message) {
        this.message = message;
        this.cause = this;
    }

    public Throwable(String message, Throwable cause) {
        this.message = message;
        this.cause = cause;
    }

    public Throwable(Throwable cause) {
        if (cause == null) {
            this.message = null;
        } else {
            this.message = cause.toString();
        }
        this.cause = cause;
    }

    public String getMessage() {
        return this.message;
    }

    public String getLocalizedMessage() {
        return getMessage();
    }

    public synchronized Throwable getCause() {
        if (this.cause == this) {
            return null;
        }
        return this.cause;
    }

    public synchronized Throwable initCause(Throwable cause) {
        this.cause = cause;
        return this;
    }

    public String toString() {
        String name = this.getClass().getName();
        if (this.message == null) {
            return name;
        }
        return name + ": " + this.message;
    }

    // ---- stack trace ----
    //
    // KajiJDK no captura la pila de forma nativa: `fillInStackTrace` no hace nada y el trace queda
    // vacío salvo que se fije con `setStackTrace`. La superficie es la del JDK.

    /**
     * Fill in the execution stack trace. In the JDK this is native and records the current stack;
     * KajiJDK has no such capture, so it just returns {@code this} with an empty trace.
     */
    public synchronized Throwable fillInStackTrace() {
        this.stackTrace = new StackTraceElement[0];
        return this;
    }

    /** The captured stack trace, or an empty array if none was captured or set. */
    public StackTraceElement[] getStackTrace() {
        if (this.stackTrace == null) {
            return new StackTraceElement[0];
        }
        return this.stackTrace.clone();
    }

    /** Replace the stack trace with a copy of {@code stackTrace}. */
    public void setStackTrace(StackTraceElement[] stackTrace) {
        StackTraceElement[] copy = stackTrace.clone();
        int i = 0;
        while (i < copy.length) {
            if (copy[i] == null) {
                throw new NullPointerException("stackTrace[" + i + "]");
            }
            i = i + 1;
        }
        this.stackTrace = copy;
    }

    // ---- suppressed exceptions (§14.20.3.1) ----

    /**
     * Record {@code exception} as suppressed by this one (a {@code try}-with-resources whose body
     * threw, and whose {@code close()} then threw too, adds the close exception here).
     *
     * @throws IllegalArgumentException if {@code exception} is this throwable
     * @throws NullPointerException if {@code exception} is null
     */
    public final synchronized void addSuppressed(Throwable exception) {
        if (exception == this) {
            throw new IllegalArgumentException("no se puede suprimir a sí misma", exception);
        }
        if (exception == null) {
            throw new NullPointerException("la excepción suprimida es null");
        }
        if (this.suppressed == null) {
            this.suppressed = new Throwable[] { exception };
            return;
        }
        Throwable[] bigger = new Throwable[this.suppressed.length + 1];
        System.arraycopy(this.suppressed, 0, bigger, 0, this.suppressed.length);
        bigger[this.suppressed.length] = exception;
        this.suppressed = bigger;
    }

    /** The exceptions suppressed by this one, most recent last; empty if none. */
    public final synchronized Throwable[] getSuppressed() {
        if (this.suppressed == null) {
            return new Throwable[0];
        }
        return this.suppressed.clone();
    }

    // ---- printing ----
    //
    // A KajiJDK va a `System.out`: no hay `System.err` en esta biblioteca todavía.

    /** Print this throwable and its backtrace to the standard output. */
    public void printStackTrace() {
        printStackTrace(System.out);
    }

    /** Print this throwable and its backtrace to {@code s}. */
    public void printStackTrace(PrintStream s) {
        s.println(this.toString());
        StackTraceElement[] trace = getStackTrace();
        int i = 0;
        while (i < trace.length) {
            s.println("\tat " + trace[i]);
            i = i + 1;
        }
        Throwable[] sup = getSuppressed();
        int k = 0;
        while (k < sup.length) {
            s.println("\tSuppressed: " + sup[k].toString());
            k = k + 1;
        }
        Throwable c = getCause();
        if (c != null) {
            s.println("Caused by: " + c.toString());
        }
    }

    /** Print this throwable and its backtrace to {@code s}. */
    public void printStackTrace(PrintWriter s) {
        s.println(this.toString());
        StackTraceElement[] trace = getStackTrace();
        int i = 0;
        while (i < trace.length) {
            s.println("\tat " + trace[i]);
            i = i + 1;
        }
        Throwable[] sup = getSuppressed();
        int k = 0;
        while (k < sup.length) {
            s.println("\tSuppressed: " + sup[k].toString());
            k = k + 1;
        }
        Throwable c = getCause();
        if (c != null) {
            s.println("Caused by: " + c.toString());
        }
    }
}
