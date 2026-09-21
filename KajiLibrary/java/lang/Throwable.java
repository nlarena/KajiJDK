package java.lang;

import java.io.Serializable;
import java.io.PrintStream;
import java.io.PrintWriter;

// KajiLibrary's java.lang.Throwable — the superclass of all errors and exceptions. Carries a detail
// message and an optional cause (another Throwable), with the JDK's `cause == this` sentinel
// meaning "not yet initialised". The message/cause plumbing is pure Java. Stack-trace **capture**
// needs the VM (there is no native `fillInStackTrace`), so the trace is empty unless one is set
// with `setStackTrace`; the suppressed-exception list (try-with-resources) is modelled in full.
public class Throwable implements Serializable {

    private String message;
    private Throwable cause;
    // The stack trace: `null` means "not captured" (KajiJDK does not capture natively), and it is
    // reported as an empty array. `setStackTrace` can set one.
    private StackTraceElement[] stackTrace;
    // The **suppressed** exceptions (§14.20.3.1, try-with-resources): it grows one at a time.
    private Throwable[] suppressed;
    // The four-argument constructor's two switches. They start `true` because that is what the
    // other four constructors do.
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
     * The constructor with the two switches, for a subclass that wants to turn them off.
     *
     * <p>Both exist for the same reason: there are exceptions thrown **a great many times** as a
     * control signal --an iterator's end, a jump in the flow-- and for those, keeping the stack and
     * the suppressed list is pure work nobody is going to look at. Turning them off is what makes
     * a singleton exception cheap.
     *
     * <p>With `enableSuppression` at `false`, `addSuppressed` **does nothing** (it does not throw)
     * and `getSuppressed` always returns empty. With `writableStackTrace` at `false`, neither
     * `fillInStackTrace` nor `setStackTrace` changes anything and the trace stays empty for
     * good.
     *
     * <p>A note of this library's own: KajiJDK **does not capture the stack natively**, so the
     * trace came out empty with the switch at `true` already. What the `false` genuinely adds here
     * is that `setStackTrace` stops having an effect, which is the observable half of the contract.
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

    /**
     * It sets the cause, and **only once**.
     *
     * <p>Both guards are part of the contract and not decoration. The first --an already set cause
     * cannot be overwritten-- exists because the chain of causes is what explains an error, and
     * letting it be rewritten would allow erasing the original reason from any `catch` along the
     * way. That is also why it cannot be used on an exception constructed **with** a cause: there
     * it is set already.
     *
     * <p>The second stops an exception being its own cause, which would leave `printStackTrace` in
     * an infinite loop.
     *
     * <p>The "not set" sentinel is `cause == this`, just as in the JDK. It is what allows telling
     * "not set yet" from "set to null", which are two different states: the second is closed to
     * future calls as well.
     *
     * @throws IllegalStateException if the cause had already been set
     * @throws IllegalArgumentException if it is passed itself
     */
    public synchronized Throwable initCause(Throwable cause) {
        if (this.cause != this) {
            throw new IllegalStateException(
                "Can't overwrite cause with " + (cause == null ? "a null" : cause.toString()));
        }
        if (cause == this) {
            throw new IllegalArgumentException("Self-causation not permitted");
        }
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
    // KajiJDK does not capture the stack natively: `fillInStackTrace` does nothing and the trace
    // stays empty unless it is set with `setStackTrace`. The surface is the JDK's.

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
     * <p>It does nothing if the object was constructed with `writableStackTrace` at `false`. The
     * argument's checks run all the same --a `null` is always rejected-- because they belong to the
     * argument's contract and not to the switch.
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
            throw new IllegalArgumentException("it cannot suppress itself", exception);
        }
        if (exception == null) {
            throw new NullPointerException("the suppressed exception is null");
        }
        // Both checks above hold all the same: they belong to the argument's contract, not to the
        // switch. Only here does suppression being off become a no-op, as in the JDK.
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
     * <p>To `System.err`, like the JDK. The comment that used to be here said it went to
     * `System.out` because "there is no `System.err` in this library yet" -- and `System.err` has
     * existed for a good while, so the note had gone stale and the destination was wrong.
     *
     * <p>It is no cosmetic detail: a trace on standard output mixes with whatever the program
     * prints, and a `program > file` carries the error inside the result instead of leaving it on
     * the console. That the two streams are separate is precisely so that does not happen.
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
