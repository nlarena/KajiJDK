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
    // Los dos interruptores del constructor de cuatro argumentos. Arrancan en `true` porque es lo
    // que hacen los otros cuatro constructores.
    private boolean suppressionEnabled = true;
    private boolean stackTraceWritable = true;

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

    /**
     * El constructor con los dos interruptores, para una subclase que quiera apagarlos.
     *
     * <p>Los dos existen por la misma razon: hay excepciones que se lanzan **muchisimas veces** como
     * senal de control --el fin de un iterador, un salto de flujo-- y para esas, guardar la pila y la
     * lista de suprimidas es trabajo puro que nadie va a mirar. Apagarlos es lo que permite que una
     * excepcion singleton sea barata.
     *
     * <p>Con `enableSuppression` en `false`, `addSuppressed` **no hace nada** (no tira) y
     * `getSuppressed` devuelve siempre vacio. Con `writableStackTrace` en `false`, ni
     * `fillInStackTrace` ni `setStackTrace` cambian nada y el trace queda vacio para siempre.
     *
     * <p>Nota propia de esta biblioteca: KajiJDK **no captura la pila de forma nativa**, asi que el
     * trace ya venia vacio con el interruptor en `true`. Lo que el `false` agrega de verdad aca es
     * que `setStackTrace` deje de tener efecto, que es la mitad observable del contrato.
     */
    protected Throwable(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        this.message = message;
        this.cause = cause;
        this.suppressionEnabled = enableSuppression;
        this.stackTraceWritable = writableStackTrace;
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
        if (!this.stackTraceWritable) {
            return this;
        }
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

    /**
     * Replace the stack trace with a copy of {@code stackTrace}.
     *
     * <p>No hace nada si el objeto se construyo con `writableStackTrace` en `false`. Los chequeos
     * del argumento corren igual --el `null` se rechaza siempre-- porque son del contrato del
     * argumento y no del interruptor.
     */
    public void setStackTrace(StackTraceElement[] stackTrace) {
        StackTraceElement[] copy = stackTrace.clone();
        int i = 0;
        while (i < copy.length) {
            if (copy[i] == null) {
                throw new NullPointerException("stackTrace[" + i + "]");
            }
            i = i + 1;
        }
        if (!this.stackTraceWritable) {
            return;
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
        // Los dos chequeos de arriba valen igual: son del contrato del argumento, no del
        // interruptor. Recien aca la supresion apagada se vuelve un no-op, como en el JDK.
        if (!this.suppressionEnabled) {
            return;
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

    /**
     * Print this throwable and its backtrace to the standard **error** stream.
     *
     * <p>A `System.err`, como el JDK. El comentario que estaba acá decia que iba a `System.out`
     * porque "no hay `System.err` en esta biblioteca todavia" -- y `System.err` existe desde hace
     * rato, asi que la nota quedo vieja y el destino equivocado.
     *
     * <p>No es un detalle cosmetico: una traza en la salida estandar se mezcla con lo que el programa
     * imprime, y un `programa > archivo` se lleva el error adentro del resultado en vez de dejarlo en
     * la consola. Que los dos flujos esten separados es justamente para que eso no pase.
     */
    public void printStackTrace() {
        printStackTrace(System.err);
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
