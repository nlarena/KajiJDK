package jdk.internal.proc;

/**
 * KajiLibrary's jdk.internal.proc.Proc -- the seam with the processes of the system.
 *
 * <p>It is what was missing for {@link java.lang.ProcessBuilder#start()} to be able to exist. Until
 * these natives were there, `start()` was **not declared**: a `Process` that represents no process
 * is not a member that can be written honestly.
 *
 * <p>It follows the same criterion as {@link jdk.internal.io.Fs}: the native does the minimum and
 * **knows nothing about the classes of Java**. It takes and returns strings, arrays and integers;
 * who `Process` or `ProcessBuilder` may be is a problem of the Java side, which can change without
 * touching Rust.
 *
 * <p>The difference with `Fs` is that here there **is** a handle. A file is read whole in one go,
 * but a process is state that lives between calls --its pipes, its exit code-- and there is no way
 * of representing it with one-shot operations. The handle is an index into a table of the VM, and
 * its entries are **not recycled**: an old handle never points at a new process.
 *
 * <p>The modes of redirection are the five `ProcessBuilder.Redirect` really tells apart:
 * <b>0</b> pipe, <b>1</b> inherit, <b>2</b> discard, <b>3</b> file overwriting, <b>4</b> file
 * appending.
 */
public final class Proc {

    private Proc() {
    }

    /**
     * It launches the process.
     *
     * @param cmd the command and its arguments; the first one is the executable
     * @param dir the working directory, or `null` to inherit ours
     * @param envKV the environment as flattened pairs (key, value, key, value...), or empty to
     *     inherit ours. If it is not empty it **replaces** the whole environment, as
     *     `ProcessBuilder.environment()`
     * @param paths the three paths of redirection (input, output, error), with `null` where it does
     *     not apply
     * @param modes the three modes
     * @param joinError whether the error goes to the same pipe as the output
     * @return the handle, or -1 if it could not be launched
     */
    public static native int spawn(String[] cmd, String dir, String[] envKV, String[] paths,
            int[] modes, boolean joinError);

    /** It waits for it to finish and returns its exit code. */
    public static native int waitFor(int handle);

    /**
     * The exit code if it has already finished, or {@link Integer#MIN_VALUE} if it is still
     * running.
     *
     * <p>The sentinel is what allows `Process.exitValue()` to throw `IllegalThreadStateException`
     * --which is what the contract asks for-- instead of blocking.
     */
    public static native int exitValue(int handle);

    /** Whether it is still running. */
    public static native boolean isAlive(int handle);

    /**
     * It kills it.
     *
     * @param force it is accepted and **changes nothing on Windows**, where there is no "kind"
     *     signal
     */
    public static native void destroy(int handle, boolean force);

    /** Its process identifier, or -1. */
    public static native long pid(int handle);

    /** It writes into its standard input. `true` if it could. */
    public static native boolean writeIn(int handle, byte[] b, int off, int len);

    /** It closes its input, which is how it is told "no more is coming". */
    public static native void closeIn(int handle);

    /** It reads from its output. It returns how many bytes it put, or -1 at end of stream. */
    public static native int readOut(int handle, byte[] b);

    /** It reads from its error output. */
    public static native int readErr(int handle, byte[] b);
}
