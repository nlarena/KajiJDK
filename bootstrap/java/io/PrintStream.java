package java.io;

// Minimal java.io.PrintStream. `println(int)` is native — the bridge to real I/O
// (the point where the JVM finally leaves bytecode and talks to the OS). The full
// JDK routes through String/char[]/FileOutputStream; we go straight to native.
public class PrintStream {
    public PrintStream() {
    }

    public native void println(int x);

    public native void println(String x);
}
