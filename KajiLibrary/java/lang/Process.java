package java.lang;

import java.io.OutputStream;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;
import java.time.Duration;
import java.util.stream.Stream;

/**
 * KajiLibrary's java.lang.Process -- the handle a running native process is seen through.
 *
 * <p>The <strong>surface</strong> of the JDK's abstract class, faithfully: the six primitives are
 * abstract, and the rest are built on them exactly as the JDK builds them. But KajiJDK has no
 * OS-process subsystem, so nothing subclasses this and nothing produces an instance; the class is
 * here for the type and its API. Where the JDK's own base class delegates to a process handle
 * ({@link #toHandle()} and the methods that go through it) it throws {@link
 * UnsupportedOperationException}, and so does this one.
 */
public abstract class Process {

    /** Constructor for subclasses (there are none in KajiJDK). */
    public Process() {
    }

    /** The pipe written to the process's standard input. */
    public abstract OutputStream getOutputStream();

    /** The pipe read from the process's standard output. */
    public abstract InputStream getInputStream();

    /** The pipe read from the process's standard error. */
    public abstract InputStream getErrorStream();

    // Los lectores/escritores envuelven los streams en un `InputStreamReader`/`OutputStreamWriter`,
    // que KajiLibrary todavía no tiene, así que no se pueden armar. Se declaran igual (superficie).
    public final BufferedReader inputReader() {
        throw new UnsupportedOperationException("inputReader necesita java.io.InputStreamReader");
    }

    public final BufferedReader inputReader(Charset charset) {
        throw new UnsupportedOperationException("inputReader necesita java.io.InputStreamReader");
    }

    public final BufferedReader errorReader() {
        throw new UnsupportedOperationException("errorReader necesita java.io.InputStreamReader");
    }

    public final BufferedReader errorReader(Charset charset) {
        throw new UnsupportedOperationException("errorReader necesita java.io.InputStreamReader");
    }

    public final BufferedWriter outputWriter() {
        throw new UnsupportedOperationException("outputWriter necesita java.io.OutputStreamWriter");
    }

    public final BufferedWriter outputWriter(Charset charset) {
        throw new UnsupportedOperationException("outputWriter necesita java.io.OutputStreamWriter");
    }

    /** Wait for the process to exit and return its exit value. */
    public abstract int waitFor() throws InterruptedException;

    /**
     * Wait up to {@code timeout} units for the process to exit; return whether it did.
     *
     * <p>Built on {@link #isAlive()} and {@link #exitValue()} like the JDK's base: poll with a short
     * sleep until the deadline.
     */
    public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
        if (!isAlive()) {
            return true;
        }
        long remaining = unit.toMillis(timeout);
        long deadline = System.currentTimeMillis() + remaining;
        while (remaining > 0) {
            Thread.sleep(Math.min(remaining, 100));
            if (!isAlive()) {
                return true;
            }
            remaining = deadline - System.currentTimeMillis();
        }
        return !isAlive();
    }

    /** Wait up to {@code timeout} for the process to exit; return whether it did. */
    public boolean waitFor(Duration timeout) throws InterruptedException {
        return waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    /** The exit value, or {@link IllegalThreadStateException} if the process is still running. */
    public abstract int exitValue();

    /** Request that the process be killed. */
    public abstract void destroy();

    /** Request forcible termination and return this process. */
    public Process destroyForcibly() {
        destroy();
        return this;
    }

    /** Whether {@link #destroy()} terminates the process normally rather than forcibly. */
    public boolean supportsNormalTermination() {
        throw new UnsupportedOperationException(this.getClass() + ".supportsNormalTermination() not supported");
    }

    /** Whether the process is still running. */
    public boolean isAlive() {
        try {
            exitValue();
            return false;
        } catch (IllegalThreadStateException e) {
            return true;
        }
    }

    /** The native process id. */
    public long pid() {
        return toHandle().pid();
    }

    /**
     * A future that completes with this process when it exits.
     *
     * <p>Como el base del JDK: un hilo del pool espera al proceso y completa el futuro. La espera no
     * propaga la interrupción del hilo del pool (el futuro sólo debe completar cuando el proceso
     * salió de verdad); el estado de interrupción se reafirma al final.
     */
    public CompletableFuture<Process> onExit() {
        return CompletableFuture.supplyAsync(() -> this.waitUninterruptibly());
    }

    private Process waitUninterruptibly() {
        boolean interrupted = false;
        try {
            while (true) {
                try {
                    waitFor();
                    return this;
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /** The handle for this process. The generic base has none, so this throws. */
    public ProcessHandle toHandle() {
        throw new UnsupportedOperationException("Process.toHandle() not supported by " + this.getClass());
    }

    /** A snapshot of information about the process (via its handle). */
    public ProcessHandle.Info info() {
        return toHandle().info();
    }

    /** The direct children of this process (via its handle). */
    public Stream<ProcessHandle> children() {
        return toHandle().children();
    }

    /** Every descendant of this process (via its handle). */
    public Stream<ProcessHandle> descendants() {
        return toHandle().descendants();
    }
}
