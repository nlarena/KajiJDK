package java.lang;

// Minimal IllegalMonitorStateException — the VM synthesizes and throws one when a thread
// touches a monitor it does not own: `monitorexit`, or `wait`/`notify`/`notifyAll` called
// without holding the object's intrinsic monitor (JVMS §6.5 monitorexit, JLS 17.2).
public class IllegalMonitorStateException extends RuntimeException {
    public IllegalMonitorStateException() {
    }
}
