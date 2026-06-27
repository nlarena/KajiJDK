// Same as Sync but WITHOUT `synchronized` — the read-modify-write races, so updates are
// lost and the total comes out < 200. This is the control that proves the monitor matters.
class RCounter { int value; }
class RAdder extends Thread {
    int id;
    RAdder(int id) { this.id = id; }
    public void run() {
        for (int i = 0; i < 100; i++) {
            Racy.c.value = Racy.c.value + 1; // no lock → lost updates
        }
        if (id == 1) { Racy.aDone = 1; } else { Racy.bDone = 1; }
    }
}
public class Racy {
    static RCounter c = new RCounter();
    static int aDone = 0;
    static int bDone = 0;
    static int run() {
        RAdder a = new RAdder(1);
        RAdder b = new RAdder(2);
        a.start();
        b.start();
        while (aDone == 0 || bDone == 0) { }
        return Racy.c.value;
    }
}
