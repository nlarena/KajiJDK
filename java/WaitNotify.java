// wait/notify handshake: a worker waits inside synchronized until the producer sets a
// value and notifies. The `while (ready == 0) wait()` guard makes it correct whatever
// the interleaving (missed notify or spurious wakeup → just re-checks). Returns 42 iff
// the worker received the produced value. (Classes prefixed Wn to avoid .class clashes.)
class WnBox {
    int value;
    int ready;
}

class WnWaiter extends Thread {
    public void run() {
        synchronized (WaitNotify.box) {
            while (WaitNotify.box.ready == 0) {
                WaitNotify.box.wait();
            }
            WaitNotify.result = WaitNotify.box.value;
        }
        WaitNotify.done = 1;
    }
}

public class WaitNotify {
    static WnBox box = new WnBox();
    static int result = 0;
    static int done = 0;

    static int run() {
        WnWaiter w = new WnWaiter();
        w.start();
        synchronized (box) {
            box.value = 42;
            box.ready = 1;
            box.notify();
        }
        while (done == 0) {
        }
        return result; // 42 iff the waiter woke and read the produced value
    }
}
