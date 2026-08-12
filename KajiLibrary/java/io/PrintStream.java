package java.io;

// KajiLibrary's java.io.PrintStream — minimal: the two `println` overloads the VM bridges
// straight to real OS output (the point where the JVM finally leaves bytecode and talks
// to the OS). The full JDK routes through String/char[]/FileOutputStream; KajiJDK goes
// straight to native.
public class PrintStream {

    public PrintStream() {
    }

    public native void println(int x);

    public native void println(String x);
}
