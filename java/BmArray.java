// Baseline workload — dimension: **arrays**.
//
// One array, allocated once outside the loop (so the heap stays flat and the GC never
// fires), then written and read to exhaustion. The opcode mix is `iaload`/`iastore` plus
// the index arithmetic around them — the bounds check and the heap access, isolated from
// allocation, fields and calls.
public class BmArray {
    static int run() {
        int[] a = new int[1024];
        int acc = 0;
        for (int i = 0; i < 1000; i++) {
            for (int j = 0; j < 1024; j++) {
                a[j] = (a[j] + i + j) & 0xFFFF;
            }
            acc = (acc + a[i & 1023]) & 0xFFFFF;
        }
        return acc;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
