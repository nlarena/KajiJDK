// F3 step 8 — **the order rule, across an inlined call**.
//
// Step 6's rule is that every guard is emitted before its instruction's first observable effect, and
// that nothing after that effect can deopt. Inlining is the first thing that could break it
// silently: an effect inside a callee must not end up before a guard that would re-execute it.
//
// `poke` is the smallest shape that asks the question. It **writes a field** and *then* meets a
// guard it may fail (an index outside the array), so a deopt has to report the `iaload` — past the
// write — and not the `putfield`. Every fourth call fails, and the field it wrote counts every
// call, failures included.
//
// That counter is the detector. `box.v` is folded into the result, so re-running the write would
// make it 500 instead of 400 and the printed number would move; the interpreted arm, which never
// deopts at all, is what pins which number is right.
class JiBox {
    int v;
}

public class JiOrder {
    static int poke(JiBox b, int[] a, int i) {
        b.v = b.v + 1;
        return a[i];
    }

    // `step` is what gets compiled, so the write and the guard that follows it are **inside an
    // inlined body** rather than inside the compiled method itself — which is the whole point:
    // asking the question of `poke` directly would test step 6 again, not this step.
    static int step(JiBox b, int[] a, int i) {
        return poke(b, a, i) + 1;
    }

    static int run() {
        JiBox box = new JiBox();
        int[] arr = { 3, 5, 7 };
        int acc = 0;
        for (int i = 0; i < 400; i++) {
            try {
                acc = (acc + step(box, arr, i % 4)) & 0xFFFFF;
            } catch (ArrayIndexOutOfBoundsException e) {
                acc = (acc + 1) & 0xFFFFF;
            }
        }
        return (acc * 31 + box.v) & 0xFFFFF;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
