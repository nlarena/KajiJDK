// Pinpoints the write barrier / remembered set. `keep` survives enough minors to be
// tenured to Old; then it's pointed at a *fresh* young object held ONLY through
// `keep.next` (no local, no stack root). More allocation forces a minor — the young
// target is reachable solely via the Old→young pointer, so it survives only if the
// write barrier recorded `keep` and the minor scanned the remembered set.
public class Barrier {
    int v;
    Barrier next;

    Barrier(int v) {
        this.v = v;
    }

    static int run() {
        Barrier keep = new Barrier(1);
        for (int i = 0; i < 100; i++) {
            new Barrier(i); // garbage to fill Eden → minors run → keep is tenured
        }
        keep.next = new Barrier(99); // Old(keep) -> young, reachable only via keep.next
        for (int i = 0; i < 100; i++) {
            new Barrier(i); // force more minors; keep.next's target must survive
        }
        return keep.next.v; // 99 only if the remembered set kept it alive
    }
}
