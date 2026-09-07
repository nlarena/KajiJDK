package javax.annotation.processing;
// Minimal output bridge for APT's round loop (phase 2): a processor prints by calling this static
// method, which the VM dispatches to a native (it writes to the interpreter's console). It exists
// because `System.out.println` does not compile yet in the project's javac (resolving `System.out`
// as a static field fails); it is the minimal version of a `Messager` delegating to Rust.
public class AptTrace {
    public static native void trace(String msg);
}
