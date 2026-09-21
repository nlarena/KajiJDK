package java.lang;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import jdk.internal.proc.Proc;

// The implementation of `Process` over a system process. Package-private: it is reached through
// `ProcessBuilder.start()`, which is the only way of making one -- just as in the JDK.
//
// All the state is the `handle`, an index into the VM's process table. The three streams are views
// over that handle and not buffers of their own: reading from `getInputStream()` reads the child's
// pipe at that moment, which is what a stream promises.
//
// Why the streams are nested classes and not one class parameterised by "which": because the input
// writes and the other two read, and a class doing both would have half its methods throwing. Two
// small classes say better what each one is.
final class ChildProcess extends Process {

    private final int handle;
    private final OutputStream input;
    private final InputStream output;
    private final InputStream error;

    ChildProcess(int handle) {
        this.handle = handle;
        this.input = new ChildInput(handle);
        this.output = new ChildOutput(handle, true);
        this.error = new ChildOutput(handle, false);
    }

    // Mind the names, which are the other way round from what one would expect and are like that in
    // the JDK: the process's "OutputStream" is ITS INPUT -- one writes there to send it data--, and
    // the "InputStream" is ITS OUTPUT. They are named from the caller's point of view.
    public OutputStream getOutputStream() {
        return this.input;
    }

    public InputStream getInputStream() {
        return this.output;
    }

    public InputStream getErrorStream() {
        return this.error;
    }

    public int waitFor() throws InterruptedException {
        return Proc.waitFor(this.handle);
    }

    /**
     * @throws IllegalThreadStateException if it has not finished yet
     */
    public int exitValue() {
        int c = Proc.exitValue(this.handle);
        if (c == Integer.MIN_VALUE) {
            throw new IllegalThreadStateException("the process has not finished yet");
        }
        return c;
    }

    public void destroy() {
        Proc.destroy(this.handle, false);
    }

    /**
     * It kills it with no chance to finish.
     *
     * <p>On Windows it is the same as {@link #destroy()}: the system has no "polite" signal a process
     * can attend to, so both kill alike. That is said here instead of faking two behaviours, and it
     * is why {@link #supportsNormalTermination()} returns `false` there.
     */
    public Process destroyForcibly() {
        Proc.destroy(this.handle, true);
        return this;
    }

    public boolean isAlive() {
        return Proc.isAlive(this.handle);
    }

    public long pid() {
        long p = Proc.pid(this.handle);
        if (p < 0L) {
            throw new UnsupportedOperationException("could not obtain the pid");
        }
        return p;
    }

    /**
     * Whether {@link #destroy()} asks for termination instead of forcing it.
     *
     * <p>`false` on Windows and `true` elsewhere, which is exactly what the JDK answers. It is not a
     * limitation of this library: it is a difference between operating systems.
     */
    public boolean supportsNormalTermination() {
        return !System.getProperty("os.name", "").startsWith("Windows");
    }

    // The child's input. Every write goes straight to the native and is flushed: nothing accumulates
    // on this side, because a buffer of our own would mean the child did not see what had already been
    // written to it until somebody called `flush()`, and the writer has no business knowing that.
    private static final class ChildInput extends OutputStream {

        private final int handle;
        private boolean closed;

        ChildInput(int handle) {
            this.handle = handle;
        }

        public void write(int b) throws IOException {
            byte[] one = new byte[] { (byte) b };
            this.write(one, 0, 1);
        }

        public void write(byte[] b, int off, int len) throws IOException {
            if (b == null) {
                throw new NullPointerException();
            }
            if (off < 0 || len < 0 || off + len > b.length) {
                throw new IndexOutOfBoundsException();
            }
            if (this.closed) {
                throw new IOException("the process's input is closed");
            }
            if (len == 0) {
                return;
            }
            if (!Proc.writeIn(this.handle, b, off, len)) {
                throw new IOException("could not write to the process");
            }
        }

        public void close() throws IOException {
            if (!this.closed) {
                this.closed = true;
                Proc.closeIn(this.handle);
            }
        }
    }

    // The child's output or its error, according to `isStdout`. It reads straight from the pipe.
    private static final class ChildOutput extends InputStream {

        private final int handle;
        private final boolean isStdout;

        ChildOutput(int handle, boolean isStdout) {
            this.handle = handle;
            this.isStdout = isStdout;
        }

        public int read() throws IOException {
            byte[] one = new byte[1];
            int n = this.read(one, 0, 1);
            if (n <= 0) {
                return -1;
            }
            // To 0..255, because `read()` returns an unsigned byte and -1 only means end.
            return one[0] & 0xFF;
        }

        public int read(byte[] b, int off, int len) throws IOException {
            if (b == null) {
                throw new NullPointerException();
            }
            if (off < 0 || len < 0 || off + len > b.length) {
                throw new IndexOutOfBoundsException();
            }
            if (len == 0) {
                return 0;
            }
            // The native fills from the start of the array it is handed, so when the caller asks for
            // a stretch an array of our own is used and copied out. It is one copy too many and it
            // saves the native having to know about offsets.
            byte[] buf = off == 0 && len == b.length ? b : new byte[len];
            int n = this.isStdout ? Proc.readOut(this.handle, buf) : Proc.readErr(this.handle, buf);
            if (n > 0 && buf != b) {
                System.arraycopy(buf, 0, b, off, n);
            }
            return n;
        }
    }
}
