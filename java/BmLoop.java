// Baseline workload — dimension: **frame-local arithmetic and branches**.
//
// One long loop over int locals: no calls, no objects, no arrays, no fields. Every
// opcode it executes is a load/store of a local, an arithmetic op or a branch — the
// cheapest thing the interpreter can do — so its ns/opcode is the *floor* of the
// current engine, the number every other workload is read against. Nothing is
// allocated, so the GC never fires here.
public class BmLoop {
    static int run() {
        int acc = 1;
        for (int i = 0; i < 900000; i++) {
            acc = acc + i;
            acc = acc ^ (acc >> 7);
            if ((i & 15) == 0) {
                acc = acc - 3;
            }
        }
        return acc & 0xFFFFF;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
