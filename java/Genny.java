// Drives the generational GC: a long-lived object (`keep`) referenced across a loop
// that allocates short-lived garbage (`tmp`), overflowing Eden many times. Each minor
// must evacuate `keep` (then promote it) and reclaim the dead `tmp`s — and the
// `keep.next = tmp` store creates an Old→young pointer the minor has to honour.
// Correct only if survivors and their references survive evacuation intact.
public class Genny {
    int v;
    Genny next;

    Genny(int v) {
        this.v = v;
    }

    static int run() {
        Genny keep = new Genny(7);
        int sum = 0;
        for (int i = 0; i < 200; i++) {
            Genny tmp = new Genny(i);
            keep.next = tmp;   // Old(keep, once tenured) -> young(tmp)
            sum += tmp.v;
        }
        // sum = 0+...+199 = 19900 ; keep.v = 7 ; keep.next = last tmp (v=199)
        return sum + keep.v + keep.next.v; // 19900 + 7 + 199 = 20106
    }
}
