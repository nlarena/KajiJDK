// Baseline workload — dimension: **objects and instance fields**.
//
// An outer loop allocates one small object per iteration (`new` + `<init>`, i.e. real
// heap pressure and therefore real GC work), and an inner loop hammers `getfield` /
// `putfield` on it so the field opcodes — not the allocation — dominate the opcode mix.
// The allocation rate is deliberately non-trivial: this is the only workload of the set
// whose time includes collection, which is exactly what makes it diagnostic.
class BmCell {
    int a;
    int b;

    BmCell(int a, int b) {
        this.a = a;
        this.b = b;
    }
}

public class BmField {
    static int run() {
        int acc = 0;
        for (int i = 0; i < 800; i++) {
            BmCell c = new BmCell(i, i + 1);
            for (int j = 0; j < 120; j++) {
                c.a = c.a + j;
                c.b = c.b ^ c.a;
                c.a = c.a & 0xFFFF;
                c.b = c.b & 0xFFFF;
            }
            acc = (acc + c.a + c.b) & 0xFFFFF;
        }
        return acc;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
