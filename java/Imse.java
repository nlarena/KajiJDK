// IllegalMonitorStateException demo: calling notify() WITHOUT holding the object's
// monitor must throw IllegalMonitorStateException (JLS 17.2). The catch proves the VM
// both raises the exception and routes it through the normal exception machinery
// (athrow + handler search). Returns 99 iff the exception was thrown and caught.
public class Imse {
    static int run() {
        Object lock = new Object();
        try {
            lock.notify(); // not inside synchronized(lock) → IllegalMonitorStateException
            return 0; // unreachable if the exception fires
        } catch (IllegalMonitorStateException e) {
            return 99;
        }
    }
}
