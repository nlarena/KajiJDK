// Monitor demo via a `synchronized` METHOD (not a block). `SmCounter.bump()` carries
// ACC_SYNCHRONIZED — there are NO monitorenter/monitorexit opcodes in its body; the VM
// takes the receiver's (`this`) monitor when it pushes the frame and releases it when the
// frame is popped (normal return or unwind). Two threads each call bump() 100×; the
// synchronized method serializes the read-modify-write of `value`, so no updates are
// lost → exactly 200. (The block-based twin is Sync.java; the unsynchronized counter-
// example is Racy.java. Classes prefixed `Sm` to avoid clobbering other demos' .class.)
class SmCounter {
    int value;

    // Synchronized INSTANCE method: equivalent to `synchronized (this) { ... }`, but
    // signalled by the access flag alone — the whole body is the critical section.
    synchronized void bump() {
        value = value + 1;
    }
}

class SmAdder extends Thread {
    int id;

    SmAdder(int id) {
        this.id = id;
    }

    public void run() {
        for (int i = 0; i < 100; i++) {
            SyncMethod.c.bump();
        }
        if (id == 1) {
            SyncMethod.aDone = 1;
        } else {
            SyncMethod.bDone = 1;
        }
    }
}

public class SyncMethod {
    static SmCounter c = new SmCounter();
    static int aDone = 0;
    static int bDone = 0;

    static int run() {
        SmAdder a = new SmAdder(1);
        SmAdder b = new SmAdder(2);
        a.start();
        b.start();
        while (aDone == 0 || bDone == 0) {
        }
        return c.value; // 200 iff the synchronized method prevented lost updates
    }
}
