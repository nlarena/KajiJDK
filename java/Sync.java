// Monitor demo: two threads each add 100 to a shared counter inside `synchronized`.
// The critical section (read-modify-write of c.value) is mutually exclusive, so there
// are NO lost updates → exactly 200. Without the lock, the per-opcode interleaving of
// `getfield value; iadd; putfield value` between the two threads would lose updates.
// (Classes prefixed `Sync` to avoid clobbering other demos' .class files in java/.)
class SyncCounter {
    int value;
}

class SyncAdder extends Thread {
    int id;

    SyncAdder(int id) {
        this.id = id;
    }

    public void run() {
        for (int i = 0; i < 100; i++) {
            synchronized (Sync.c) {
                Sync.c.value = Sync.c.value + 1;
            }
        }
        if (id == 1) {
            Sync.aDone = 1;
        } else {
            Sync.bDone = 1;
        }
    }
}

public class Sync {
    static SyncCounter c = new SyncCounter();
    static int aDone = 0;
    static int bDone = 0;

    static int run() {
        SyncAdder a = new SyncAdder(1);
        SyncAdder b = new SyncAdder(2);
        a.start();
        b.start();
        while (aDone == 0 || bDone == 0) {
        }
        return Sync.c.value; // 200 iff synchronized prevented lost updates
    }
}
