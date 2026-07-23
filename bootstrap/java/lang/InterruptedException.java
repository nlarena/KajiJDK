package java.lang;

// Thrown when a thread is interrupted while it is blocked in an interruptible wait —
// Object.wait(), Thread.sleep() or Thread.join(). The VM rewinds to the blocking call and
// throws this so a `try { sleep(); } catch (InterruptedException e)` catches it; throwing
// it also clears the thread's interrupt status. (In the real JDK it extends Exception;
// same here.)
public class InterruptedException extends Exception {
    public InterruptedException() {
    }
}
